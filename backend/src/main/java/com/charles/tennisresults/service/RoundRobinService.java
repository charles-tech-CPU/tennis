package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.EntryDto;
import com.charles.tennisresults.dto.RoundRobinDto;
import com.charles.tennisresults.dto.RoundRobinGroupDto;
import com.charles.tennisresults.dto.RoundRobinStandingDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tournoi en poules (format des ATP Finals, nittoatpfinals.com "Rules &
 * Format") : 2 poules de 4, chacun joue les 3 autres joueurs de sa poule ; le
 * 1er de la poule A affronte le 2e de la poule B en demi-finale et
 * inversement, puis finale.
 *
 * Organisation en base (voir V27) :
 *  - entrees : positions 1-4 = poule A, 5-8 = poule B, 9+ = remplacants ;
 *  - tour 1 = matchs de poule (positions 1-6 poule A, 7-12 poule B, dans
 *    l'ordre de {@link #PAIRS}), tour 2 = demi-finales, tour 3 = finale.
 *
 * Forfait : {@link #replace} met le remplacant a la place du joueur dans ses
 * matchs de poule pas encore joues ; les matchs deja joues par le joueur
 * forfait restent les siens (il reste dans le classement de la poule).
 */
@Service
public class RoundRobinService {

    public static final int GROUP_SIZE = 4;
    public static final int GROUP_COUNT = 2;
    public static final int GROUP_ROUND = 1;
    public static final int SEMI_FINAL_ROUND = 2;

    /** Les 6 affiches d'une poule (numero du joueur dans sa poule, 1 a 4). */
    static final int[][] PAIRS = {{1, 4}, {2, 3}, {1, 3}, {2, 4}, {1, 2}, {3, 4}};

    private static final Pattern SET_PATTERN = Pattern.compile("(\\d+)-(\\d+)(?:\\(\\d+\\))?");

    private final MatchRepository matchRepository;
    private final EntryRepository entryRepository;
    private final TournamentRepository tournamentRepository;
    private final BracketService bracketService;

    public RoundRobinService(
            MatchRepository matchRepository,
            EntryRepository entryRepository,
            TournamentRepository tournamentRepository,
            BracketService bracketService) {
        this.matchRepository = matchRepository;
        this.entryRepository = entryRepository;
        this.tournamentRepository = tournamentRepository;
        this.bracketService = bracketService;
    }

    /** Poule (1 = A, 2 = B) d'un match de poule d'apres sa position dans le tour. */
    static int groupOfMatch(int positionInRound) {
        return (positionInRound - 1) / PAIRS.length + 1;
    }

    /** Poule (1 = A, 2 = B) d'une position du tableau, 0 pour un remplacant. */
    static int groupOfPosition(int drawPosition) {
        return drawPosition >= 1 && drawPosition <= GROUP_SIZE * GROUP_COUNT
                ? (drawPosition - 1) / GROUP_SIZE + 1
                : 0;
    }

    /** Position du tableau du joueur `slot` (0 = entry1, 1 = entry2) du match de poule `positionInRound`. */
    private static int expectedDrawPosition(int positionInRound, int slot) {
        int group = groupOfMatch(positionInRound);
        int pairIndex = (positionInRound - 1) % PAIRS.length;
        return (group - 1) * GROUP_SIZE + PAIRS[pairIndex][slot];
    }

    /**
     * Remplit les cases vides des matchs de poule a partir des entrees (a
     * appeler apres tout ajout/retrait d'entree). Ne touche jamais une case
     * deja remplie : un remplacant mis en place par {@link #replace} reste.
     */
    @Transactional
    public void syncGroupMatches(Long tournamentId) {
        Tournament tournament = getOrThrow(tournamentId);
        Map<Integer, Entry> byPosition = new LinkedHashMap<>();
        entryRepository.findByTournamentIdOrderByDrawPositionAsc(tournamentId)
                .forEach(e -> byPosition.put(e.getDrawPosition(), e));

        for (Match m : groupMatches(tournamentId)) {
            if (m.getStatus() == MatchStatus.COMPLETED) {
                continue;
            }
            if (m.getEntry1() == null) {
                m.setEntry1(byPosition.get(expectedDrawPosition(m.getPositionInRound(), 0)));
            }
            if (m.getEntry2() == null) {
                m.setEntry2(byPosition.get(expectedDrawPosition(m.getPositionInRound(), 1)));
            }
            m.setStatus(m.getEntry1() != null && m.getEntry2() != null ? MatchStatus.SCHEDULED : MatchStatus.PENDING);
            matchRepository.save(m);
        }
        refreshSemiFinals(tournament);
    }

    /** Forfait : le remplacant reprend les matchs de poule pas encore joues du joueur. */
    @Transactional
    public void replace(Long tournamentId, Long withdrawnEntryId, Long alternateEntryId) {
        Tournament tournament = getOrThrow(tournamentId);
        if (!tournament.isRoundRobin()) {
            throw new IllegalArgumentException("Ce tournoi n'a pas de phase de poules.");
        }
        Entry withdrawn = entryOf(tournamentId, withdrawnEntryId);
        Entry alternate = entryOf(tournamentId, alternateEntryId);
        if (groupOfPosition(alternate.getDrawPosition()) != 0) {
            throw new IllegalArgumentException("Le remplacant doit etre inscrit en position "
                    + (GROUP_SIZE * GROUP_COUNT + 1) + " ou plus (pas dans une poule).");
        }
        if (withdrawn.getId().equals(alternate.getId())) {
            throw new IllegalArgumentException("Un joueur ne peut pas se remplacer lui-meme.");
        }

        List<Match> toReplace = groupMatches(tournamentId).stream()
                .filter(m -> m.getStatus() != MatchStatus.COMPLETED)
                .filter(m -> isEntry(m.getEntry1(), withdrawn) || isEntry(m.getEntry2(), withdrawn))
                .toList();
        if (toReplace.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun match de poule restant a jouer pour ce joueur : rien a remplacer.");
        }
        int group = groupOfMatch(toReplace.get(0).getPositionInRound());
        boolean alternateInOtherGroup = groupMatches(tournamentId).stream()
                .filter(m -> groupOfMatch(m.getPositionInRound()) != group)
                .anyMatch(m -> isEntry(m.getEntry1(), alternate) || isEntry(m.getEntry2(), alternate));
        if (alternateInOtherGroup) {
            throw new IllegalArgumentException("Ce remplacant joue deja dans l'autre poule.");
        }
        for (Match m : toReplace) {
            if (isEntry(m.getEntry1(), withdrawn)) {
                m.setEntry1(alternate);
            } else {
                m.setEntry2(alternate);
            }
            matchRepository.save(m);
        }
    }

    /**
     * A appeler apres chaque resultat de poule : place les 2 premiers de
     * chaque poule terminee en demi-finale (1er A - 2e B, 1er B - 2e A), et
     * annule une demi-finale (et la finale) deja jouee si ses joueurs changent
     * suite a une correction de score.
     */
    @Transactional
    public void refreshSemiFinals(Tournament tournament) {
        List<Match> groupMatches = groupMatches(tournament.getId());
        Entry[][] qualifiers = new Entry[GROUP_COUNT][2];
        for (int g = 1; g <= GROUP_COUNT; g++) {
            GroupResult result = groupResult(tournament, g, groupMatches);
            if (result.complete()) {
                qualifiers[g - 1][0] = result.ranked().get(0).entry;
                qualifiers[g - 1][1] = result.ranked().size() > 1 ? result.ranked().get(1).entry : null;
            }
        }
        // SF1 = 1er A - 2e B, SF2 = 1er B - 2e A
        setSemiFinal(tournament, 1, qualifiers[0][0], qualifiers[1][1]);
        setSemiFinal(tournament, 2, qualifiers[1][0], qualifiers[0][1]);
    }

    private void setSemiFinal(Tournament tournament, int position, Entry entry1, Entry entry2) {
        Match sf = matchRepository
                .findByTournamentIdAndRoundOrderAndPositionInRound(tournament.getId(), SEMI_FINAL_ROUND, position)
                .orElse(null);
        if (sf == null) {
            return;
        }
        boolean unchanged = sameEntry(sf.getEntry1(), entry1) && sameEntry(sf.getEntry2(), entry2);
        if (unchanged) {
            return;
        }
        if (sf.getStatus() == MatchStatus.COMPLETED || sf.getStatus() == MatchStatus.BYE) {
            bracketService.resetDownstream(sf); // la finale reposait sur l'ancien resultat
            sf.setScore(null);
            sf.setWinnerEntry(null);
        }
        sf.setEntry1(entry1);
        sf.setEntry2(entry2);
        sf.setStatus(entry1 != null && entry2 != null ? MatchStatus.SCHEDULED : MatchStatus.PENDING);
        matchRepository.save(sf);
    }

    @Transactional(readOnly = true)
    public RoundRobinDto overview(Long tournamentId) {
        Tournament tournament = getOrThrow(tournamentId);
        if (!tournament.isRoundRobin()) {
            throw new IllegalArgumentException("Ce tournoi n'a pas de phase de poules.");
        }
        List<Match> groupMatches = groupMatches(tournamentId);
        List<RoundRobinGroupDto> groups = new ArrayList<>();
        for (int g = 1; g <= GROUP_COUNT; g++) {
            GroupResult result = groupResult(tournament, g, groupMatches);
            List<RoundRobinStandingDto> rows = new ArrayList<>();
            for (int i = 0; i < result.ranked().size(); i++) {
                Stat s = result.ranked().get(i);
                rows.add(new RoundRobinStandingDto(
                        EntryDto.from(s.entry),
                        i + 1,
                        s.wins,
                        s.losses,
                        s.setsWon,
                        s.setsLost,
                        s.gamesWon,
                        s.gamesLost,
                        result.complete() && i < 2,
                        result.withdrawn().contains(s.entry.getId())));
            }
            groups.add(new RoundRobinGroupDto(g, groupName(tournament, g), result.complete(), rows));
        }
        List<EntryDto> alternates = entryRepository.findByTournamentIdOrderByDrawPositionAsc(tournamentId).stream()
                .filter(e -> groupOfPosition(e.getDrawPosition()) == 0 && !e.isBye())
                .map(EntryDto::from)
                .toList();
        return new RoundRobinDto(groups, alternates);
    }

    private static String groupName(Tournament t, int group) {
        String name = group == 1 ? t.getGroupAName() : t.getGroupBName();
        return name != null && !name.isBlank() ? name : "Groupe " + (char) ('A' + group - 1);
    }

    // ---------------------------------------------------------------------
    // Classement d'une poule
    // ---------------------------------------------------------------------

    private static final class Stat {
        final Entry entry;
        int wins;
        int losses;
        int setsWon;
        int setsLost;
        int gamesWon;
        int gamesLost;

        Stat(Entry entry) {
            this.entry = entry;
        }

        int played() {
            return wins + losses;
        }
    }

    private record GroupResult(boolean complete, List<Stat> ranked, List<Long> withdrawn) {}

    private GroupResult groupResult(Tournament tournament, int group, List<Match> allGroupMatches) {
        List<Match> matches = allGroupMatches.stream()
                .filter(m -> groupOfMatch(m.getPositionInRound()) == group)
                .toList();

        // Joueurs de la poule : ceux de ses 4 positions, plus les remplacants entres en jeu.
        Map<Long, Stat> stats = new LinkedHashMap<>();
        entryRepository.findByTournamentIdOrderByDrawPositionAsc(tournament.getId()).stream()
                .filter(e -> groupOfPosition(e.getDrawPosition()) == group && !e.isBye())
                .forEach(e -> stats.put(e.getId(), new Stat(e)));
        List<Long> withdrawn = new ArrayList<>();
        for (Match m : matches) {
            for (int slot = 0; slot < 2; slot++) {
                Entry e = slot == 0 ? m.getEntry1() : m.getEntry2();
                if (e != null) {
                    stats.computeIfAbsent(e.getId(), k -> new Stat(e));
                    if (groupOfPosition(e.getDrawPosition()) == 0) {
                        // un remplacant occupe cette case : le joueur prevu a declare forfait
                        int expected = expectedDrawPosition(m.getPositionInRound(), slot);
                        stats.values().stream()
                                .filter(s -> s.entry.getDrawPosition() == expected)
                                .map(s -> s.entry.getId())
                                .filter(id -> !withdrawn.contains(id))
                                .forEach(withdrawn::add);
                    }
                }
            }
        }

        boolean complete = !matches.isEmpty()
                && matches.stream().allMatch(m -> m.getStatus() == MatchStatus.COMPLETED);
        List<Match> completed = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED && m.getWinnerEntry() != null)
                .toList();
        for (Match m : completed) {
            Entry winner = m.getWinnerEntry();
            Entry loser = isEntry(m.getEntry1(), winner) ? m.getEntry2() : m.getEntry1();
            Stat w = stats.get(winner.getId());
            Stat l = loser != null ? stats.get(loser.getId()) : null;
            if (w != null) {
                w.wins++;
            }
            if (l != null) {
                l.losses++;
            }
            for (int[] set : parseSets(m.getScore())) {
                // score ecrit du point de vue du vainqueur du match
                if (w != null) {
                    w.gamesWon += set[0];
                    w.gamesLost += set[1];
                    w.setsWon += set[0] > set[1] ? 1 : 0;
                    w.setsLost += set[0] < set[1] ? 1 : 0;
                }
                if (l != null) {
                    l.gamesWon += set[1];
                    l.gamesLost += set[0];
                    l.setsWon += set[1] > set[0] ? 1 : 0;
                    l.setsLost += set[1] < set[0] ? 1 : 0;
                }
            }
        }
        return new GroupResult(complete, rank(new ArrayList<>(stats.values()), completed), withdrawn);
    }

    /**
     * Classement officiel : a) victoires, b) matchs joues ; puis entre
     * joueurs a egalite : 2 joueurs -> confrontation directe ; 3 joueurs ou
     * plus -> % de sets gagnes, puis % de jeux gagnes, puis classement ATP ;
     * des qu'un critere isole un joueur et que 2 restent a egalite, ces 2 se
     * departagent a la confrontation directe.
     */
    private static List<Stat> rank(List<Stat> stats, List<Match> completed) {
        stats.sort(Comparator.comparingInt((Stat s) -> -s.wins)
                .thenComparingInt(s -> -s.played())
                .thenComparingInt(s -> s.entry.getDrawPosition()));
        List<Stat> ranked = new ArrayList<>();
        for (List<Stat> block : blocks(stats, Comparator.comparingInt((Stat s) -> -s.wins)
                .thenComparingInt(s -> -s.played()))) {
            ranked.addAll(resolveTie(block, completed));
        }
        return ranked;
    }

    private static final List<Comparator<Stat>> TIE_BREAKS = List.of(
            (a, b) -> compareRatio(b.setsWon, b.setsWon + b.setsLost, a.setsWon, a.setsWon + a.setsLost),
            (a, b) -> compareRatio(b.gamesWon, b.gamesWon + b.gamesLost, a.gamesWon, a.gamesWon + a.gamesLost),
            Comparator.comparingInt(s -> s.entry.getRankingAtEntry() != null
                    ? s.entry.getRankingAtEntry()
                    : Integer.MAX_VALUE));

    private static List<Stat> resolveTie(List<Stat> block, List<Match> completed) {
        if (block.size() <= 1) {
            return block;
        }
        if (block.size() == 2) {
            Entry h2hWinner = headToHeadWinner(block.get(0).entry, block.get(1).entry, completed);
            if (h2hWinner != null) {
                return isEntry(block.get(0).entry, h2hWinner) ? block : List.of(block.get(1), block.get(0));
            }
        }
        for (Comparator<Stat> criterion : TIE_BREAKS) {
            List<Stat> sorted = new ArrayList<>(block);
            sorted.sort(criterion);
            List<List<Stat>> sub = blocks(sorted, criterion);
            if (sub.size() > 1) {
                List<Stat> out = new ArrayList<>();
                sub.forEach(b -> out.addAll(resolveTie(b, completed)));
                return out;
            }
        }
        return block; // egalite parfaite : ordre du tableau
    }

    /** Decoupe une liste deja triee en paquets d'elements egaux pour ce critere. */
    private static List<List<Stat>> blocks(List<Stat> sorted, Comparator<Stat> criterion) {
        List<List<Stat>> blocks = new ArrayList<>();
        for (Stat s : sorted) {
            if (blocks.isEmpty() || criterion.compare(blocks.get(blocks.size() - 1).get(0), s) != 0) {
                blocks.add(new ArrayList<>());
            }
            blocks.get(blocks.size() - 1).add(s);
        }
        return blocks;
    }

    /** Compare a/b et c/d (fractions exactes) ; une fraction sans denominateur vaut 0. */
    private static int compareRatio(int a, int b, int c, int d) {
        long left = b == 0 ? 0 : (long) a * (d == 0 ? 1 : d);
        long right = d == 0 ? 0 : (long) c * (b == 0 ? 1 : b);
        return Long.compare(left, right);
    }

    private static Entry headToHeadWinner(Entry x, Entry y, List<Match> completed) {
        return completed.stream()
                .filter(m -> (isEntry(m.getEntry1(), x) && isEntry(m.getEntry2(), y))
                        || (isEntry(m.getEntry1(), y) && isEntry(m.getEntry2(), x)))
                .map(Match::getWinnerEntry)
                .findFirst()
                .orElse(null);
    }

    static List<int[]> parseSets(String score) {
        List<int[]> sets = new ArrayList<>();
        if (score == null) {
            return sets;
        }
        for (String token : score.trim().split("\\s+")) {
            Matcher matcher = SET_PATTERN.matcher(token);
            if (matcher.matches()) {
                sets.add(new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))});
            }
        }
        return sets;
    }

    // ---------------------------------------------------------------------

    private List<Match> groupMatches(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(tournamentId).stream()
                .filter(m -> m.getRoundOrder() == GROUP_ROUND)
                .toList();
    }

    private Entry entryOf(Long tournamentId, Long entryId) {
        Entry e = entryRepository
                .findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entree introuvable: " + entryId));
        if (!e.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Cette entree n'appartient pas a ce tournoi.");
        }
        return e;
    }

    private Tournament getOrThrow(Long id) {
        return tournamentRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournoi introuvable: " + id));
    }

    private static boolean isEntry(Entry candidate, Entry entry) {
        return candidate != null && entry != null && Objects.equals(candidate.getId(), entry.getId());
    }

    private static boolean sameEntry(Entry a, Entry b) {
        return a == null ? b == null : isEntry(a, b);
    }
}
