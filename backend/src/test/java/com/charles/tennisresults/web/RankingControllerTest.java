package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.service.RankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingControllerTest {

    @Mock
    private RankingService rankingService;

    @InjectMocks
    private RankingController controller;

    @Test
    void leClassementEstCalculeParLeService() {
        controller.ranking();

        verify(rankingService).computeRanking();
    }
}
