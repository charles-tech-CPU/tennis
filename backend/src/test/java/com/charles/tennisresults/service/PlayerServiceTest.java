package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.dto.PlayerCreateDto;
import com.charles.tennisresults.dto.PlayerDto;
import com.charles.tennisresults.repository.PlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void lesJoueursSontTriesParNom() {
        when(playerRepository.findAll())
                .thenReturn(
                        List.of(new Player("SINNER", "Jannik", "ITALIE"), new Player("ALCARAZ", "Carlos", "ESPAGNE")));

        assertThat(playerService.findAll()).extracting(PlayerDto::lastName).containsExactly("ALCARAZ", "SINNER");
    }

    @Test
    void creerUnJoueur() {
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        PlayerDto dto = playerService.create(new PlayerCreateDto("FILS", "Arthur", "FRANCE"));

        assertThat(dto.lastName()).isEqualTo("FILS");
        assertThat(dto.nationality()).isEqualTo("FRANCE");
    }

    @Test
    void corrigerUnJoueur() {
        Player player = new Player("MPETSHI", null, null);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);

        PlayerDto dto = playerService.update(1L, new PlayerCreateDto("MPETSHI PERRICARD", "Giovanni", "FRANCE"));

        assertThat(dto.lastName()).isEqualTo("MPETSHI PERRICARD");
        assertThat(dto.firstName()).isEqualTo("Giovanni");
    }

    @Test
    void corrigerUnJoueurInconnuLeveUneErreur() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());
        PlayerCreateDto dto = new PlayerCreateDto("X", null, null);

        assertThatThrownBy(() -> playerService.update(1L, dto)).isInstanceOf(EntityNotFoundException.class);
    }
}
