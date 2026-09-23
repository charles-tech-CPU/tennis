package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    private static final Long TOURNAMENT_ID = 1L;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private BracketService bracketService;

    private Tournament tournament;

    /** Matchs "en base" : findByTournamentIdAndRoundOrderAndPositionInRound les cherche ici. */
    private final List<Match> matches = new ArrayList<>();

    @BeforeEach
    void setUp() {
        tournament = new Tournament();
        tournament.setId(TOURNAMENT_ID);
        tournament.setDrawSize(4);
        lenient()
                .when(matchRepository.findByTournamentIdAndRoundOrderAndPositionInRound(anyLong(), anyInt(), anyInt()))
                .thenAnswer(inv -> matches.stream()
                        .filter(m -> m.getRoundOrder().equals(inv.getArgument(1))
                                && m.getPositionInRound().equals(inv.getArgument(2)))
                        .findFirst());
    }

    @Test
    void leSqueletteDUnTableauDe8CreeTousLesMatchsJusquALaFinale() {
        bracketService.initializeSkeleton(tournament, 8);

        verify(matchRepository, times(4 + 2 + 1)).save(any(Match.class));
    }

    @Test
    void leSqueletteDesQualifsSArreteAvantLaFinale() {
        // 32 cases, 3 tours : 16 + 8 + 4 matchs, soit 4 qualifies
        bracketService.initializeSkeleton(tournament, 32, 3);

        verify(matchRepository, times(16 + 8 + 4)).save(any(Match.class));
    }

    @Test
    void leVainqueurDUnMatchImpairPrendLaPremierePlaceDuTourSuivant() {
        Match semi = match(1, 1, MatchStatus.COMPLETED);
        Match finale = match(2, 1, MatchStatus.PENDING);
        Entry winner = entry(10L, 1);

        bracketService.advanceWinner(semi, winner);

        assertThat(finale.getEntry1()).isSameAs(winner);
        assertThat(finale.getEntry2()).isNull();
        assertThat(finale.getStatus()).isEqualTo(MatchStatus.PENDING);
    }

    @Test
    void leTourSuivantEstProgrammeQuandLesDeuxJoueursSontConnus() {
        match(1, 2, MatchStatus.COMPLETED);
        Match finale = match(2, 1, MatchStatus.PENDING);
        finale.setEntry1(entry(10L, 1));
        Entry winner = entry(30L, 3);

        bracketService.advanceWinner(matches.get(0), winner);

        assertThat(finale.getEntry2()).isSameAs(winner);
        assertThat(finale.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        verify(matchRepository).save(finale);
    }

    @Test
    void gagnerLaFinaleNeQualifiePourAucunAutreMatch() {
        Match finale = match(2, 1, MatchStatus.COMPLETED);

        bracketService.advanceWinner(finale, entry(10L, 1));

        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void corrigerUnResultatAnnuleEnCascadeLesToursDejaJoues() {
        Match quarter = match(1, 1, MatchStatus.COMPLETED);
        Match semi = match(2, 1, MatchStatus.COMPLETED);
        Match finale = match(3, 1, MatchStatus.SCHEDULED);
        Entry oldWinner = entry(10L, 1);
        semi.setEntry1(oldWinner);
        semi.setWinnerEntry(oldWinner);
        semi.setScore("6-4 6-4");
        finale.setEntry1(oldWinner);

        bracketService.resetDownstream(quarter);

        assertThat(semi.getEntry1()).isNull();
        assertThat(semi.getWinnerEntry()).isNull();
        assertThat(semi.getScore()).isNull();
        assertThat(semi.getStatus()).isEqualTo(MatchStatus.PENDING);
        assertThat(finale.getEntry1()).isNull();
        assertThat(finale.getStatus()).isEqualTo(MatchStatus.PENDING);
    }

    @Test
    void unByeAuPremierTourQualifieDirectementLAdversaire() {
        Entry seed = entry(10L, 1);
        Entry bye = entry(20L, 2);
        bye.setBye(true);
        Entry player3 = entry(30L, 3);
        Entry player4 = entry(40L, 4);
        Match first = match(1, 1, MatchStatus.PENDING);
        Match second = match(1, 2, MatchStatus.PENDING);
        Match finale = match(2, 1, MatchStatus.PENDING);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(entryRepository.findByTournamentIdOrderByDrawPositionAsc(TOURNAMENT_ID))
                .thenReturn(List.of(seed, bye, player3, player4));

        bracketService.syncRound1FromEntries(TOURNAMENT_ID);

        assertThat(first.getStatus()).isEqualTo(MatchStatus.BYE);
        assertThat(first.getWinnerEntry()).isSameAs(seed);
        assertThat(second.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(finale.getEntry1()).isSameAs(seed);
    }

    private Match match(int round, int position, MatchStatus status) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRoundOrder(round);
        match.setPositionInRound(position);
        match.setStatus(status);
        matches.add(match);
        return match;
    }

    private Entry entry(Long id, int drawPosition) {
        Entry entry = new Entry();
        entry.setId(id);
        entry.setTournament(tournament);
        entry.setDrawPosition(drawPosition);
        return entry;
    }
}
