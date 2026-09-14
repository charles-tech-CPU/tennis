package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findFirstByLastNameIgnoreCase(String lastName);
}
