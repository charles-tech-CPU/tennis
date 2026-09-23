package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.TeamBracketCreateDto;
import com.charles.tennisresults.dto.TeamRubberUpdateDto;
import com.charles.tennisresults.dto.TeamTieCreateDto;
import com.charles.tennisresults.dto.TeamTieDto;
import com.charles.tennisresults.dto.TeamTieUpdateDto;
import com.charles.tennisresults.service.TeamCompetitionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team-ties")
public class TeamCompetitionController {

    private final TeamCompetitionService service;

    public TeamCompetitionController(TeamCompetitionService service) {
        this.service = service;
    }

    /** competition = DAVIS_CUP ou UNITED_CUP. */
    @GetMapping
    public List<TeamTieDto> findBySeason(@RequestParam String competition, @RequestParam Integer season) {
        return service.findBySeason(competition, season);
    }

    /** Saisons qui ont au moins une rencontre. */
    @GetMapping("/seasons")
    public List<Integer> findSeasons(@RequestParam String competition) {
        return service.findSeasons(competition);
    }

    @PostMapping
    public TeamTieDto create(@Valid @RequestBody TeamTieCreateDto dto) {
        return service.create(dto);
    }

    /** Tableau final complet : 4 quarts + demies et finale en attente. */
    @PostMapping("/bracket")
    public List<TeamTieDto> createBracket(@Valid @RequestBody TeamBracketCreateDto dto) {
        return service.createBracket(dto);
    }

    @DeleteMapping("/bracket")
    public void deleteBracket(@RequestParam String competition, @RequestParam Integer season) {
        service.deleteBracket(competition, season);
    }

    @PutMapping("/{tieId}")
    public TeamTieDto update(@PathVariable Long tieId, @RequestBody TeamTieUpdateDto dto) {
        return service.update(tieId, dto);
    }

    @DeleteMapping("/{tieId}")
    public void delete(@PathVariable Long tieId) {
        service.delete(tieId);
    }

    @PutMapping("/{tieId}/rubbers/{rubberOrder}")
    public TeamTieDto updateRubber(
            @PathVariable Long tieId, @PathVariable Integer rubberOrder, @Valid @RequestBody TeamRubberUpdateDto dto) {
        return service.updateRubber(tieId, rubberOrder, dto);
    }
}
