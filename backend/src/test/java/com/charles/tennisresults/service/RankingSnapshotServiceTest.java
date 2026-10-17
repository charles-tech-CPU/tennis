package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.dto.RankingHistoryPointDto;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.RankingSnapshotRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingSnapshotServiceTest {

    private static final int SEASON = 2026;

    @Mock
    private RankingService rankingService;

    @Mock
    private RankingSnapshotRepository snapshotRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private RankingSnapshotService service;

    private final List<Tournament> tournaments = new ArrayList<>();
    private final List<Match> decided = new ArrayList<>();
    private long nextId = 1;

    @Test
    void seulesLesSemainesManquantesSontCalculeesJusquaLaSemaineSuivantLeDernierTournoiCommence() {
        started(tournament(1));
        started(tournament(3));
        tournament(5); // pas encore commence : n'etend pas l'historique
        stubCalendar();
        when(snapshotRepository.computedWeeks(SEASON)).thenReturn(Set.of(2));
        List<RankingRowDto> rankingWeek3 = List.of();
        List<RankingRowDto> rankingWeek4 = List.of();
        when(rankingService.computeRankingBefore(SEASON, 3)).thenReturn(rankingWeek3);
        when(rankingService.computeRankingBefore(SEASON, 4)).thenReturn(rankingWeek4);
        List<RankingHistoryPointDto> history = List.of(new RankingHistoryPointDto(SEASON, 4, 12, 300));
        when(snapshotRepository.historyOf(7L, SEASON)).thenReturn(history);

        assertThat(service.history(7L, SEASON)).isSameAs(history);

        verify(rankingService, never()).computeRankingBefore(SEASON, 2); // deja calculee
        verify(snapshotRepository).saveWeek(SEASON, 3, rankingWeek3);
        verify(snapshotRepository).saveWeek(SEASON, 4, rankingWeek4);
        verify(rankingService, never()).computeRankingBefore(SEASON, 5);
    }

    @Test
    void aucuneSemaineNEstCalculeeTantQuAucunTournoiNACommence() {
        tournament(1);
        stubCalendar();

        assertThat(service.history(7L, SEASON)).isEmpty();

        verify(snapshotRepository, never()).saveWeek(anyInt(), anyInt(), anyList());
    }

    @Test
    void unResultatEffaceLesPhotosDesSemainesSuivantes() {
        service.invalidateFrom(tournament(10));

        verify(snapshotRepository).deleteAfter(SEASON, 10);
    }

    @Test
    void desQualifsComptentPourLaSemaineDeLeurTournoiPrincipal() {
        Tournament main = tournament(17);
        Tournament qualifying = tournament(16);
        qualifying.setQualifying(true);
        qualifying.setMainTournamentId(main.getId());
        when(tournamentRepository.findById(main.getId())).thenReturn(Optional.of(main));

        service.invalidateFrom(qualifying);

        verify(snapshotRepository).deleteAfter(SEASON, 17);
    }

    @Test
    void sansSemaineConnueToutEstEfface() {
        service.invalidateFrom(tournament(null));

        verify(snapshotRepository).deleteAll();
        verify(snapshotRepository, never()).deleteAfter(anyInt(), anyInt());
    }

    @Test
    void lEffacementCompletEstDelegueAuDepot() {
        service.invalidateAll();

        verify(snapshotRepository).deleteAll();
    }

    private Tournament tournament(Integer week) {
        Tournament t = new Tournament();
        t.setId(nextId++);
        t.setName("T" + t.getId());
        t.setCategory(TournamentCategory.ATP_250);
        t.setSeason(SEASON);
        t.setWeekNumber(week);
        t.setDrawSize(32);
        tournaments.add(t);
        return t;
    }

    /** Un 1er tour joue : le tournoi est commence. */
    private void started(Tournament t) {
        Match m = new Match();
        m.setId(nextId++);
        m.setTournament(t);
        m.setRoundOrder(1);
        m.setPositionInRound(1);
        m.setStatus(MatchStatus.COMPLETED);
        decided.add(m);
    }

    private void stubCalendar() {
        when(tournamentRepository.findBySeason(SEASON)).thenReturn(tournaments);
        when(tournamentRepository.findAll()).thenReturn(tournaments);
        when(matchRepository.findByTournament_IdInAndStatusIn(
                        anyList(), eq(List.of(MatchStatus.COMPLETED, MatchStatus.BYE))))
                .thenReturn(decided);
    }
}
