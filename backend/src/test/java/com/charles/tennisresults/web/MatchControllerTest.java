package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.dto.ScoreUpdateDto;
import com.charles.tennisresults.service.MatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock
    private MatchService matchService;

    @InjectMocks
    private MatchController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        ScoreUpdateDto score = new ScoreUpdateDto("6-4 6-4", 10L);

        controller.findByTournament(1L);
        controller.recordScore(3L, score);

        verify(matchService).findByTournament(1L);
        verify(matchService).recordScore(3L, score);
    }
}
