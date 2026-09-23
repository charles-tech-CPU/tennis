package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.dto.PlayerCreateDto;
import com.charles.tennisresults.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private PlayerController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        PlayerCreateDto dto = new PlayerCreateDto("FILS", "Arthur", "FRANCE");

        controller.findAll();
        controller.create(dto);
        controller.update(1L, dto);

        verify(playerService).findAll();
        verify(playerService).create(dto);
        verify(playerService).update(1L, dto);
    }
}
