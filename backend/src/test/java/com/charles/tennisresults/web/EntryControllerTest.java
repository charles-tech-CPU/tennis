package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.dto.EntryCreateDto;
import com.charles.tennisresults.service.EntryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntryControllerTest {

    @Mock
    private EntryService entryService;

    @InjectMocks
    private EntryController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        EntryCreateDto create = new EntryCreateDto(50L, 1, 1, null, false);

        controller.findByTournament(1L);
        controller.create(1L, create);
        controller.delete(1L, 7L);

        verify(entryService).findByTournament(1L);
        verify(entryService).create(1L, create);
        verify(entryService).delete(7L);
    }
}
