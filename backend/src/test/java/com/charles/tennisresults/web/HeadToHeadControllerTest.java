package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.service.HeadToHeadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeadToHeadControllerTest {

    @Mock
    private HeadToHeadService headToHeadService;

    @InjectMocks
    private HeadToHeadController controller;

    @Test
    void leFaceAFaceEstCalculeParLeService() {
        controller.headToHead(1L, 2L);

        verify(headToHeadService).headToHead(1L, 2L);
    }
}
