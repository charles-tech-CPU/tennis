package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.RoundPointsDto;
import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentStatus;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentRoundRepository tournamentRoundRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private BracketService bracketService;

    @InjectMocks
    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> {
            Tournament t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
    }

    @Test
    void sansBaremeSaisiLeTournoiRecoitLeBaremeParDefautDeSaCategorie() {
        var dto = tournamentService.create(create(28, null));

        assertThat(savedRoundPoints()).containsExactly(25, 50, 100, 165, 250);
        assertThat(dto.drawSlots()).isEqualTo(32);
        assertThat(dto.status()).isEqualTo(TournamentStatus.NOT_STARTED);
        verify(bracketService).initializeSkeleton(any(Tournament.class), eq(32));
    }

    @Test
    void unBaremeIncompletRepeteSonDernierMontantPourLesToursRestants() {
        tournamentService.create(create(8, List.of(new RoundPointsDto(1, "QF", 10), new RoundPointsDto(2, "SF", 20))));

        assertThat(savedRoundPoints()).containsExactly(10, 20, 20);
    }

    private List<Integer> savedRoundPoints() {
        ArgumentCaptor<TournamentRound> rounds = ArgumentCaptor.forClass(TournamentRound.class);
        verify(tournamentRoundRepository, atLeastOnce()).save(rounds.capture());
        return rounds.getAllValues().stream().map(TournamentRound::getPoints).toList();
    }

    private static TournamentCreateDto create(int drawSize, List<RoundPointsDto> rounds) {
        return new TournamentCreateDto(
                "DOHA", TournamentCategory.ATP_250, 2026, 7, "QATAR", null, drawSize, null, null, null, rounds);
    }
}
