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
import com.charles.tennisresults.dto.PlayerMatchDto;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private record Run(
            Entry entry, int reachedRound, boolean won, boolean inProgress, int points, List<Match> matches) {

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
        }
        Map<Long, Tournament> mainsById = mainTournamentsOfQualifying(entries);

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

        List<PlayerTournamentResultDto> tournaments = runsByMainId.values().stream()
                .map(runs -> result(runs, mainsById, labels))
                .sorted(MOST_RECENT_FIRST)
                .toList();

        List<PlayerTournamentResultDto> titles =
                tournaments.stream().filter(PlayerTournamentResultDto::champion).toList();

        return withRanking(
                player,
                matchRecord(entryIds, matchesByEntryId),
                titles,
                bestByCategory(tournaments, runsByMainId),
                tournaments);
    }

    /** Tournois principaux des tableaux de qualifs disputes (aucune requete s'il n'y en a pas). */
    private Map<Long, Tournament> mainTournamentsOfQualifying(List<Entry> entries) {
        Set<Long> mainIds = entries.stream()
                .map(Entry::getTournament)
                .filter(Tournament::isQualifying)
                .map(Tournament::getMainTournamentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (mainIds.isEmpty()) {
            return Map.of();
        }
        return tournamentRepository.findAllById(mainIds).stream().collect(Collectors.toMap(Tournament::getId, t -> t));
    }

    private PlayerProfileDto withRanking(
            Player player,
            PlayerRecordDto matchRecord,
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
                PlayerDto.from(player),
                position,
                total,
                ranking.size(),
                matchRecord,
                titles,
                bestByCategory,
                tournaments);
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
            return new Run(entry, 1, false, true, points, matches);
        }
        boolean wonDeepest = deepest.getWinnerEntry() != null
                && deepest.getWinnerEntry().getId().equals(entry.getId());
        int r = deepest.getRoundOrder();
        if (!wonDeepest) {
            return new Run(entry, r, false, false, points, matches);
        }
        if (totalRounds > 0 && r >= totalRounds) {
            return new Run(entry, r, true, false, points, matches); // champion (ou qualifie)
        }
        return new Run(entry, r + 1, false, true, points, matches);
    }

    /**
     * Resultat fusionne d'un tournoi : le tableau principal prime sur les
     * qualifs. runs n'est jamais vide (au moins l'entree qui a cree le groupe).
     */
    private PlayerTournamentResultDto result(
            List<Run> runs, Map<Long, Tournament> mainsById, Map<Long, Map<Integer, String>> labels) {
        Optional<Run> mainRun =
                runs.stream().filter(r -> !r.tournament().isQualifying()).findFirst();
        boolean viaQualifying = runs.stream().anyMatch(r -> r.tournament().isQualifying());
        Run shown = mainRun.orElse(runs.get(0));
        Tournament t = shown.tournament();
        Tournament main = t.isQualifying() ? mainsById.getOrDefault(t.getMainTournamentId(), t) : t;

        String label = labels.getOrDefault(t.getId(), Map.of())
                .getOrDefault(shown.reachedRound(), RoundLabels.labelFor(t, shown.reachedRound()));
        return new PlayerTournamentResultDto(
                main.getId(),
                main.getName(),
                main.getCountry(),
                main.getSurface(),
                main.getIndoor(),
                main.getSeason(),
                main.getWeekNumber(),
                main.getCategory(),
                label,
                mainRun.map(Run::won).orElse(false),
                shown.inProgress(),
                viaQualifying,
                rankingAtEntry(runs),
                runs.stream().mapToInt(Run::points).sum(),
                matchesOf(runs, labels));
    }

    /**
     * Classement fige a l'entree dans le tournoi : celui des qualifs si le
     * joueur en est sorti (premier tableau dispute), sinon du tableau principal.
     */
    private static Integer rankingAtEntry(List<Run> runs) {
        return runs.stream()
                .sorted(Comparator.comparing(r -> !r.tournament().isQualifying()))
                .map(r -> r.entry().getRankingAtEntry())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Matchs decides du tournoi, du plus recent au plus ancien : tableau
     * principal (joue apres) puis qualifs, tour le plus avance d'abord.
     */
    private static List<PlayerMatchDto> matchesOf(List<Run> runs, Map<Long, Map<Integer, String>> labels) {
        return runs.stream()
                .sorted(Comparator.comparing(r -> r.tournament().isQualifying()))
                .flatMap(r -> r.matches().stream()
                        .sorted(Comparator.comparing(Match::getRoundOrder, Comparator.reverseOrder()))
                        .map(m -> matchDto(r.entry(), m, labels)))
                .toList();
    }

    private static PlayerMatchDto matchDto(Entry self, Match m, Map<Long, Map<Integer, String>> labels) {
        Tournament t = self.getTournament();
        Entry opponent =
                self.getId().equals(m.getEntry1() == null ? null : m.getEntry1().getId())
                        ? m.getEntry2()
                        : m.getEntry1();
        Player opponentPlayer = opponent == null ? null : opponent.getPlayer();
        boolean bye = m.getStatus() == MatchStatus.BYE || opponentPlayer == null;
        String label = labels.getOrDefault(t.getId(), Map.of())
                .getOrDefault(m.getRoundOrder(), RoundLabels.labelFor(t, m.getRoundOrder()));
        return new PlayerMatchDto(
                m.getId(),
                t.isQualifying(),
                m.getRoundOrder(),
                label,
                bye,
                bye ? null : PlayerDto.from(opponentPlayer),
                bye ? null : opponent.getSeed(),
                bye ? null : opponent.getEntryType(),
                bye ? null : opponent.getRankingAtEntry(),
                bye ? null : m.getScore(),
                m.getWinnerEntry() != null && m.getWinnerEntry().getId().equals(self.getId()));
    }

    /**
     * Matchs reellement joues (COMPLETED) : un bye (BYE) n'est pas un match
     * dispute et gonflerait artificiellement le nombre de victoires.
     */
    private PlayerRecordDto matchRecord(Set<Long> entryIds, Map<Long, List<Match>> matchesByEntryId) {
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
        // EnumMap : parcours dans l'ordre des categories (Grand Chelem d'abord).
        Map<TournamentCategory, PlayerTournamentResultDto> best = new EnumMap<>(TournamentCategory.class);
        Map<TournamentCategory, Integer> bestDepth = new EnumMap<>(TournamentCategory.class);
        Map<TournamentCategory, Integer> times = new EnumMap<>(TournamentCategory.class);
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
        return best.entrySet().stream()
                .map(e -> new CategoryBestResultDto(e.getKey(), e.getValue(), times.get(e.getKey())))
                .toList();
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
