package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.ReplacementDto;
import com.charles.tennisresults.dto.RoundRobinDto;
import com.charles.tennisresults.service.RoundRobinService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/round-robin")
public class RoundRobinController {

    private final RoundRobinService roundRobinService;

    public RoundRobinController(RoundRobinService roundRobinService) {
        this.roundRobinService = roundRobinService;
    }

    @GetMapping
    public RoundRobinDto overview(@PathVariable Long tournamentId) {
        return roundRobinService.overview(tournamentId);
    }

    @PostMapping("/replacements")
    public RoundRobinDto replace(@PathVariable Long tournamentId, @Valid @RequestBody ReplacementDto dto) {
        roundRobinService.replace(tournamentId, dto.withdrawnEntryId(), dto.alternateEntryId());
        return roundRobinService.overview(tournamentId);
    }
}
