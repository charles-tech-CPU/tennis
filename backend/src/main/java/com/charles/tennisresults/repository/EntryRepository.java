package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.Entry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, Long> {
    List<Entry> findByTournamentIdOrderByDrawPositionAsc(Long tournamentId);

    List<Entry> findByPlayerId(Long playerId);

    List<Entry> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    List<Entry> findByPlayerIsNotNull();

    List<Entry> findByTournament_IdIn(List<Long> tournamentIds);

    long countByTournamentId(Long tournamentId);
}
