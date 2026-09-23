package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.dto.QualifyingCreateDto;
import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentUpdateDto;
import com.charles.tennisresults.service.TournamentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentControllerTest {

    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private TournamentController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        TournamentCreateDto create =
                new TournamentCreateDto("DOHA", null, 2026, 7, null, null, 32, null, null, null, null);
        TournamentUpdateDto update = new TournamentUpdateDto(null, 8, null, null, null, null, null, null, null);
        QualifyingCreateDto qualifying = new QualifyingCreateDto(16, List.of());

        controller.findAll();
        controller.findOne(1L);
        controller.create(create);
        controller.update(1L, update);
        controller.createQualifying(1L, qualifying);
        controller.delete(1L);

        verify(tournamentService).findAll();
        verify(tournamentService).findOne(1L);
        verify(tournamentService).create(create);
        verify(tournamentService).update(1L, update);
        verify(tournamentService).createQualifying(1L, qualifying);
        verify(tournamentService).delete(1L);
    }
}
