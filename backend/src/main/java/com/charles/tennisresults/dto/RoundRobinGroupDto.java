package com.charles.tennisresults.dto;

import java.util.List;

/** Une poule : group = 1 (A) ou 2 (B), complete = ses 6 matchs sont joues. */
public record RoundRobinGroupDto(int group, String name, boolean complete, List<RoundRobinStandingDto> standings) {}
