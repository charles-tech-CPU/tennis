package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntryRankingServiceTest {

    private final RankingService rankingService = mock(RankingService.class);
    private final EntryRepository entryRepository = mock(EntryRepository.class);
    private final TournamentRepository tournamentRepository = mock(TournamentRepository.class);
    private final EntryRankingService service =
            new EntryRankingService(rankingService, entryRepository, tournamentRepository);

    private final Player sinner = player(1L);
    private final Player alcaraz = player(2L);

    @Test
    void figeLeClassementDuDebutDeLaSemaineDuTournoi() {
        Tournament t = tournament(10L, 2026, 8);
        Entry eSinner = entry(t, sinner);
        Entry eAlcaraz = entry(t, alcaraz);
        Entry eInconnu = entry(t, player(3L));
        Entry bye = entry(t, null);
        when(entryRepository.findByTournamentId(t.getId())).thenReturn(List.of(eSinner, eAlcaraz, eInconnu, bye));
        when(rankingService.computeRankingBefore(2026, 8)).thenReturn(List.of(row(sinner), row(alcaraz)));

        service.freezeIfNeeded(t);

        assertThat(eSinner.getRankingAtEntry()).isEqualTo(1);
        assertThat(eAlcaraz.getRankingAtEntry()).isEqualTo(2);
        assertThat(eInconnu.getRankingAtEntry()).isNull(); // non classe
        assertThat(bye.getRankingAtEntry()).isNull();
        assertThat(t.isRankingsFrozen()).isTrue();
    }

    @Test
    void desQualifsPrennentLaSemaineDeLeurTableauPrincipal() {
        Tournament main = tournament(10L, 2026, 8);
        Tournament quali = tournament(11L, 2026, 7);
        quali.setQualifying(true);
        quali.setMainTournamentId(main.getId());
        Entry eSinner = entry(quali, sinner);
        when(tournamentRepository.findById(main.getId())).thenReturn(java.util.Optional.of(main));
        when(entryRepository.findByTournamentId(quali.getId())).thenReturn(List.of(eSinner));
        when(rankingService.computeRankingBefore(2026, 8)).thenReturn(List.of(row(sinner)));

        service.freezeIfNeeded(quali);

        assertThat(eSinner.getRankingAtEntry()).isEqualTo(1);
    }

    @Test
    void unTournoiDejaFigeNEstPasRecalcule() {
        Tournament t = tournament(10L, 2026, 8);
        t.setRankingsFrozen(true);

        service.freezeIfNeeded(t);

        verify(rankingService, never()).computeRankingBefore(2026, 8);
    }

    @Test
    void leRattrapageRemplitLesTournoisFigesSansEcraserLExistant() {
        Tournament doha = tournament(10L, 2026, 7);
        doha.setRankingsFrozen(true);
        Tournament dubai = tournament(20L, 2026, 8);
        dubai.setRankingsFrozen(true);
        Tournament futur = tournament(30L, 2026, 9); // pas encore demarre : pas touche
        Entry eDoha = entry(doha, sinner);
        Entry eDubai = entry(dubai, alcaraz);
        Entry dejaFige = entry(dubai, sinner);
        dejaFige.setRankingAtEntry(42);
        when(tournamentRepository.findAll()).thenReturn(List.of(doha, dubai, futur));
        when(entryRepository.findByTournament_IdIn(anyList())).thenReturn(List.of(eDoha, eDubai, dejaFige));
        when(rankingService.computeRankingBefore(2026, 7)).thenReturn(List.of(row(sinner)));
        when(rankingService.computeRankingBefore(2026, 8)).thenReturn(List.of(row(sinner), row(alcaraz)));

        int filled = service.backfillFrozenTournaments();

        assertThat(filled).isEqualTo(2);
        assertThat(eDoha.getRankingAtEntry()).isEqualTo(1);
        assertThat(eDubai.getRankingAtEntry()).isEqualTo(2);
        assertThat(dejaFige.getRankingAtEntry()).isEqualTo(42);
        verify(rankingService, never()).computeRankingBefore(2026, 9);
    }

    private static Tournament tournament(Long id, int season, int week) {
        Tournament t = new Tournament();
        t.setId(id);
        t.setSeason(season);
        t.setWeekNumber(week);
        return t;
    }

    private static Player player(Long id) {
        Player p = new Player();
        p.setId(id);
        return p;
    }

    private static Entry entry(Tournament t, Player p) {
        Entry e = new Entry();
        e.setTournament(t);
        e.setPlayer(p);
        return e;
    }

    private static RankingRowDto row(Player p) {
        return new RankingRowDto(
                p.getId(), "X", null, null, Map.of(), null, null, List.of(), null, List.of(), 0, 0, 0, 0, List.of());
    }
}
