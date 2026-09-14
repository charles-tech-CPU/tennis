package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.MatchDto;
import com.charles.tennisresults.dto.ScoreUpdateDto;
import com.charles.tennisresults.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/tournaments/{tournamentId}/matches")
    public List<MatchDto> findByTournament(@PathVariable Long tournamentId) {
        return matchService.findByTournament(tournamentId);
    }

    @PutMapping("/matches/{matchId}/score")
    public MatchDto recordScore(@PathVariable Long matchId, @Valid @RequestBody ScoreUpdateDto dto) {
        return matchService.recordScore(matchId, dto);
    }
}
