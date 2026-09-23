package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.service.RankingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/api/ranking")
    public List<RankingRowDto> ranking() {
        return rankingService.computeRanking();
    }
}
