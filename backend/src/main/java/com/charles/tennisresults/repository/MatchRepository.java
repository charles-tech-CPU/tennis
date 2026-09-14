package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(Long tournamentId);

    Optional<Match> findByTournamentIdAndRoundOrderAndPositionInRound(
            Long tournamentId, Integer roundOrder, Integer positionInRound);

    List<Match> findByEntry1_IdOrEntry2_Id(Long entry1Id, Long entry2Id);
}
