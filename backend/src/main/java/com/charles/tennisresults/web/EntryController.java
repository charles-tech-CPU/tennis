package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.EntryCreateDto;
import com.charles.tennisresults.dto.EntryDto;
import com.charles.tennisresults.service.EntryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/entries")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping
    public List<EntryDto> findByTournament(@PathVariable Long tournamentId) {
        return entryService.findByTournament(tournamentId);
    }

    @PostMapping
    public EntryDto create(@PathVariable Long tournamentId, @Valid @RequestBody EntryCreateDto dto) {
        return entryService.create(tournamentId, dto);
    }

    @DeleteMapping("/{entryId}")
    public void delete(@PathVariable Long tournamentId, @PathVariable Long entryId) {
        entryService.delete(entryId);
    }
}
