package com.charles.tennisresults.dto;

import java.util.List;

/** Phase de poules d'un tournoi round robin et ses remplacants (positions 9+). */
public record RoundRobinDto(List<RoundRobinGroupDto> groups, List<EntryDto> alternates) {}
