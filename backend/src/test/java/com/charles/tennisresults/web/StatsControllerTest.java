package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.service.StatsService;
import java.time.Year;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private StatsController statsController;

    @Test
    void laSaisonDemandeeEstTransmise() {
        statsController.stats(2026);

        verify(statsService).computeStats(2026);
    }

    @Test
    void sansSaisonOnPrendLAnneeEnCours() {
        statsController.stats(null);

        verify(statsService).computeStats(Year.now(ZoneId.of("Europe/Paris")).getValue());
    }
}
