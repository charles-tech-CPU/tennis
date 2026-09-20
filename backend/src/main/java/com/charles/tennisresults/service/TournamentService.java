package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.QualifyingCreateDto;
import com.charles.tennisresults.dto.RoundPointsDto;
import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentDto;
import com.charles.tennisresults.dto.TournamentStatus;
import com.charles.tennisresults.dto.TournamentUpdateDto;
import com.charles.tennisresults.dto.TournamentWinnerDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public List<TournamentDto> findAll() {
        List<Tournament> tournaments = tournamentRepository.findAll().stream()
                .filter(t -> !t.isQualifying())
                .toList();

        // Un seul aller-retour pour tous les matchs decides de tous les tournois
        // (+ leurs qualifs), plutot qu'une requete par tournoi (N+1) juste pour
        // colorer la liste.
        TournamentProgress progress = TournamentProgress.compute(tournamentRepository, matchRepository, tournaments);
        Map<Long, Integer> hues = progress.hueByTournamentId(tournaments);

        // Ordre demande par Charles : les tournois en cours en tete de liste,
        // puis semaine, puis importance de la categorie (Grand Chelem > Masters
        // 1000 > ATP 500 > ... > ATP 50 - exactement l'ordre de declaration de
        // l'enum TournamentCategory, donc son ordinal).
        return tournaments.stream()
                .map(t -> toDto(t, progress.statusOf(t), hues.get(t.getId())))
                .sorted(Comparator.comparing((TournamentDto t) -> t.status() == TournamentStatus.IN_PROGRESS ? 0 : 1)
                        .thenComparing(Comparator.comparing(TournamentDto::season).reversed())
                        .thenComparing(t -> t.weekNumber() == null ? 0 : t.weekNumber())
                        .thenComparing(t -> t.category().ordinal())
                        .thenComparing(TournamentDto::name))
                .toList();
    }

    @Transactional(readOnly = true)
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

        if (dto.drawSize() != null && !dto.drawSize().equals(t.getDrawSize())) {
            resizeDraw(t, dto.drawSize());
        }

        if (dto.rounds() != null) {
            for (RoundPointsDto roundDto : dto.rounds()) {
                t.getRounds().stream()
                        .filter(r -> r.getRoundOrder().equals(roundDto.roundOrder()))
                        .findFirst()
                        .ifPresent(r -> r.setPoints(roundDto.points()));
            }
        }

        // Les qualifs partagent leur semaine avec le tableau principal "par
        // construction" (cf. EntryService) : si on deplace le principal sans
        // repercuter le changement sur ses qualifs, les deux se desynchronisent
        // et cassent la detection de conflit de semaine (Charles, 2026-09-20 -
        // exactement le bug qui a touche Madrid).
        if (!t.isQualifying()) {
            tournamentRepository.findByMainTournamentId(t.getId())
                    .ifPresent(q -> q.setWeekNumber(computeQualifyingWeekNumber(t)));
        }

        return toDto(t);
    }

    /**
     * Change la taille reelle du tableau principal (ex: Rotterdam cree a tort
     * en 48 au lieu de 32, Charles 2026-09-16) - uniquement possible tant
     * qu'aucun joueur n'est encore place (sinon les positions/matchs deja
     * saisis n'auraient plus de sens). Regenere entierement le bareme par
     * defaut de la categorie et le squelette (vide) du tableau.
     */
    private void resizeDraw(Tournament t, int newDrawSize) {
        if (t.isQualifying()) {
            throw new IllegalArgumentException(
                    "Redimensionner un tableau de qualifications n'est pas supporte : supprime-le et recree-le.");
        }
        if (entryRepository.countByTournamentId(t.getId()) > 0) {
            throw new IllegalArgumentException(
                    "Ce tournoi a deja des joueurs places dans son tableau : retire-les d'abord avant de changer sa taille.");
        }

        matchRepository.deleteByTournamentId(t.getId());
        // Suppression immediate (flush), pas juste orpheline en fin de
        // transaction : sinon les INSERT des nouveaux tours plus bas entrent en
        // conflit avec la contrainte unique (tournament_id, round_order) tant
        // que les anciens tours ne sont pas encore reellement effaces en base.
        List<TournamentRound> oldRounds = new ArrayList<>(t.getRounds());
        t.getRounds().clear();
        tournamentRoundRepository.deleteAll(oldRounds);
        tournamentRoundRepository.flush();

        t.setDrawSize(newDrawSize);
        int drawSlots = RoundLabels.nextPowerOfTwo(newDrawSize);
        int totalRounds = RoundLabels.roundCount(drawSlots);
        List<Integer> points = CategoryDefaults.pointsFor(t.getCategory(), drawSlots);
        for (int r = 1; r <= totalRounds; r++) {
            int pts = (r - 1) < points.size() ? points.get(r - 1) : points.get(points.size() - 1);
            tournamentRoundRepository.save(new TournamentRound(t, r, RoundLabels.labelFor(r, totalRounds), pts));
        }
        bracketService.initializeSkeleton(t, drawSlots);
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
        q.setWeekNumber(computeQualifyingWeekNumber(main));
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

    /**
     * Les Grand Chelem et les Masters 1000 jouent leurs qualifs la semaine
     * PRECEDENT le tableau principal (evenement a part sur le calendrier,
     * meme pour les Masters 1000 sur 1 semaine comme Madrid ou Monte Carlo :
     * les qualifs se jouent le week-end avant) ; pour toutes les autres
     * categories, qualifs et tableau principal sont la meme semaine (Charles,
     * 2026-09-15 - ex: Watanuki inscrit a Phan Thiet la meme semaine que les
     * qualifs de l'Australian Open). Charles, 2026-09-20 - generalise a tous
     * les Masters 1000 (pas seulement Indian Wells/Miami) suite au meme bug
     * sur Madrid : ses qualifs etaient restees a la meme semaine (ou apres,
     * une fois le tableau principal deplace) que le tableau principal,
     * empechant a tort les joueurs elimines des qualifs de s'inscrire a un
     * tournoi la semaine du tableau principal.
     */
    private Integer computeQualifyingWeekNumber(Tournament main) {
        boolean qualifsWeekBefore = main.getCategory() == TournamentCategory.GRAND_SLAM
                || main.getCategory() == TournamentCategory.MASTERS_1000;
        return qualifsWeekBefore && main.getWeekNumber() != null
                ? main.getWeekNumber() - 1
                : main.getWeekNumber();
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
                t.isQualifying(), t.getMainTournamentId(), qualifyingTournamentId, status, colorHue,
                winnerOf(t, status)
        );
    }

    /** Vainqueur de la finale du tableau principal, uniquement si le tournoi est termine. */
    private TournamentWinnerDto winnerOf(Tournament t, TournamentStatus status) {
        if (t.isQualifying() || status != TournamentStatus.COMPLETED) {
            return null;
        }
        int totalRounds = RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(t.getDrawSize()));
        return matchRepository.findByTournamentIdAndRoundOrderAndPositionInRound(t.getId(), totalRounds, 1)
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED && m.getWinnerEntry() != null
                        && m.getWinnerEntry().getPlayer() != null)
                .map(Match::getWinnerEntry)
                .map(e -> new TournamentWinnerDto(e.getPlayer().getId(), e.getPlayer().getLastName(),
                        e.getPlayer().getFirstName(), e.getPlayer().getNationality()))
                .orElse(null);
    }
}
