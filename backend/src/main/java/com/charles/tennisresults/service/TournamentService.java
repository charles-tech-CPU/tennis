package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.QualifyingCreateDto;
import com.charles.tennisresults.dto.RoundPointsDto;
import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentDto;
import com.charles.tennisresults.dto.TournamentStatus;
import com.charles.tennisresults.dto.TournamentUpdateDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRoundRepository tournamentRoundRepository;
    private final MatchRepository matchRepository;
    private final EntryRepository entryRepository;
    private final BracketService bracketService;

    public TournamentService(TournamentRepository tournamentRepository,
                              TournamentRoundRepository tournamentRoundRepository,
                              MatchRepository matchRepository,
                              EntryRepository entryRepository,
                              BracketService bracketService) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.matchRepository = matchRepository;
        this.entryRepository = entryRepository;
        this.bracketService = bracketService;
    }

    public List<TournamentDto> findAll() {
        List<Tournament> tournaments = tournamentRepository.findAll().stream()
                .filter(t -> !t.isQualifying())
                .toList();

        // Un seul aller-retour pour tous les matchs decides de tous les tournois
        // (+ leurs qualifs), plutot qu'une requete par tournoi (N+1) juste pour
        // colorer la liste.
        TournamentProgress progress = TournamentProgress.compute(tournamentRepository, matchRepository, tournaments);
        Map<Long, Integer> hues = progress.hueByTournamentId(tournaments);

        return tournaments.stream()
                .map(t -> toDto(t, progress.statusOf(t), hues.get(t.getId())))
                .sorted(Comparator.comparing(TournamentDto::season).reversed()
                        .thenComparing(t -> t.weekNumber() == null ? 0 : t.weekNumber())
                        .thenComparing(TournamentDto::name))
                .toList();
    }

    public TournamentDto findOne(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public TournamentDto create(TournamentCreateDto dto) {
        Tournament t = new Tournament();
        t.setName(dto.name());
        t.setCategory(dto.category());
        t.setSeason(dto.season());
        t.setWeekNumber(dto.weekNumber());
        t.setCountry(dto.country());
        t.setMandatorySlot(dto.mandatorySlot());
        t.setDrawSize(dto.drawSize());
        t.setQualifyingRound1Points(dto.qualifyingRound1Points());
        t.setQualifyingRound2Points(dto.qualifyingRound2Points());
        t.setRunnerUpPoints(dto.runnerUpPoints());
        t = tournamentRepository.save(t);

        int drawSlots = RoundLabels.nextPowerOfTwo(dto.drawSize());
        List<Integer> points = (dto.rounds() != null && !dto.rounds().isEmpty())
                ? dto.rounds().stream().map(RoundPointsDto::points).toList()
                : CategoryDefaults.pointsFor(dto.category(), drawSlots);

        int totalRounds = RoundLabels.roundCount(drawSlots);
        for (int r = 1; r <= totalRounds; r++) {
            int pts = (r - 1) < points.size() ? points.get(r - 1) : points.get(points.size() - 1);
            TournamentRound round = new TournamentRound(t, r, RoundLabels.labelFor(r, totalRounds), pts);
            tournamentRoundRepository.save(round);
        }

        bracketService.initializeSkeleton(t, drawSlots);

        return toDto(t);
    }

    @Transactional
    public TournamentDto update(Long id, TournamentUpdateDto dto) {
        Tournament t = getOrThrow(id);
        if (dto.category() != null) {
            t.setCategory(dto.category());
        }
        t.setWeekNumber(dto.weekNumber());
        t.setCountry(dto.country());
        t.setMandatorySlot(dto.mandatorySlot());
        t.setQualifyingRound1Points(dto.qualifyingRound1Points());
        t.setQualifyingRound2Points(dto.qualifyingRound2Points());
        t.setRunnerUpPoints(dto.runnerUpPoints());

        if (dto.rounds() != null) {
            for (RoundPointsDto roundDto : dto.rounds()) {
                t.getRounds().stream()
                        .filter(r -> r.getRoundOrder().equals(roundDto.roundOrder()))
                        .findFirst()
                        .ifPresent(r -> r.setPoints(roundDto.points()));
            }
        }

        return toDto(t);
    }

    @Transactional
    public TournamentDto createQualifying(Long mainTournamentId, QualifyingCreateDto dto) {
        Tournament main = getOrThrow(mainTournamentId);
        if (main.isQualifying()) {
            throw new IllegalArgumentException("Un tableau de qualifications ne peut pas avoir ses propres qualifications.");
        }
        if (tournamentRepository.findByMainTournamentId(mainTournamentId).isPresent()) {
            throw new IllegalArgumentException("Ce tournoi a deja un tableau de qualifications.");
        }

        Tournament q = new Tournament();
        q.setName(main.getName() + " - Qualifs");
        q.setCategory(main.getCategory());
        q.setSeason(main.getSeason());
        // Seuls les Grand Chelem jouent leurs qualifs la semaine PRECEDENT le
        // tableau principal (evenement a part sur le calendrier) ; pour toutes
        // les autres categories, qualifs et tableau principal sont la meme
        // semaine (Charles, 2026-09-15 - ex: Watanuki inscrit a Phan Thiet la
        // meme semaine que les qualifs de l'Australian Open).
        q.setWeekNumber(main.getCategory() == TournamentCategory.GRAND_SLAM && main.getWeekNumber() != null
                ? main.getWeekNumber() - 1
                : main.getWeekNumber());
        q.setCountry(main.getCountry());
        q.setDrawSize(dto.drawSize());
        q.setQualifying(true);
        q.setMainTournamentId(mainTournamentId);
        q = tournamentRepository.save(q);

        int totalRounds = dto.rounds().size();
        for (int r = 1; r <= totalRounds; r++) {
            int pts = dto.rounds().get(r - 1).points();
            TournamentRound round = new TournamentRound(q, r, RoundLabels.qualifyingLabelFor(r), pts);
            tournamentRoundRepository.save(round);
        }

        bracketService.initializeSkeleton(q, dto.drawSize(), totalRounds);

        return toDto(q);
    }

    @Transactional
    public void delete(Long id) {
        Tournament t = getOrThrow(id);
        if (entryRepository.countByTournamentId(id) > 0) {
            throw new IllegalArgumentException(
                    "Ce tournoi a deja des joueurs places dans son tableau, il ne peut pas etre supprime.");
        }
        tournamentRepository.findByMainTournamentId(id).ifPresent(q -> {
            if (entryRepository.countByTournamentId(q.getId()) > 0) {
                throw new IllegalArgumentException(
                        "Le tableau de qualifications de ce tournoi a deja des joueurs, il ne peut pas etre supprime.");
            }
            tournamentRepository.delete(q);
        });
        tournamentRepository.delete(t);
    }

    private Tournament getOrThrow(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournoi introuvable: " + id));
    }

    private TournamentDto toDto(Tournament t) {
        Tournament main = t.isQualifying() && t.getMainTournamentId() != null
                ? tournamentRepository.findById(t.getMainTournamentId()).orElse(t)
                : t;
        // Portee globale (pas juste ce tournoi) : la teinte d'un tournoi en cours
        // doit etre EXACTEMENT la meme ici que dans la liste et le classement.
        List<Tournament> allMains = tournamentRepository.findAll().stream()
                .filter(x -> !x.isQualifying())
                .toList();
        TournamentProgress progress = TournamentProgress.compute(tournamentRepository, matchRepository, allMains);
        Integer hue = progress.hueByTournamentId(allMains).get(main.getId());
        return toDto(t, progress.statusOf(main), hue);
    }

    private TournamentDto toDto(Tournament t, TournamentStatus status, Integer colorHue) {
        int drawSlots = t.isQualifying() ? t.getDrawSize() : RoundLabels.nextPowerOfTwo(t.getDrawSize());
        List<RoundPointsDto> rounds = tournamentRoundRepository.findByTournamentIdOrderByRoundOrderAsc(t.getId())
                .stream()
                .map(r -> new RoundPointsDto(r.getRoundOrder(), r.getRoundLabel(), r.getPoints()))
                .toList();
        Long qualifyingTournamentId = t.isQualifying() ? null
                : tournamentRepository.findByMainTournamentId(t.getId()).map(Tournament::getId).orElse(null);
        return new TournamentDto(
                t.getId(), t.getName(), t.getCategory(), t.getSeason(), t.getWeekNumber(), t.getCountry(),
                t.getMandatorySlot(), t.getDrawSize(), drawSlots,
                t.getQualifyingRound1Points(), t.getQualifyingRound2Points(), t.getRunnerUpPoints(), rounds,
                t.isQualifying(), t.getMainTournamentId(), qualifyingTournamentId, status, colorHue
        );
    }
}
