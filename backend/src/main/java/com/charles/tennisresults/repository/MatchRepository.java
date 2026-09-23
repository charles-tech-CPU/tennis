package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(Long tournamentId);

    Optional<Match> findByTournamentIdAndRoundOrderAndPositionInRound(
            Long tournamentId, Integer roundOrder, Integer positionInRound);

    List<Match> findByEntry1_IdOrEntry2_Id(Long entry1Id, Long entry2Id);

    List<Match> findByTournament_IdInAndStatusIn(List<Long> tournamentIds, List<MatchStatus> statuses);

    void deleteByTournamentId(Long tournamentId);
}
