package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.MandatorySlot;
import com.charles.tennisresults.domain.Tournament;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findBySeason(Integer season);

    Optional<Tournament> findBySeasonAndMandatorySlot(Integer season, MandatorySlot mandatorySlot);

    Optional<Tournament> findByMainTournamentId(Long mainTournamentId);
}
