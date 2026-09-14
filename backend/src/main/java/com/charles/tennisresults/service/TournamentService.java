package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.RoundPointsDto;
import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentDto;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRoundRepository tournamentRoundRepository;
    private final BracketService bracketService;

    public TournamentService(TournamentRepository tournamentRepository,
                              TournamentRoundRepository tournamentRoundRepository,
                              BracketService bracketService) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.bracketService = bracketService;
    }

    public List<TournamentDto> findAll() {
        return tournamentRepository.findAll().stream()
                .map(this::toDto)
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

    private Tournament getOrThrow(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournoi introuvable: " + id));
    }

    private TournamentDto toDto(Tournament t) {
        int drawSlots = RoundLabels.nextPowerOfTwo(t.getDrawSize());
        List<RoundPointsDto> rounds = tournamentRoundRepository.findByTournamentIdOrderByRoundOrderAsc(t.getId())
                .stream()
                .map(r -> new RoundPointsDto(r.getRoundOrder(), r.getRoundLabel(), r.getPoints()))
                .toList();
        return new TournamentDto(
                t.getId(), t.getName(), t.getCategory(), t.getSeason(), t.getWeekNumber(), t.getCountry(),
                t.getMandatorySlot(), t.getDrawSize(), drawSlots,
                t.getQualifyingRound1Points(), t.getQualifyingRound2Points(), t.getRunnerUpPoints(), rounds
        );
    }
}
