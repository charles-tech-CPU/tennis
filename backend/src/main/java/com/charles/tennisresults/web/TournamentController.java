package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.QualifyingCreateDto;
import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentDto;
import com.charles.tennisresults.dto.TournamentUpdateDto;
import com.charles.tennisresults.service.TournamentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{id}")
    public TournamentDto update(@PathVariable Long id, @Valid @RequestBody TournamentUpdateDto dto) {
        return tournamentService.update(id, dto);
    }

    @PostMapping("/{id}/qualifying")
    public TournamentDto createQualifying(@PathVariable Long id, @Valid @RequestBody QualifyingCreateDto dto) {
        return tournamentService.createQualifying(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        tournamentService.delete(id);
    }
}
