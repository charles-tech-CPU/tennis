package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.TeamTie;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamTieRepository extends JpaRepository<TeamTie, Long> {

    List<TeamTie> findByCompetitionAndSeasonOrderByStageAscPositionAsc(String competition, Integer season);

    List<TeamTie> findByCompetitionAndSeasonAndStageIn(String competition, Integer season, Collection<String> stages);

    Optional<TeamTie> findByCompetitionAndSeasonAndStageAndPosition(
            String competition, Integer season, String stage, Integer position);

    @Query("select distinct t.season from TeamTie t where t.competition = :competition order by t.season")
    List<Integer> findSeasons(@Param("competition") String competition);

    @Query("select coalesce(max(t.position), 0) from TeamTie t "
            + "where t.competition = :competition and t.season = :season and t.stage = :stage")
    int maxPosition(
            @Param("competition") String competition, @Param("season") Integer season, @Param("stage") String stage);
}
