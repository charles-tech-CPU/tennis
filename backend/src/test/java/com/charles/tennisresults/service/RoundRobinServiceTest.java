package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.RoundRobinDto;
import com.charles.tennisresults.dto.RoundRobinStandingDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Poules des ATP Finals : remplissage, classement officiel, demi-finales et forfaits. */
@ExtendWith(MockitoExtension.class)
class RoundRobinServiceTest {

    private static final long TID = 243L;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private BracketService bracketService;

    @InjectMocks
    private RoundRobinService service;

    private Tournament tournament;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Match> matches = new ArrayList<>();

    @BeforeEach
    void setUp() {
        tournament = new Tournament();
        tournament.setId(TID);
        tournament.setRoundRobin(true);
        tournament.setDrawSize(8);
        tournament.setGroupAName("John McEnroe");
        for (int pos = 1; pos <= 12; pos++) {
            matches.add(match(1, pos));
        }
        matches.add(match(2, 1));
        matches.add(match(2, 2));
        matches.add(match(3, 1));
        for (int pos = 1; pos <= 9; pos++) {
            Entry e = new Entry();
            e.setId(100L + pos);
            e.setTournament(tournament);
            e.setDrawPosition(pos);
            e.setRankingAtEntry(pos);
            Player p = new Player();
            p.setId(1000L + pos);
            p.setLastName("J" + pos);
            e.setPlayer(p);
            entries.add(e);
        }

        lenient().when(tournamentRepository.findById(TID)).thenReturn(Optional.of(tournament));
        lenient()
                .when(entryRepository.findByTournamentIdOrderByDrawPositionAsc(TID))
                .thenAnswer(i -> entries.stream()
                        .sorted(Comparator.comparing(Entry::getDrawPosition))
                        .toList());
        lenient()
                .when(entryRepository.findById(anyLong()))
                .thenAnswer(i -> entries.stream()
                        .filter(e -> e.getId().equals(i.getArgument(0)))
                        .findFirst());
        lenient()
                .when(matchRepository.findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(TID))
                .thenAnswer(i -> new ArrayList<>(matches));
        lenient()
                .when(matchRepository.findByTournamentIdAndRoundOrderAndPositionInRound(any(), anyInt(), anyInt()))
                .thenAnswer(i -> matches.stream()
                        .filter(m -> m.getRoundOrder().equals(i.getArgument(1))
                                && m.getPositionInRound().equals(i.getArgument(2)))
                        .findFirst());
    }

    @Test
    void lesMatchsDePouleSontRemplisDepuisLesPositions() {
        service.syncGroupMatches(TID);

        // poule A : 1-4, 2-3, 1-3, 2-4, 1-2, 3-4 ; poule B pareil avec 5 a 8
        assertThat(players(groupMatch(1))).containsExactly(1, 4);
        assertThat(players(groupMatch(6))).containsExactly(3, 4);
        assertThat(players(groupMatch(7))).containsExactly(5, 8);
        assertThat(groupMatch(1).getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(semi(1).getEntry1()).isNull();
    }

    @Test
    void lesDeuxPremiersDeChaquePouleSontCroisesEnDemiFinale() {
        service.syncGroupMatches(TID);
        // poule A : 1 > 2 > 3 > 4 (3-0, 2-1, 1-2, 0-3)
        playGroup(0, new int[] {1, 2, 1, 2, 1, 3});
        // poule B : 6 > 5 > 7 > 8
        playGroup(6, new int[] {5, 6, 5, 6, 6, 7});

        service.refreshSemiFinals(tournament);

        assertThat(players(semi(1))).containsExactly(1, 5); // 1er A - 2e B
        assertThat(players(semi(2))).containsExactly(6, 2); // 1er B - 2e A
        RoundRobinDto dto = service.overview(TID);
        assertThat(dto.groups().get(0).name()).isEqualTo("John McEnroe");
        assertThat(dto.groups().get(1).name()).isEqualTo("Groupe B");
        assertThat(dto.groups().get(0).standings())
                .extracting(s -> s.entry().drawPosition())
                .containsExactly(1, 2, 3, 4);
        assertThat(dto.groups().get(0).standings().get(1).qualified()).isTrue();
        assertThat(dto.groups().get(0).standings().get(2).qualified()).isFalse();
        assertThat(dto.alternates()).extracting(e -> e.drawPosition()).containsExactly(9);
    }

    @Test
    void troisJoueursAEgaliteSeDepartagentAuPourcentageDeSets() {
        service.syncGroupMatches(TID);
        // 1-4 : 1 ; 2-3 : 2 ; 1-3 : 3 ; 2-4 : 2 ; 1-2 : 1 ; 3-4 : 3 -> 1, 2, 3 a 2 victoires
        int[] winners = {1, 2, 3, 2, 1, 3};
        String[] scores = {"6-0 6-0", "6-4 6-4", "6-4 6-4", "6-4 6-4", "6-4 4-6 6-4", "6-4 4-6 6-4"};
        for (int i = 0; i < 6; i++) {
            play(groupMatch(i + 1), winners[i], scores[i]);
        }
        // sets : J1 2-0, 0-2, 2-1 -> 4-3 ; J2 2-0, 2-0, 1-2 -> 5-2 ; J3 0-2, 2-0, 2-1 -> 4-3
        // J2 superieur (71 %), J1 et J3 a egalite (57 %) -> confrontation directe : J3 a battu J1
        service.refreshSemiFinals(tournament);

        List<RoundRobinStandingDto> a = service.overview(TID).groups().get(0).standings();
        assertThat(a).extracting(s -> s.entry().drawPosition()).containsExactly(2, 3, 1, 4);
    }

    @Test
    void unRemplacantReprendLesMatchsRestantsDuJoueurForfait() {
        service.syncGroupMatches(TID);
        play(groupMatch(1), 1, "6-3 6-3"); // J1 bat J4

        service.replace(TID, 101L, 109L); // J1 forfait, J9 remplacant

        assertThat(players(groupMatch(1))).containsExactly(1, 4); // match deja joue : inchange
        assertThat(players(groupMatch(3))).containsExactly(9, 3);
        assertThat(players(groupMatch(5))).containsExactly(9, 2);
        RoundRobinDto dto = service.overview(TID);
        assertThat(dto.groups().get(0).standings()).hasSize(5);
        assertThat(dto.groups().get(0).standings())
                .filteredOn(RoundRobinStandingDto::withdrawn)
                .extracting(s -> s.entry().drawPosition())
                .containsExactly(1);
    }

    @Test
    void leRemplacantDoitEtreHorsPoule() {
        service.syncGroupMatches(TID);

        assertThatThrownBy(() -> service.replace(TID, 101L, 105L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uneCorrectionDeScoreQuiChangeLesQualifiesAnnuleLaDemiDejaJouee() {
        service.syncGroupMatches(TID);
        playGroup(0, new int[] {1, 2, 1, 2, 1, 3});
        playGroup(6, new int[] {5, 6, 5, 6, 6, 7});
        service.refreshSemiFinals(tournament);
        play(semi(1), 1, "6-4 6-4");

        play(groupMatch(5), 2, "6-4 6-4"); // correction : J2 bat J1 -> J2 1er, J1 2e
        service.refreshSemiFinals(tournament);

        assertThat(players(semi(1))).containsExactly(2, 5);
        assertThat(semi(1).getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(semi(1).getWinnerEntry()).isNull();
    }

    // ---------------------------------------------------------------------

    private void playGroup(int offset, int[] winnersByPairIndex) {
        for (int i = 0; i < 6; i++) {
            play(groupMatch(offset + i + 1), winnersByPairIndex[i], "6-4 6-4");
        }
    }

    private void play(Match m, int winnerPosition, String score) {
        Entry winner = m.getEntry1().getDrawPosition() == winnerPosition ? m.getEntry1() : m.getEntry2();
        assertThat(winner.getDrawPosition()).isEqualTo(winnerPosition);
        m.setWinnerEntry(winner);
        m.setScore(score);
        m.setStatus(MatchStatus.COMPLETED);
    }

    private Match groupMatch(int position) {
        return matches.get(position - 1);
    }

    private Match semi(int position) {
        return matches.get(11 + position);
    }

    private static List<Integer> players(Match m) {
        return List.of(m.getEntry1().getDrawPosition(), m.getEntry2().getDrawPosition());
    }

    private Match match(int round, int position) {
        Match m = new Match();
        m.setId((long) (round * 100 + position));
        m.setTournament(tournament);
        m.setRoundOrder(round);
        m.setPositionInRound(position);
        m.setStatus(MatchStatus.PENDING);
        return m;
    }
}
