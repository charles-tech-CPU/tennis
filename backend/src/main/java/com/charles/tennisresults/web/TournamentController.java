package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentDto;
import com.charles.tennisresults.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public List<TournamentDto> findAll() {
        return tournamentService.findAll();
    }

    @GetMapping("/{id}")
    public TournamentDto findOne(@PathVariable Long id) {
        return tournamentService.findOne(id);
    }

    @PostMapping
    public TournamentDto create(@Valid @RequestBody TournamentCreateDto dto) {
        return tournamentService.create(dto);
    }
}
