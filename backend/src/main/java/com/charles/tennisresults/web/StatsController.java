package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.StatsDto;
import com.charles.tennisresults.service.StatsService;
import java.time.Year;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/api/stats")
    public StatsDto stats(@RequestParam(required = false) Integer season) {
        int s = season != null ? season : Year.now().getValue();
        return statsService.computeStats(s);
    }
}
