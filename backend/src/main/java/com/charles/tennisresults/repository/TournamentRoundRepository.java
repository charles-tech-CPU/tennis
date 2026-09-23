package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.TournamentRound;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRoundRepository extends JpaRepository<TournamentRound, Long> {
    List<TournamentRound> findByTournamentIdOrderByRoundOrderAsc(Long tournamentId);
}
