package com.charles.tennisresults.web;

import static org.mockito.Mockito.verify;

import com.charles.tennisresults.dto.TeamBracketCreateDto;
import com.charles.tennisresults.dto.TeamRubberUpdateDto;
import com.charles.tennisresults.dto.TeamTieCreateDto;
import com.charles.tennisresults.dto.TeamTieUpdateDto;
import com.charles.tennisresults.service.TeamCompetitionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamCompetitionControllerTest {

    @Mock
    private TeamCompetitionService service;

    @InjectMocks
    private TeamCompetitionController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        TeamTieCreateDto create = new TeamTieCreateDto(
                "DAVIS_CUP", 2026, "QUALIFIERS_R1", null, "FRANCE", "ITALIE", null, null, null, null, 5);
        TeamBracketCreateDto bracket =
                new TeamBracketCreateDto("DAVIS_CUP", 2026, List.of(), null, null, null, null, 3);
        TeamTieUpdateDto update = new TeamTieUpdateDto("FRANCE", "ITALIE", null, null, null, null, null);
        TeamRubberUpdateDto rubber = new TeamRubberUpdateDto(null, null, "6-4 6-4", 1, "COMPLETED");

        controller.findBySeason("DAVIS_CUP", 2026);
        controller.findSeasons("DAVIS_CUP");
        controller.create(create);
        controller.createBracket(bracket);
        controller.deleteBracket("DAVIS_CUP", 2026);
        controller.update(1L, update);
        controller.delete(1L);
        controller.updateRubber(1L, 2, rubber);

        verify(service).findBySeason("DAVIS_CUP", 2026);
        verify(service).findSeasons("DAVIS_CUP");
        verify(service).create(create);
        verify(service).createBracket(bracket);
        verify(service).deleteBracket("DAVIS_CUP", 2026);
        verify(service).update(1L, update);
        verify(service).delete(1L);
        verify(service).updateRubber(1L, 2, rubber);
    }
}
