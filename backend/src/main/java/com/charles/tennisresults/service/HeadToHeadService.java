package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.HeadToHeadDto;
import com.charles.tennisresults.dto.HeadToHeadMatchDto;
import com.charles.tennisresults.dto.PlayerDto;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.PlayerRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Face a face entre deux joueurs : tous leurs matchs decides (COMPLETED/BYE
 * avec un vainqueur), tous tournois et saisons confondus - qualifs comprises.
 */
@Service
public class HeadToHeadService {

    // Du plus recent au plus ancien : saison, puis semaine ATP (deux tournois
    // d'une meme saison), puis tableau principal avant ses qualifs (joue
    // apres), puis tour le plus avance.
    private static final Comparator<Match> MOST_RECENT_FIRST = Comparator.<Match, Integer>comparing(
                    m -> m.getTournament().getSeason(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(m -> m.getTournament().getWeekNumber(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(m -> m.getTournament().isQualifying())
            .thenComparing(Match::getRoundOrder, Comparator.reverseOrder());

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRoundRepository tournamentRoundRepository;

    public HeadToHeadService(
            MatchRepository matchRepository,
            PlayerRepository playerRepository,
            TournamentRoundRepository tournamentRoundRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
    }

    @Transactional(readOnly = true)
    public HeadToHeadDto headToHead(Long player1Id, Long player2Id) {
        if (player1Id.equals(player2Id)) {
            throw new IllegalArgumentException("Choisir deux joueurs differents.");
        }
        Player player1 = findPlayer(player1Id);
        Player player2 = findPlayer(player2Id);

        List<Match> matches =
                matchRepository
                        .findHeadToHead(player1Id, player2Id, List.of(MatchStatus.COMPLETED, MatchStatus.BYE))
                        .stream()
                        .sorted(MOST_RECENT_FIRST)
                        .toList();

        Map<Long, Map<Integer, String>> labelsByTournamentId = roundLabels(matches);
        List<HeadToHeadMatchDto> dtos =
                matches.stream().map(m -> toDto(m, labelsByTournamentId)).toList();

        int player1Wins = (int)
                dtos.stream().filter(m -> player1Id.equals(m.winnerPlayerId())).count();
        return new HeadToHeadDto(toDto(player1), toDto(player2), player1Wins, dtos.size() - player1Wins, dtos);
    }

    private Player findPlayer(Long id) {
        return playerRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable: " + id));
    }

    /** Libelles des tours (bareme du tournoi) de tous les tournois concernes, en une requete. */
    private Map<Long, Map<Integer, String>> roundLabels(List<Match> matches) {
        Set<Long> tournamentIds =
                matches.stream().map(m -> m.getTournament().getId()).collect(Collectors.toSet());
        if (tournamentIds.isEmpty()) {
            return Map.of();
        }
        return tournamentRoundRepository.findByTournamentIdIn(tournamentIds).stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTournament().getId(),
                        Collectors.toMap(TournamentRound::getRoundOrder, TournamentRound::getRoundLabel)));
    }

    private static HeadToHeadMatchDto toDto(Match m, Map<Long, Map<Integer, String>> labelsByTournamentId) {
        Tournament t = m.getTournament();
        String label = labelsByTournamentId
                .getOrDefault(t.getId(), Map.of())
                .getOrDefault(m.getRoundOrder(), fallbackLabel(t, m.getRoundOrder()));
        return new HeadToHeadMatchDto(
                m.getId(),
                t.getId(),
                t.getName(),
                t.getSeason(),
                t.isQualifying(),
                m.getRoundOrder(),
                label,
                m.getScore(),
                m.getWinnerEntry().getPlayer().getId());
    }

    /** Libelle deduit de la taille du tableau, si le bareme du tournoi n'en fournit pas. */
    private static String fallbackLabel(Tournament t, int roundOrder) {
        if (t.isQualifying()) {
            return RoundLabels.qualifyingLabelFor(roundOrder);
        }
        if (t.getDrawSize() == null) {
            return null;
        }
        return RoundLabels.labelFor(roundOrder, RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(t.getDrawSize())));
    }

    private static PlayerDto toDto(Player p) {
        return new PlayerDto(
                p.getId(), p.getLastName(), p.getFirstName(), p.getNationality(), p.getLegacySnapshotPoints());
    }
}
