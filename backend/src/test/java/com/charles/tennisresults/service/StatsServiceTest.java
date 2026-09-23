package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.dto.StatsDto;
import com.charles.tennisresults.dto.StreakDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    private static final int SEASON = 2026;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private StatsService statsService;

    private final List<Match> matches = new ArrayList<>();
    private long nextId = 1;

    @Test
    void laSerieDeVictoiresSuitLOrdreDesSemaines() {
        Player alcaraz = player("ALCARAZ");
        Player sinner = player("SINNER");
        Player zverev = player("ZVEREV");
        Tournament doha = tournament(7);
        Tournament dubai = tournament(8);
        // semaine 8 declaree en premier : l'ordre chronologique doit venir du numero de semaine
        played(dubai, 1, alcaraz, zverev);
        played(dubai, 2, sinner, alcaraz);
        played(doha, 1, alcaraz, zverev);
        played(doha, 2, alcaraz, sinner);
        when(tournamentRepository.findBySeason(SEASON)).thenReturn(List.of(doha, dubai));
        when(matchRepository.findByTournament_IdInAndStatusIn(anyList(), anyList()))
                .thenReturn(matches);

        StatsDto stats = statsService.computeStats(SEASON);

        assertThat(stats.longestWinStreaks())
                .filteredOn(s -> s.lastName().equals("ALCARAZ"))
                .singleElement()
                .extracting(StreakDto::streakLength)
                .isEqualTo(3);
        assertThat(stats.longestWinStreaks())
                .filteredOn(s -> s.lastName().equals("ZVEREV"))
                .isEmpty();
    }

    private Tournament tournament(int week) {
        Tournament t = new Tournament();
        t.setId(nextId++);
        t.setName("T" + week);
        t.setSeason(SEASON);
        t.setWeekNumber(week);
        t.setDrawSize(4);
        t.setCategory(TournamentCategory.ATP_250);
        return t;
    }

    private void played(Tournament t, int round, Player winner, Player loser) {
        Entry w = entry(t, winner);
        Match m = new Match();
        m.setId(nextId++);
        m.setTournament(t);
        m.setRoundOrder(round);
        m.setPositionInRound(1);
        m.setEntry1(w);
        m.setEntry2(entry(t, loser));
        m.setWinnerEntry(w);
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
