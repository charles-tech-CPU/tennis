package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.EntryType;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.dto.NationCountDto;
import com.charles.tennisresults.dto.PlayerCountDto;
import com.charles.tennisresults.dto.QualifierRunDto;
import com.charles.tennisresults.dto.StatsDto;
import com.charles.tennisresults.dto.StreakDto;
import com.charles.tennisresults.dto.WinRateDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stats saisonnieres, pensees pour etre etendues au fur et a mesure des
 * demandes de Charles (voir StatsDto) plutot que comme une liste figee.
 * Contrairement au classement (glissant sur 52 semaines, voir RankingService),
 * une stat "de l'annee" reste rattachee a la saison civile (`Tournament.season`).
 */
@Service
public class StatsService {

    private static final int TOP_N = 5;
    /** En dessous, un % de victoires n'est pas significatif (ex: 1 victoire sur 1 match = 100%). */
    private static final int MIN_MATCHES_FOR_WIN_RATE = 10;
    /** "6-3", "7-6(4)" ... - le nombre de jeux du VAINQUEUR est toujours le 1er (convention standard). */
    private static final Pattern SET_PATTERN = Pattern.compile("(\\d+)-(\\d+)(?:\\(\\d+\\))?");

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final EntryRepository entryRepository;

    public StatsService(
            TournamentRepository tournamentRepository,
            MatchRepository matchRepository,
            EntryRepository entryRepository) {
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public StatsDto computeStats(int season) {
        List<Tournament> mains = tournamentRepository.findBySeason(season).stream()
                .filter(t -> !t.isQualifying())
                .toList();
        List<Long> mainIds = mains.stream().map(Tournament::getId).toList();

        List<Long> allIds = new ArrayList<>(mainIds);
        tournamentRepository.findAll().stream()
                .filter(Tournament::isQualifying)
                .filter(q -> mainIds.contains(q.getMainTournamentId()))
                .forEach(q -> allIds.add(q.getId()));

        List<Match> completedMatches =
                matchRepository.findByTournament_IdInAndStatusIn(allIds, List.of(MatchStatus.COMPLETED));

        Map<Player, Long> titlesByPlayer = titlesByPlayer(mains);

        return new StatsDto(
                season,
                toRankedPlayers(titlesByPlayer),
                toRankedPlayers(matchWinsByPlayer(completedMatches)),
                toRankedPlayers(titlesByPlayer(filterByCategory(mains, TournamentCategory.GRAND_SLAM))),
                toRankedPlayers(titlesByPlayer(filterByCategory(mains, TournamentCategory.MASTERS_1000))),
                topNationsByTitles(titlesByPlayer),
                toRankedPlayers(runnersUpByPlayer(mains)),
                bestQualifierRuns(mains),
                topWinRate(completedMatches),
                longestWinStreaks(completedMatches),
                toRankedPlayers(bagelsInflictedByPlayer(completedMatches)),
                toRankedPlayers(epicWinsByPlayer(completedMatches)),
                toRankedPlayers(activityByPlayer(allIds)));
    }

    private List<Tournament> filterByCategory(List<Tournament> mains, TournamentCategory category) {
        return mains.stream().filter(t -> t.getCategory() == category).toList();
    }

    private Map<Player, Long> titlesByPlayer(List<Tournament> mains) {
        Map<Player, Long> titles = new HashMap<>();
        for (Tournament t : mains) {
            int totalRounds = RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(t.getDrawSize()));
            matchRepository
                    .findByTournamentIdAndRoundOrderAndPositionInRound(t.getId(), totalRounds, 1)
                    .filter(m -> m.getStatus() == MatchStatus.COMPLETED
                            && m.getWinnerEntry() != null
                            && m.getWinnerEntry().getPlayer() != null)
                    .ifPresent(m -> titles.merge(m.getWinnerEntry().getPlayer(), 1L, Long::sum));
        }
        return titles;
    }

    private Map<Player, Long> matchWinsByPlayer(List<Match> completedMatches) {
        return completedMatches.stream()
                .filter(m -> m.getWinnerEntry() != null && m.getWinnerEntry().getPlayer() != null)
                .collect(Collectors.groupingBy(m -> m.getWinnerEntry().getPlayer(), Collectors.counting()));
    }

    private List<NationCountDto> topNationsByTitles(Map<Player, Long> titlesByPlayer) {
        Map<String, Long> byNation = new HashMap<>();
        titlesByPlayer.forEach((player, count) -> {
            if (player.getNationality() != null) {
                byNation.merge(player.getNationality(), count, Long::sum);
            }
        });
        return byNation.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_N)
                .map(e -> new NationCountDto(e.getKey(), e.getValue()))
                .toList();
    }

    private Map<Player, Long> runnersUpByPlayer(List<Tournament> mains) {
        Map<Player, Long> runnersUp = new HashMap<>();
        for (Tournament t : mains) {
            int totalRounds = RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(t.getDrawSize()));
            matchRepository
                    .findByTournamentIdAndRoundOrderAndPositionInRound(t.getId(), totalRounds, 1)
                    .filter(m -> m.getStatus() == MatchStatus.COMPLETED && m.getWinnerEntry() != null)
                    .ifPresent(m -> {
                        Entry loser = opponentOf(m, m.getWinnerEntry());
                        if (loser != null && loser.getPlayer() != null) {
                            runnersUp.merge(loser.getPlayer(), 1L, Long::sum);
                        }
                    });
        }
        return runnersUp;
    }

    /** Meilleur parcours d'un joueur passe par les qualifs, dans le tableau principal. */
    private List<QualifierRunDto> bestQualifierRuns(List<Tournament> mains) {
        record Run(Entry entry, Tournament tournament, int roundsWon) {}
        List<Run> runs = new ArrayList<>();
        for (Tournament t : mains) {
            List<Entry> qualifierEntries = entryRepository.findByTournamentIdOrderByDrawPositionAsc(t.getId()).stream()
                    .filter(e -> e.getEntryType() == EntryType.QUALIFIER && e.getPlayer() != null)
                    .toList();
            if (qualifierEntries.isEmpty()) {
                continue;
            }
            List<Match> matches = matchRepository.findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(t.getId());
            for (Entry qualifier : qualifierEntries) {
                long roundsWon = matches.stream()
                        .filter(m -> m.getStatus() == MatchStatus.COMPLETED
                                && m.getWinnerEntry() != null
                                && m.getWinnerEntry().getId().equals(qualifier.getId()))
                        .count();
                if (roundsWon > 0) {
                    runs.add(new Run(qualifier, t, (int) roundsWon));
                }
            }
        }
        return runs.stream()
                .sorted(Comparator.comparingInt(Run::roundsWon).reversed())
                .limit(TOP_N)
                .map(r -> {
                    int totalRounds = RoundLabels.roundCount(
                            RoundLabels.nextPowerOfTwo(r.tournament().getDrawSize()));
                    String roundReached = r.roundsWon() == totalRounds
                            ? "Titre"
                            : RoundLabels.labelFor(r.roundsWon() + 1, totalRounds);
                    Player p = r.entry().getPlayer();
                    return new QualifierRunDto(
                            p.getId(),
                            p.getLastName(),
                            p.getFirstName(),
                            p.getNationality(),
                            r.tournament().getName(),
                            roundReached);
                })
                .toList();
    }

    private List<WinRateDto> topWinRate(List<Match> completedMatches) {
        Map<Player, long[]> tally = new HashMap<>(); // [victoires, defaites]
        for (Match m : completedMatches) {
            Entry winner = m.getWinnerEntry();
            if (winner == null || winner.getPlayer() == null) {
                continue;
            }
            tally.computeIfAbsent(winner.getPlayer(), k -> new long[2])[0]++;
            Entry loser = opponentOf(m, winner);
            if (loser != null && loser.getPlayer() != null) {
                tally.computeIfAbsent(loser.getPlayer(), k -> new long[2])[1]++;
            }
        }
        return tally.entrySet().stream()
                .filter(e -> e.getValue()[0] + e.getValue()[1] >= MIN_MATCHES_FOR_WIN_RATE)
                .sorted(Comparator.<Map.Entry<Player, long[]>>comparingDouble(
                                e -> e.getValue()[0] / (double) (e.getValue()[0] + e.getValue()[1]))
                        .reversed())
                .limit(TOP_N)
                .map(e -> {
                    Player p = e.getKey();
                    long wins = e.getValue()[0];
                    long losses = e.getValue()[1];
                    return new WinRateDto(
                            p.getId(),
                            p.getLastName(),
                            p.getFirstName(),
                            p.getNationality(),
                            wins,
                            losses,
                            wins / (double) (wins + losses));
                })
                .toList();
    }

    private static Map<Player, List<Match>> matchesByPlayer(List<Match> matches) {
        Map<Player, List<Match>> byPlayer = new HashMap<>();
        for (Match m : matches) {
            for (Entry e : Arrays.asList(m.getEntry1(), m.getEntry2())) {
                if (e != null && e.getPlayer() != null) {
                    byPlayer.computeIfAbsent(e.getPlayer(), k -> new ArrayList<>())
                            .add(m);
                }
            }
        }
        return byPlayer;
    }

    /** Vrai si l'entree e (cote du match) est celle de ce joueur et qu'elle a gagne. */
    private static boolean wonBy(Match m, Entry e, Player player) {
        return e != null
                && e.getPlayer() == player
                && m.getWinnerEntry() != null
                && m.getWinnerEntry().getId().equals(e.getId());
    }

    /**
     * Approximation : les matchs n'ont pas de date, seulement un numero de semaine
     * ATP par tournoi. On ordonne donc chronologiquement par semaine, puis qualifs
     * avant tableau principal pour une semaine donnee, puis par tour - suffisant
     * pour une serie de victoires "de saison", pas une horloge exacte.
     */
    private List<StreakDto> longestWinStreaks(List<Match> completedMatches) {
        Map<Player, List<Match>> byPlayer = matchesByPlayer(completedMatches);
        Comparator<Match> chronological = Comparator.<Match>comparingInt(m ->
                        Optional.ofNullable(m.getTournament().getWeekNumber()).orElse(Integer.MAX_VALUE))
                .thenComparingInt(m -> m.getTournament().isQualifying() ? 0 : 1)
                .thenComparingLong(m -> m.getTournament().getId())
                .thenComparingInt(Match::getRoundOrder);

        List<StreakDto> streaks = new ArrayList<>();
        for (Map.Entry<Player, List<Match>> entry : byPlayer.entrySet()) {
            Player player = entry.getKey();
            List<Match> matches =
                    entry.getValue().stream().sorted(chronological).toList();
            int best = 0;
            int current = 0;
            for (Match m : matches) {
                boolean won = wonBy(m, m.getEntry1(), player) || wonBy(m, m.getEntry2(), player);
                current = won ? current + 1 : 0;
                best = Math.max(best, current);
            }
            if (best > 0) {
                streaks.add(new StreakDto(
                        player.getId(), player.getLastName(), player.getFirstName(), player.getNationality(), best));
            }
        }
        return streaks.stream()
                .sorted(Comparator.comparingInt(StreakDto::streakLength).reversed())
                .limit(TOP_N)
                .toList();
    }

    private Map<Player, Long> bagelsInflictedByPlayer(List<Match> completedMatches) {
        Map<Player, Long> counts = new HashMap<>();
        for (Match m : completedMatches) {
            Entry winner = m.getWinnerEntry();
            if (winner == null || winner.getPlayer() == null || m.getScore() == null) {
                continue;
            }
            long bagels = parseSets(m.getScore()).stream()
                    .filter(set -> set[0] >= 6 && set[1] == 0)
                    .count();
            if (bagels > 0) {
                counts.merge(winner.getPlayer(), bagels, Long::sum);
            }
        }
        return counts;
    }

    /** Victoire "a la distance" : tous les sets possibles ont ete joues (3 sets, ou 5 en Grand Chelem). */
    private Map<Player, Long> epicWinsByPlayer(List<Match> completedMatches) {
        Map<Player, Long> counts = new HashMap<>();
        for (Match m : completedMatches) {
            Entry winner = m.getWinnerEntry();
            if (winner == null || winner.getPlayer() == null || m.getScore() == null) {
                continue;
            }
            int distance = m.getTournament().getCategory() == TournamentCategory.GRAND_SLAM ? 5 : 3;
            if (parseSets(m.getScore()).size() == distance) {
                counts.merge(winner.getPlayer(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private Map<Player, Long> activityByPlayer(List<Long> tournamentIds) {
        return entryRepository.findByTournament_IdIn(tournamentIds).stream()
                .filter(e -> !e.isBye() && e.getPlayer() != null)
                .collect(Collectors.groupingBy(Entry::getPlayer, Collectors.counting()));
    }

    private Entry opponentOf(Match m, Entry entry) {
        if (m.getEntry1() != null && m.getEntry1().getId().equals(entry.getId())) {
            return m.getEntry2();
        }
        if (m.getEntry2() != null && m.getEntry2().getId().equals(entry.getId())) {
            return m.getEntry1();
        }
        return null;
    }

    /** "6-3 7-6(4) ..." -> [[6,3],[7,6]...] ; ignore les tokens hors format (w.o., ab., ...). */
    private List<int[]> parseSets(String score) {
        List<int[]> sets = new ArrayList<>();
        for (String token : score.trim().split("\\s+")) {
            Matcher matcher = SET_PATTERN.matcher(token);
            if (matcher.matches()) {
                sets.add(new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))});
            }
        }
        return sets;
    }

    private List<PlayerCountDto> toRankedPlayers(Map<Player, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Player, Long>comparingByValue().reversed())
                .limit(TOP_N)
                .map(e -> new PlayerCountDto(
                        e.getKey().getId(),
                        e.getKey().getLastName(),
                        e.getKey().getFirstName(),
                        e.getKey().getNationality(),
                        e.getValue()))
                .toList();
    }
}
