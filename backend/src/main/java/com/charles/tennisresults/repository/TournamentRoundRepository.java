package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.TournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRoundRepository extends JpaRepository<TournamentRound, Long> {
    List<TournamentRound> findByTournamentIdOrderByRoundOrderAsc(Long tournamentId);
}
