package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.service.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/api/ranking")
    public List<RankingRowDto> ranking(@RequestParam(required = false) Integer season) {
        int s = season != null ? season : Year.now().getValue();
        return rankingService.computeRanking(s);
    }
}
