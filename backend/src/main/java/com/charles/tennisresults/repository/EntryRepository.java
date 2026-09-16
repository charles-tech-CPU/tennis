package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.Entry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntryRepository extends JpaRepository<Entry, Long> {
    List<Entry> findByTournamentIdOrderByDrawPositionAsc(Long tournamentId);

    List<Entry> findByPlayerId(Long playerId);

    List<Entry> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    List<Entry> findByPlayerIsNotNull();

    long countByTournamentId(Long tournamentId);
}
