package com.charles.tennisresults.repository;

import com.charles.tennisresults.domain.Player;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findFirstByLastNameIgnoreCase(String lastName);
}
