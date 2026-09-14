package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.*;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcule automatiquement le classement de chaque joueur pour une saison, en
 * reproduisant la logique de la formule Excel de Charles :
 *   total = (4 Grand Chelem + ATP Finals + 8 des 9 Masters 1000, hors Monte-Carlo)
 *         + (somme des 5 meilleurs "autres" tournois)
 *         + max(points a Monte-Carlo, 6e meilleur "autre" tournoi)
 *
 * Seuls les matchs COMPLETED/BYE comptent : un tournoi en cours, pour un joueur qui
 * n'a pas encore perdu, ne rapporte aucun point tant que son elimination (ou son
 * titre) n'est pas actee - comme pour le classement calcule dans lol-results et
 * foot-results, on ne devine jamais un resultat non joue.
 */
@Service
public class RankingService {

    private static final Set<MandatorySlot> MANDATORY_EXCLUDING_MC = EnumSet.complementOf(
            EnumSet.of(MandatorySlot.MONTE_CARLO));

    private final EntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final TournamentRoundRepository tournamentRoundRepository;

    public RankingService(EntryRepository entryRepository, MatchRepository matchRepository,
                           TournamentRoundRepository tournamentRoundRepository) {
        this.entryRepository = entryRepository;
        this.matchRepository = matchRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
    }

    public List<RankingRowDto> computeRanking(int season) {
        List<Entry> entries = entryRepository.findByTournament_SeasonAndPlayerIsNotNull(season);

        Map<Player, List<Entry>> byPlayer = entries.stream()
                .collect(Collectors.groupingBy(Entry::getPlayer));

        List<RankingRowDto> rows = new ArrayList<>();
        for (Map.Entry<Player, List<Entry>> e : byPlayer.entrySet()) {
            Player player = e.getKey();
            int mandatoryTotal = 0;
            int monteCarloPoints = 0;
            List<Integer> othersPoints = new ArrayList<>();

            for (Entry entry : e.getValue()) {
                int points = pointsEarned(entry);
                MandatorySlot slot = entry.getTournament().getMandatorySlot();
                if (slot == MandatorySlot.MONTE_CARLO) {
                    monteCarloPoints = Math.max(monteCarloPoints, points);
                } else if (slot != null) {
                    mandatoryTotal += points;
                } else {
                    othersPoints.add(points);
                }
            }

            othersPoints.sort(Comparator.reverseOrder());
            int best5 = othersPoints.stream().limit(5).mapToInt(Integer::intValue).sum();
            int sixthBest = othersPoints.size() > 5 ? othersPoints.get(5) : 0;
            int replacementValue = Math.max(monteCarloPoints, sixthBest);
            int total = mandatoryTotal + best5 + replacementValue;

            rows.add(new RankingRowDto(player.getId(), player.getLastName(), player.getFirstName(),
                    player.getNationality(), mandatoryTotal, best5, replacementValue, total));
        }

        rows.sort(Comparator.comparingInt(RankingRowDto::total).reversed());
        return rows;
    }

    /** Points gagnes par cette entree dans son tournoi, uniquement d'apres des matchs decides. */
    private int pointsEarned(Entry entry) {
        Tournament tournament = entry.getTournament();
        int totalRounds = tournament.getDrawSize() == null ? 0
                : RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(tournament.getDrawSize()));
        if (totalRounds == 0) {
            return 0;
        }
        Map<Integer, Integer> pointsByRound = tournamentRoundRepository
                .findByTournamentIdOrderByRoundOrderAsc(tournament.getId())
                .stream()
                .collect(Collectors.toMap(TournamentRound::getRoundOrder, TournamentRound::getPoints));

        List<Match> matches = matchRepository.findByEntry1_IdOrEntry2_Id(entry.getId(), entry.getId());

        Match deepest = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED || m.getStatus() == MatchStatus.BYE)
                .max(Comparator.comparingInt(Match::getRoundOrder))
                .orElse(null);

        if (deepest == null) {
            return 0;
        }

        boolean won = deepest.getWinnerEntry() != null && deepest.getWinnerEntry().getId().equals(entry.getId());
        int r = deepest.getRoundOrder();

        if (won) {
            if (r == totalRounds) {
                return pointsByRound.getOrDefault(totalRounds, 0);
            }
            // a gagne ce tour mais le tour suivant n'est pas encore decide : pas de points tant
            // que son elimination (ou son titre) n'est pas actee.
            return 0;
        } else {
            if (r == totalRounds) {
                return tournament.getRunnerUpPoints() != null
                        ? tournament.getRunnerUpPoints()
                        : pointsByRound.getOrDefault(totalRounds - 1, 0);
            }
            return pointsByRound.getOrDefault(r, 0);
        }
    }
}
