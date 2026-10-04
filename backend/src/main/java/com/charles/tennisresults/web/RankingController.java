package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.service.EntryRankingService;
import com.charles.tennisresults.service.RankingService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RankingController {

    private final RankingService rankingService;
    private final EntryRankingService entryRankingService;

    public RankingController(RankingService rankingService, EntryRankingService entryRankingService) {
        this.rankingService = rankingService;
        this.entryRankingService = entryRankingService;
    }

    @GetMapping("/api/ranking")
    public List<RankingRowDto> ranking() {
        return rankingService.computeRanking();
    }

    /** Rattrapage du classement au debut du tournoi pour les tournois deja demarres. */
    @PostMapping("/api/ranking/backfill-entry-rankings")
    public Map<String, Integer> backfillEntryRankings() {
        return Map.of("filledEntries", entryRankingService.backfillFrozenTournaments());
    }
}
