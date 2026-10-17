package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.PlayerCreateDto;
import com.charles.tennisresults.dto.PlayerDto;
import com.charles.tennisresults.dto.PlayerProfileDto;
import com.charles.tennisresults.dto.RankingHistoryPointDto;
import com.charles.tennisresults.service.PlayerProfileService;
import com.charles.tennisresults.service.PlayerService;
import com.charles.tennisresults.service.RankingSnapshotService;
import jakarta.validation.Valid;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerProfileService playerProfileService;
    private final RankingSnapshotService rankingSnapshotService;

    public PlayerController(
            PlayerService playerService,
            PlayerProfileService playerProfileService,
            RankingSnapshotService rankingSnapshotService) {
        this.playerService = playerService;
        this.playerProfileService = playerProfileService;
        this.rankingSnapshotService = rankingSnapshotService;
    }

    @GetMapping
    public List<PlayerDto> findAll() {
        return playerService.findAll();
    }

    @GetMapping("/{id}/profile")
    public PlayerProfileDto profile(@PathVariable Long id) {
        return playerProfileService.profile(id);
    }

    /** Classement du joueur semaine par semaine sur la saison (par defaut l'annee en cours). */
    @GetMapping("/{id}/ranking-history")
    public List<RankingHistoryPointDto> rankingHistory(
            @PathVariable Long id, @RequestParam(required = false) Integer season) {
        int s = season != null ? season : Year.now(ZoneId.of("Europe/Paris")).getValue();
        return rankingSnapshotService.history(id, s);
    }

    @PostMapping
    public PlayerDto create(@Valid @RequestBody PlayerCreateDto dto) {
        return playerService.create(dto);
    }

    @PatchMapping("/{id}/favorite")
    public PlayerDto toggleFavorite(@PathVariable Long id) {
        return playerService.toggleFavorite(id);
    }

    @PutMapping("/{id}")
    public PlayerDto update(@PathVariable Long id, @Valid @RequestBody PlayerCreateDto dto) {
        return playerService.update(id, dto);
    }
}
