package com.charles.tennisresults.dto;

import java.util.List;

public record TeamTieDto(
        Long id,
        String competition,
        Integer season,
        String stage,
        String groupName,
        Integer position,
        String team1,
        String team2,
        String team1Placeholder,
        String team2Placeholder,
        Integer team1Score,
        Integer team2Score,
        Integer winner,
        String status,
        String dates,
        String city,
        String venue,
        String surface,
        List<TeamRubberDto> rubbers) {
    public record TeamRubberDto(
            Long id,
            Integer order,
            boolean doubles,
            String team1Players,
            String team2Players,
            String score,
            Integer winner,
            String status) {}
}
