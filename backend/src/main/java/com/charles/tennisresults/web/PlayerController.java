package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.PlayerCreateDto;
import com.charles.tennisresults.dto.PlayerDto;
import com.charles.tennisresults.dto.PlayerProfileDto;
import com.charles.tennisresults.service.PlayerProfileService;
import com.charles.tennisresults.service.PlayerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerProfileService playerProfileService;

    public PlayerController(PlayerService playerService, PlayerProfileService playerProfileService) {
        this.playerService = playerService;
        this.playerProfileService = playerProfileService;
    }

    @GetMapping
    public List<PlayerDto> findAll() {
        return playerService.findAll();
    }

    @GetMapping("/{id}/profile")
    public PlayerProfileDto profile(@PathVariable Long id) {
        return playerProfileService.profile(id);
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
