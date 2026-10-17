package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.service.EntryRankingService;
import com.charles.tennisresults.service.RankingService;
import com.charles.tennisresults.service.RankingSnapshotService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RankingController {

    private final RankingService rankingService;
    private final EntryRankingService entryRankingService;
    private final RankingSnapshotService rankingSnapshotService;

    public RankingController(
            RankingService rankingService,
            EntryRankingService entryRankingService,
            RankingSnapshotService rankingSnapshotService) {
        this.rankingService = rankingService;
        this.entryRankingService = entryRankingService;
        this.rankingSnapshotService = rankingSnapshotService;
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

    /**
     * Efface les photos hebdomadaires du classement (recalculees a la demande) :
     * utile apres une correction faite directement en base, hors de l'appli.
     */
    @DeleteMapping("/api/ranking/snapshots")
    public void clearSnapshots() {
        rankingSnapshotService.invalidateAll();
    }
}
