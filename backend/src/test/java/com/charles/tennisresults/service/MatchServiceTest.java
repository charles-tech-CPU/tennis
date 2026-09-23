package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.MatchDto;
import com.charles.tennisresults.dto.ScoreUpdateDto;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    private static final Long MATCH_ID = 10L;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentRoundRepository tournamentRoundRepository;

    @Mock
    private BracketService bracketService;

    @InjectMocks
    private MatchService matchService;

    private Tournament tournament;
    private Entry alcaraz;
    private Entry sinner;

    @BeforeEach
    void setUp() {
        tournament = new Tournament();
        tournament.setId(1L);
        alcaraz = entry(100L);
        sinner = entry(200L);
    }

    @Test
    void unMatchInconnuLeveUneErreur() {
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.recordScore(MATCH_ID, new ScoreUpdateDto("6-4 6-4", 100L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void impossibleDeSaisirUnScoreSiUnJoueurEstInconnu() {
        givenMatch(match(alcaraz, null, MatchStatus.PENDING));

        assertThatThrownBy(() -> matchService.recordScore(MATCH_ID, new ScoreUpdateDto("6-4 6-4", 100L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void leVainqueurDoitEtreUnDesDeuxJoueurs() {
        givenMatch(match(alcaraz, sinner, MatchStatus.SCHEDULED));

        assertThatThrownBy(() -> matchService.recordScore(MATCH_ID, new ScoreUpdateDto("6-4 6-4", 999L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saisirUnScoreTermineLeMatchEtQualifieLeVainqueur() {
        Match match = givenMatch(match(alcaraz, sinner, MatchStatus.SCHEDULED));

        MatchDto dto = matchService.recordScore(MATCH_ID, new ScoreUpdateDto("6-3 7-6(4)", 200L));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(match.getWinnerEntry()).isSameAs(sinner);
        assertThat(dto.score()).isEqualTo("6-3 7-6(4)");
        assertThat(dto.winnerEntryId()).isEqualTo(200L);
        verify(bracketService, never()).resetDownstream(match);
        verify(bracketService).advanceWinner(match, sinner);
    }

    @Test
    void corrigerUnResultatAnnuleDAbordLaSuiteDuTableau() {
        Match match = givenMatch(match(alcaraz, sinner, MatchStatus.COMPLETED));
        match.setWinnerEntry(sinner);

        matchService.recordScore(MATCH_ID, new ScoreUpdateDto("6-4 6-4", 100L));

        assertThat(match.getWinnerEntry()).isSameAs(alcaraz);
        InOrder order = inOrder(bracketService);
        order.verify(bracketService).resetDownstream(match);
        order.verify(bracketService).advanceWinner(match, alcaraz);
    }

    private Match givenMatch(Match match) {
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        return match;
    }

    private Match match(Entry entry1, Entry entry2, MatchStatus status) {
        Match match = new Match();
        match.setId(MATCH_ID);
        match.setTournament(tournament);
        match.setRoundOrder(1);
        match.setPositionInRound(1);
        match.setEntry1(entry1);
        match.setEntry2(entry2);
        match.setStatus(status);
        return match;
    }

    private Entry entry(Long id) {
        Entry entry = new Entry();
        entry.setId(id);
        entry.setTournament(tournament);
        return entry;
    }
}
