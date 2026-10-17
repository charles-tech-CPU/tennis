package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.EntryType;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Surface;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.dto.NationCountDto;
import com.charles.tennisresults.dto.PlayerCountDto;
import com.charles.tennisresults.dto.StatsDto;
import com.charles.tennisresults.dto.StreakDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Test
    void statsDUnGrandChelemRemporteParUnQualifie() {
        Player sinner = player("SINNER");
        sinner.setNationality("ITALIE");
        Player alcaraz = player("ALCARAZ");
        Player zverev = player("ZVEREV");
        Player fritz = player("FRITZ");
        Tournament ao = tournament(3);
        ao.setName("AUSTRALIAN OPEN");
        ao.setCategory(TournamentCategory.GRAND_SLAM);
        Entry s = entry(ao, sinner);
        s.setEntryType(EntryType.QUALIFIER);
        Entry a = entry(ao, alcaraz);
        Entry z = entry(ao, zverev);
        Entry f = entry(ao, fritz);
        match(ao, 1, 1, s, a, "6-0 6-0 6-2");
        match(ao, 1, 2, z, f, "6-4 3-6 7-5 6-3");
        Match finale = match(ao, 2, 1, s, z, "6-4 3-6 6-7(5) 6-3 6-2");
        List<Entry> entries = List.of(s, a, z, f);
        when(tournamentRepository.findBySeason(SEASON)).thenReturn(List.of(ao));
        when(matchRepository.findByTournament_IdInAndStatusIn(anyList(), anyList()))
                .thenReturn(matches);
        when(matchRepository.findByTournamentIdAndRoundOrderAndPositionInRound(ao.getId(), 2, 1))
                .thenReturn(Optional.of(finale));
        when(entryRepository.findByTournamentIdOrderByDrawPositionAsc(ao.getId()))
                .thenReturn(entries);
        when(matchRepository.findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(ao.getId()))
                .thenReturn(matches);
        when(entryRepository.findByTournament_IdIn(anyList())).thenReturn(entries);

        StatsDto stats = statsService.computeStats(SEASON);

        assertThat(stats.topTournamentWinners())
                .extracting(PlayerCountDto::lastName)
                .containsExactly("SINNER");
        assertThat(stats.topGrandSlamWinners())
                .extracting(PlayerCountDto::lastName)
                .containsExactly("SINNER");
        assertThat(stats.topMasters1000Winners()).isEmpty();
        assertThat(stats.topNationsByTitles())
                .extracting(NationCountDto::nationality)
                .containsExactly("ITALIE");
        assertThat(stats.topRunnersUp()).extracting(PlayerCountDto::lastName).containsExactly("ZVEREV");
        assertThat(stats.topMatchWinners())
                .first()
                .extracting(PlayerCountDto::count)
                .isEqualTo(2L);
        assertThat(stats.bestQualifierRuns()).singleElement().satisfies(run -> {
            assertThat(run.lastName()).isEqualTo("SINNER");
            assertThat(run.roundReached()).isEqualTo("Titre");
            assertThat(run.tournamentName()).isEqualTo("AUSTRALIAN OPEN");
        });
        assertThat(stats.topBagelsInflicted())
                .singleElement()
                .extracting(PlayerCountDto::count)
                .isEqualTo(2L);
        // 5 sets joues en Grand Chelem = victoire "a la distance" ; 4 sets ne suffisent pas
        assertThat(stats.topEpicWins()).extracting(PlayerCountDto::lastName).containsExactly("SINNER");
        assertThat(stats.mostActivePlayers()).hasSize(4);
        assertThat(stats.topWinRate()).isEmpty(); // moins de 10 matchs joues
    }

    @Test
    void victoiresParSurfaceSurprisesTop10TieBreaksEtMatchsLesPlusLongs() {
        Player sinner = player("SINNER");
        Player alcaraz = player("ALCARAZ");
        Player fils = player("FILS");
        Player moutet = player("MOUTET");
        Tournament rome = tournament(18);
        rome.setSurface(Surface.CLAY);
        Tournament miami = tournament(12);
        miami.setSurface(Surface.HARD);
        // Moutet (80e) bat Sinner (1er) : rapport 80 ; Fils (20e) bat Alcaraz (2e) : rapport 10.
        match(rome, 1, 1, ranked(rome, moutet, 80), ranked(rome, sinner, 1), "7-6(5) 6-7(3) 7-5");
        match(rome, 1, 2, ranked(rome, fils, 20), ranked(rome, alcaraz, 2), "6-4 6-4");
        match(miami, 1, 1, ranked(miami, sinner, 1), ranked(miami, fils, 20), "6-1 6-1");
        when(tournamentRepository.findBySeason(SEASON)).thenReturn(List.of(rome, miami));
        when(matchRepository.findByTournament_IdInAndStatusIn(anyList(), anyList()))
                .thenReturn(matches);

        StatsDto stats = statsService.computeStats(SEASON);

        assertThat(stats.topWinsBySurface()).containsOnlyKeys(Surface.CLAY, Surface.GRASS, Surface.HARD);
        assertThat(stats.topWinsBySurface().get(Surface.CLAY))
                .extracting(PlayerCountDto::lastName)
                .containsExactlyInAnyOrder("MOUTET", "FILS");
        assertThat(stats.topWinsBySurface().get(Surface.HARD))
                .extracting(PlayerCountDto::lastName)
                .containsExactly("SINNER");
        assertThat(stats.topWinsBySurface().get(Surface.GRASS)).isEmpty();
        assertThat(stats.biggestUpsets())
                .extracting(u -> u.winner().lastName())
                .containsExactly("MOUTET", "FILS"); // la victoire de Sinner sur Fils n'en est pas une
        assertThat(stats.biggestUpsets().get(0)).satisfies(u -> {
            assertThat(u.winnerRanking()).isEqualTo(80);
            assertThat(u.loserRanking()).isEqualTo(1);
            assertThat(u.roundLabel()).isEqualTo("SF");
        });
        assertThat(stats.topTop10Wins())
                .extracting(PlayerCountDto::lastName)
                .containsExactlyInAnyOrder("MOUTET", "FILS");
        // 7-6 gagne par Moutet (vainqueur), 6-7 gagne par Sinner (perdant)
        assertThat(stats.topTiebreaksWon())
                .extracting(PlayerCountDto::lastName, PlayerCountDto::count)
                .containsExactlyInAnyOrder(tuple("MOUTET", 1L), tuple("SINNER", 1L));
        assertThat(stats.longestMatches()).first().satisfies(m -> {
            assertThat(m.winner().lastName()).isEqualTo("MOUTET");
            assertThat(m.games()).isEqualTo(13 + 13 + 12);
        });
    }

    private Entry ranked(Tournament t, Player p, int ranking) {
        Entry e = entry(t, p);
        e.setRankingAtEntry(ranking);
        return e;
    }

    private Match match(Tournament t, int round, int position, Entry winner, Entry loser, String score) {
        Match m = new Match();
        m.setId(nextId++);
        m.setTournament(t);
        m.setRoundOrder(round);
        m.setPositionInRound(position);
        m.setEntry1(winner);
        m.setEntry2(loser);
        m.setWinnerEntry(winner);
        m.setScore(score);
        m.setStatus(MatchStatus.COMPLETED);
        matches.add(m);
        return m;
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
