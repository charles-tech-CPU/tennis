package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.HeadToHeadDto;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.PlayerRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
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
class HeadToHeadServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TournamentRoundRepository tournamentRoundRepository;

    @InjectMocks
    private HeadToHeadService headToHeadService;

    private final List<Match> matches = new ArrayList<>();
    private final List<TournamentRound> rounds = new ArrayList<>();
    private long nextId = 1;

    private Player alcaraz;
    private Player sinner;

    @BeforeEach
    void setUp() {
        alcaraz = player("ALCARAZ");
        sinner = player("SINNER");
        lenient().when(playerRepository.findById(alcaraz.getId())).thenReturn(Optional.of(alcaraz));
        lenient().when(playerRepository.findById(sinner.getId())).thenReturn(Optional.of(sinner));
        lenient()
                .when(matchRepository.findHeadToHead(eq(alcaraz.getId()), eq(sinner.getId()), anyList()))
                .thenReturn(matches);
        lenient()
                .when(tournamentRoundRepository.findByTournamentIdIn(anyCollection()))
                .thenReturn(rounds);
    }

    @Test
    void lesConfrontationsSontTrieesDeLaPlusRecenteALaPlusAncienne() {
        Tournament rolandGarros = tournament("ROLAND GARROS", 2026, 22, false);
        Tournament doha = tournament("DOHA", 2027, 7, false);
        Tournament wimbledon = tournament("WIMBLEDON", 2027, 27, false);
        Tournament wimbledonQ = tournament("WIMBLEDON", 2027, 27, true);
        label(wimbledon, 7, "F"); // libelle du bareme prioritaire sur celui deduit du tableau
        played(rolandGarros, 3, alcaraz, sinner);
        played(doha, 2, sinner, alcaraz);
        played(wimbledonQ, 3, alcaraz, sinner);
        played(wimbledon, 7, sinner, alcaraz);

        HeadToHeadDto h2h = headToHeadService.headToHead(alcaraz.getId(), sinner.getId());

        assertThat(h2h.matches())
                .extracting(m -> m.tournamentName() + " " + m.season() + " " + m.roundLabel())
                .containsExactly("WIMBLEDON 2027 F", "WIMBLEDON 2027 Q3", "DOHA 2027 SF", "ROLAND GARROS 2026 F");
        assertThat(h2h.player1Wins()).isEqualTo(2);
        assertThat(h2h.player2Wins()).isEqualTo(2);
        assertThat(h2h.matches().get(0).winnerPlayerId()).isEqualTo(sinner.getId());
    }

    @Test
    void aucuneConfrontation() {
        HeadToHeadDto h2h = headToHeadService.headToHead(alcaraz.getId(), sinner.getId());

        assertThat(h2h.matches()).isEmpty();
        assertThat(h2h.player1Wins()).isZero();
        assertThat(h2h.player2Wins()).isZero();
        assertThat(h2h.player1().lastName()).isEqualTo("ALCARAZ");
    }

    @Test
    void unJoueurContreLuiMemeEstRefuse() {
        assertThatThrownBy(() -> headToHeadService.headToHead(alcaraz.getId(), alcaraz.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unJoueurInconnuEstSignale() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> headToHeadService.headToHead(alcaraz.getId(), 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Tournament tournament(String name, int season, int week, boolean qualifying) {
        Tournament t = new Tournament();
        t.setId(nextId++);
        t.setName(name);
        t.setSeason(season);
        t.setWeekNumber(week);
        t.setDrawSize(8);
        t.setQualifying(qualifying);
        if (qualifying) {
            t.setMainTournamentId(t.getId() - 1);
        }
        return t;
    }

    private void label(Tournament t, int roundOrder, String label) {
        TournamentRound round = new TournamentRound();
        round.setTournament(t);
        round.setRoundOrder(roundOrder);
        round.setRoundLabel(label);
        round.setPoints(0);
        rounds.add(round);
    }

    private void played(Tournament t, int roundOrder, Player winner, Player loser) {
        Match m = new Match();
        m.setId(nextId++);
        m.setTournament(t);
        m.setRoundOrder(roundOrder);
        m.setEntry1(entry(t, winner));
        m.setEntry2(entry(t, loser));
        m.setWinnerEntry(m.getEntry1());
        m.setScore("6-4 6-4");
        m.setStatus(MatchStatus.COMPLETED);
        matches.add(m);
    }

    private Entry entry(Tournament t, Player p) {
        Entry e = new Entry();
        e.setId(nextId++);
        e.setTournament(t);
        e.setPlayer(p);
        return e;
    }

    private Player player(String lastName) {
        Player p = new Player();
        p.setId(nextId++);
        p.setLastName(lastName);
        return p;
    }
}
