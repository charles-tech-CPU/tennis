package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderByRoundOrderAscPositionInRoundAsc(Long tournamentId);

    Optional<Match> findByTournamentIdAndRoundOrderAndPositionInRound(
            Long tournamentId, Integer roundOrder, Integer positionInRound);

    List<Match> findByEntry1_IdOrEntry2_Id(Long entry1Id, Long entry2Id);

    List<Match> findByTournament_IdInAndStatusIn(List<Long> tournamentIds, List<MatchStatus> statuses);

    void deleteByTournamentId(Long tournamentId);

    /** Matchs decides (avec un vainqueur) ayant oppose ces deux joueurs, dans un sens ou dans l'autre. */
    @Query("""
            select m from Match m
            join fetch m.tournament t
            join fetch m.entry1 e1
            join fetch m.entry2 e2
            join fetch m.winnerEntry w
            where m.status in :statuses
              and ((e1.player.id = :player1Id and e2.player.id = :player2Id)
                or (e1.player.id = :player2Id and e2.player.id = :player1Id))
            """)
    List<Match> findHeadToHead(
            @Param("player1Id") Long player1Id,
            @Param("player2Id") Long player2Id,
            @Param("statuses") List<MatchStatus> statuses);
}
