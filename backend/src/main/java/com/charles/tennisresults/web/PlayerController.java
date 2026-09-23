package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.PlayerCreateDto;
import com.charles.tennisresults.dto.PlayerDto;
import com.charles.tennisresults.service.PlayerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public List<PlayerDto> findAll() {
        return playerService.findAll();
    }

    @PostMapping
    public PlayerDto create(@Valid @RequestBody PlayerCreateDto dto) {
        return playerService.create(dto);
    }

    @PutMapping("/{id}")
    public PlayerDto update(@PathVariable Long id, @Valid @RequestBody PlayerCreateDto dto) {
        return playerService.update(id, dto);
    }
}
