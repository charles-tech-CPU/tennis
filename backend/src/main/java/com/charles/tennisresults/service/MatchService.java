package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.dto.MatchDto;
import com.charles.tennisresults.dto.ScoreUpdateDto;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRoundRepository tournamentRoundRepository;
    private final BracketService bracketService;

    public MatchService(MatchRepository matchRepository, TournamentRoundRepository tournamentRoundRepository,
                         BracketService bracketService) {
        this.matchRepository = matchRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.bracketService = bracketService;
    }

    public List<MatchDto> findByTournament(Long tournamentId) {
        Map<Integer, String> labels = tournamentRoundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId)
                .stream().collect(Collectors.toMap(r -> r.getRoundOrder(), r -> r.getRoundLabel()));
        return matchRepository.findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(tournamentId).stream()
                .map(m -> toDto(m, labels.get(m.getRoundOrder())))
                .toList();
    }

    @Transactional
    public MatchDto recordScore(Long matchId, ScoreUpdateDto dto) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match introuvable: " + matchId));

        if (match.getEntry1() == null || match.getEntry2() == null) {
            throw new IllegalArgumentException("Les deux joueurs de ce match ne sont pas encore connus.");
        }

        Entry newWinner;
        if (dto.winnerEntryId().equals(match.getEntry1().getId())) {
            newWinner = match.getEntry1();
        } else if (dto.winnerEntryId().equals(match.getEntry2().getId())) {
            newWinner = match.getEntry2();
        } else {
            throw new IllegalArgumentException("Le vainqueur doit etre l'un des deux joueurs du match.");
        }

        boolean wasAlreadyCompleted = match.getStatus() == MatchStatus.COMPLETED;
        if (wasAlreadyCompleted) {
            // on corrige un resultat deja saisi : on annule d'abord ce qui en decoulait plus loin
            bracketService.resetDownstream(match);
        }

        match.setScore(dto.score());
        match.setWinnerEntry(newWinner);
        match.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(match);

        bracketService.advanceWinner(match, newWinner);

        String label = tournamentRoundRepository.findByTournamentIdOrderByRoundOrderAsc(match.getTournament().getId())
                .stream()
                .filter(r -> r.getRoundOrder().equals(match.getRoundOrder()))
                .map(r -> r.getRoundLabel())
                .findFirst()
                .orElse(null);
        return toDto(match, label);
    }

    private MatchDto toDto(Match m, String roundLabel) {
        return new MatchDto(
                m.getId(),
                m.getTournament().getId(),
                m.getRoundOrder(),
                roundLabel,
                m.getPositionInRound(),
                toEntryDto(m.getEntry1()),
                toEntryDto(m.getEntry2()),
                m.getScore(),
                m.getWinnerEntry() != null ? m.getWinnerEntry().getId() : null,
                m.getStatus()
        );
    }

    private com.charles.tennisresults.dto.EntryDto toEntryDto(Entry e) {
        if (e == null) {
            return null;
        }
        return new com.charles.tennisresults.dto.EntryDto(
                e.getId(),
                e.getTournament().getId(),
                e.getPlayer() != null ? e.getPlayer().getId() : null,
                e.getPlayer() != null ? e.getPlayer().getLastName() : (e.isBye() ? "BYE" : null),
                e.getPlayer() != null ? e.getPlayer().getFirstName() : null,
                e.getDrawPosition(),
                e.getSeed(),
                e.getEntryType(),
                e.isBye()
        );
    }
}
