package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.CategoryBestResultDto;
import com.charles.tennisresults.dto.PlayerDto;
import com.charles.tennisresults.dto.PlayerProfileDto;
import com.charles.tennisresults.dto.PlayerRecordDto;
import com.charles.tennisresults.dto.PlayerTournamentResultDto;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.PlayerRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fiche d'un joueur : position au classement (calcule par
 * {@link RankingService}, comme /api/ranking), bilan, titres, meilleur
 * resultat par categorie et historique de ses tournois.
 *
 * Comme dans le classement, un tableau de qualifs et son tableau principal
 * forment un seul tournoi : un joueur sorti des qualifs puis battu en 8e a
 * un seul resultat (R16), dont les points cumulent qualifs + tableau principal.
 */
@Service
public class PlayerProfileService {

    private static final List<MatchStatus> DECIDED = List.of(MatchStatus.COMPLETED, MatchStatus.BYE);

    // Du plus recent au plus ancien : saison puis semaine ATP.
    private static final Comparator<PlayerTournamentResultDto> MOST_RECENT_FIRST = Comparator.comparing(
                    PlayerTournamentResultDto::season, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PlayerTournamentResultDto::week, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PlayerTournamentResultDto::tournamentName);

    private final PlayerRepository playerRepository;
    private final EntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRoundRepository tournamentRoundRepository;
    private final RankingService rankingService;

    public PlayerProfileService(
            PlayerRepository playerRepository,
            EntryRepository entryRepository,
            MatchRepository matchRepository,
            TournamentRepository tournamentRepository,
            TournamentRoundRepository tournamentRoundRepository,
            RankingService rankingService) {
        this.playerRepository = playerRepository;
        this.entryRepository = entryRepository;
        this.matchRepository = matchRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.rankingService = rankingService;
    }

    /** Parcours d'une entree (un tableau : principal OU qualifs). */
    private record Run(Entry entry, int reachedRound, boolean won, boolean inProgress, int points) {

        Tournament tournament() {
            return entry.getTournament();
        }
    }

    @Transactional(readOnly = true)
    public PlayerProfileDto profile(Long playerId) {
        Player player = playerRepository
                .findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable: " + playerId));

        List<Entry> entries = entryRepository.findByPlayerId(playerId);
        Set<Long> entryIds = entries.stream().map(Entry::getId).collect(Collectors.toSet());
        Set<Long> tournamentIds =
                entries.stream().map(e -> e.getTournament().getId()).collect(Collectors.toSet());

        Map<Long, List<Match>> matchesByEntryId = new HashMap<>();
        List<TournamentRound> rounds = List.of();
        Map<Long, Tournament> mainsById = Map.of();
        if (!entries.isEmpty()) {
            for (Match m : matchRepository.findByEntryIdsAndStatusIn(entryIds, DECIDED)) {
                for (Entry side : new Entry[] {m.getEntry1(), m.getEntry2()}) {
                    if (side != null && entryIds.contains(side.getId())) {
                        matchesByEntryId
                                .computeIfAbsent(side.getId(), k -> new ArrayList<>())
                                .add(m);
                    }
                }
            }
            rounds = tournamentRoundRepository.findByTournamentIdIn(tournamentIds);
            Set<Long> mainIds = entries.stream()
                    .map(Entry::getTournament)
                    .filter(Tournament::isQualifying)
                    .map(Tournament::getMainTournamentId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            mainsById = tournamentRepository.findAllById(mainIds).stream()
                    .collect(Collectors.toMap(Tournament::getId, t -> t));
        }

        Map<Long, Map<Integer, Integer>> pointsByRound = rounds.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTournament().getId(),
                        Collectors.toMap(TournamentRound::getRoundOrder, TournamentRound::getPoints)));
        Map<Long, Map<Integer, String>> labels = rounds.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTournament().getId(),
                        Collectors.toMap(TournamentRound::getRoundOrder, TournamentRound::getRoundLabel)));

        // Qualifs + tableau principal regroupes sous le tournoi principal.
        Map<Long, List<Run>> runsByMainId = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Tournament t = entry.getTournament();
            Run run = run(
                    entry,
                    pointsByRound.getOrDefault(t.getId(), Map.of()),
                    matchesByEntryId.getOrDefault(entry.getId(), List.of()));
            Tournament main = t.isQualifying() ? mainsById.getOrDefault(t.getMainTournamentId(), t) : t;
            runsByMainId.computeIfAbsent(main.getId(), k -> new ArrayList<>()).add(run);
        }

        List<PlayerTournamentResultDto> tournaments = new ArrayList<>();
        for (Map.Entry<Long, List<Run>> e : runsByMainId.entrySet()) {
            tournaments.add(result(e.getValue(), mainsById, labels));
        }
        tournaments.sort(MOST_RECENT_FIRST);

        List<PlayerTournamentResultDto> titles =
                tournaments.stream().filter(PlayerTournamentResultDto::champion).toList();

        return withRanking(
                player,
                record(entryIds, matchesByEntryId),
                titles,
                bestByCategory(tournaments, runsByMainId),
                tournaments);
    }

    private PlayerProfileDto withRanking(
            Player player,
            PlayerRecordDto record,
            List<PlayerTournamentResultDto> titles,
            List<CategoryBestResultDto> bestByCategory,
            List<PlayerTournamentResultDto> tournaments) {
        List<RankingRowDto> ranking = rankingService.computeRanking();
        Integer position = null;
        Integer total = null;
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).playerId().equals(player.getId())) {
                position = i + 1;
                total = ranking.get(i).total();
                break;
            }
        }
        return new PlayerProfileDto(
                PlayerDto.from(player), position, total, ranking.size(), record, titles, bestByCategory, tournaments);
    }

    /**
     * Tour atteint par cette entree, d'apres ses seuls matchs decides (meme
     * principe que le classement) : battu au tour r -> r ; vainqueur du tour r
     * pas encore suivi d'un resultat -> r + 1, encore en jeu.
     */
    private Run run(Entry entry, Map<Integer, Integer> pointsByRound, List<Match> matches) {
        int points = rankingService.pointsEarned(entry, pointsByRound, matches);
        int totalRounds = RankingService.totalRounds(entry.getTournament(), pointsByRound);
        Match deepest = matches.stream()
                .max(Comparator.comparingInt(Match::getRoundOrder))
                .orElse(null);
        if (deepest == null) {
            return new Run(entry, 1, false, true, points);
        }
        boolean wonDeepest = deepest.getWinnerEntry() != null
                && deepest.getWinnerEntry().getId().equals(entry.getId());
        int r = deepest.getRoundOrder();
        if (!wonDeepest) {
            return new Run(entry, r, false, false, points);
        }
        if (totalRounds > 0 && r >= totalRounds) {
            return new Run(entry, r, true, false, points); // champion (ou qualifie)
        }
        return new Run(entry, r + 1, false, true, points);
    }

    /** Resultat fusionne d'un tournoi : le tableau principal prime sur les qualifs. */
    private PlayerTournamentResultDto result(
            List<Run> runs, Map<Long, Tournament> mainsById, Map<Long, Map<Integer, String>> labels) {
        Run mainRun = runs.stream()
                .filter(r -> !r.tournament().isQualifying())
                .findFirst()
                .orElse(null);
        Run qualifyingRun = runs.stream()
                .filter(r -> r.tournament().isQualifying())
                .findFirst()
                .orElse(null);
        Run shown = mainRun != null ? mainRun : qualifyingRun;
        Tournament t = shown.tournament();
        Tournament main = t.isQualifying() ? mainsById.getOrDefault(t.getMainTournamentId(), t) : t;

        String label = labels.getOrDefault(t.getId(), Map.of())
                .getOrDefault(shown.reachedRound(), RoundLabels.labelFor(t, shown.reachedRound()));
        return new PlayerTournamentResultDto(
                main.getId(),
                main.getName(),
                main.getSeason(),
                main.getWeekNumber(),
                main.getCategory(),
                label,
                mainRun != null && mainRun.won(),
                shown.inProgress(),
                qualifyingRun != null,
                runs.stream().mapToInt(Run::points).sum());
    }

    /**
     * Matchs reellement joues (COMPLETED) : un bye (BYE) n'est pas un match
     * dispute et gonflerait artificiellement le nombre de victoires.
     */
    private PlayerRecordDto record(Set<Long> entryIds, Map<Long, List<Match>> matchesByEntryId) {
        Set<Match> played = matchesByEntryId.values().stream()
                .flatMap(List::stream)
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                .collect(Collectors.toSet());
        int wins = (int) played.stream()
                .filter(m -> m.getWinnerEntry() != null
                        && entryIds.contains(m.getWinnerEntry().getId()))
                .count();
        int losses = played.size() - wins;
        Double winRate = played.isEmpty() ? null : (double) wins / played.size();
        return new PlayerRecordDto(played.size(), wins, losses, winRate);
    }

    /**
     * Meilleur resultat par categorie (du tournoi principal), toutes saisons
     * confondues : tableau principal avant qualifs, puis le moins de tours
     * restants avant la finale (un QF vaut un QF quelle que soit la taille du
     * tableau), titre en tete. A egalite, le plus recent est retenu
     * (tournaments est deja trie du plus recent au plus ancien).
     */
    private List<CategoryBestResultDto> bestByCategory(
            List<PlayerTournamentResultDto> tournaments, Map<Long, List<Run>> runsByMainId) {
        Map<TournamentCategory, PlayerTournamentResultDto> best = new LinkedHashMap<>();
        Map<TournamentCategory, Integer> bestDepth = new HashMap<>();
        Map<TournamentCategory, Integer> times = new HashMap<>();
        for (PlayerTournamentResultDto r : tournaments) {
            if (r.category() == null) {
                continue;
            }
            int depth = depth(runsByMainId.get(r.tournamentId()));
            Integer current = bestDepth.get(r.category());
            if (current == null || depth > current) {
                best.put(r.category(), r);
                bestDepth.put(r.category(), depth);
                times.put(r.category(), 1);
            } else if (depth == current) {
                times.merge(r.category(), 1, Integer::sum);
            }
        }
        List<CategoryBestResultDto> result = new ArrayList<>();
        for (TournamentCategory category : TournamentCategory.values()) {
            if (best.containsKey(category)) {
                result.add(new CategoryBestResultDto(category, best.get(category), times.get(category)));
            }
        }
        return result;
    }

    /** Profondeur comparable d'un parcours : plus grand = meilleur. */
    private static int depth(List<Run> runs) {
        Run mainRun = runs.stream()
                .filter(r -> !r.tournament().isQualifying())
                .findFirst()
                .orElse(null);
        if (mainRun != null) {
            Tournament t = mainRun.tournament();
            int totalRounds = t.getDrawSize() == null
                    ? mainRun.reachedRound()
                    : RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(t.getDrawSize()));
            int remaining = totalRounds - mainRun.reachedRound();
            return 1000 - 2 * remaining + (mainRun.won() ? 1 : 0);
        }
        return runs.stream().mapToInt(Run::reachedRound).max().orElse(0);
    }
}
