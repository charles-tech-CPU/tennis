package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.EntryCreateDto;
import com.charles.tennisresults.dto.EntryDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.PlayerRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntryServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private BracketService bracketService;

    @InjectMocks
    private EntryService entryService;

    private Tournament doha;
    private Player alcaraz;

    @BeforeEach
    void setUp() {
        doha = tournament(1L, "DOHA", 7);
        alcaraz = new Player();
        alcaraz.setId(50L);
        alcaraz.setLastName("ALCARAZ");
        alcaraz.setFirstName("Carlos");
        alcaraz.setNationality("ESPAGNE");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(doha));
        lenient().when(playerRepository.findById(50L)).thenReturn(Optional.of(alcaraz));
        lenient().when(entryRepository.save(any(Entry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void inscrireUnJoueurMetAJourLePremierTour() {
        EntryDto dto = entryService.create(1L, new EntryCreateDto(50L, 3, 1, null, false));

        assertThat(dto.playerLastName()).isEqualTo("ALCARAZ");
        assertThat(dto.playerNationality()).isEqualTo("ESPAGNE");
        assertThat(dto.seed()).isEqualTo(1);
        verify(bracketService).syncRound1FromEntries(1L);
    }

    @Test
    void unByeSAfficheCommeTel() {
        EntryDto dto = entryService.create(1L, new EntryCreateDto(null, 2, null, null, true));

        assertThat(dto.bye()).isTrue();
        assertThat(dto.playerId()).isNull();
        assertThat(dto.playerLastName()).isEqualTo("BYE");
    }

    @Test
    void unJoueurNePeutPasJouerDeuxTournoisLaMemeSemaine() {
        givenAlreadyEnteredIn(tournament(2L, "DUBAI", 7));
        EntryCreateDto dto = new EntryCreateDto(50L, 3, null, null, false);

        assertThatThrownBy(() -> entryService.create(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DUBAI");
    }

    @Test
    void lesQualifsDuMemeTournoiNeSontPasUnConflit() {
        Tournament qualifs = tournament(2L, "DOHA - QUALIFS", 7);
        qualifs.setQualifying(true);
        qualifs.setMainTournamentId(1L);
        when(tournamentRepository.findByMainTournamentId(1L)).thenReturn(Optional.of(qualifs));
        givenAlreadyEnteredIn(qualifs);

        EntryDto dto = entryService.create(1L, new EntryCreateDto(50L, 3, null, null, false));

        assertThat(dto.playerId()).isEqualTo(50L);
    }

    @Test
    void lesMasters1000ExemptesNeDeclenchentPasDeConflit() {
        givenAlreadyEnteredIn(tournament(2L, "Miami", 7));

        EntryDto dto = entryService.create(1L, new EntryCreateDto(50L, 3, null, null, false));

        assertThat(dto.playerId()).isEqualTo(50L);
    }

    @Test
    void unAutreTournoiUneAutreSemaineNEstPasUnConflit() {
        givenAlreadyEnteredIn(tournament(2L, "DUBAI", 8));

        EntryDto dto = entryService.create(1L, new EntryCreateDto(50L, 3, null, null, false));

        assertThat(dto.playerId()).isEqualTo(50L);
    }

    @Test
    void unJoueurNeSInscritQuUneFoisDansUnTableau() {
        when(entryRepository.findByTournamentIdAndPlayerId(1L, 50L)).thenReturn(List.of(new Entry()));
        EntryCreateDto dto = new EntryCreateDto(50L, 3, null, null, false);

        assertThatThrownBy(() -> entryService.create(1L, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    private void givenAlreadyEnteredIn(Tournament other) {
        Entry existing = new Entry();
        existing.setTournament(other);
        existing.setPlayer(alcaraz);
        when(entryRepository.findByPlayerId(50L)).thenReturn(List.of(existing));
    }

    private static Tournament tournament(Long id, String name, int week) {
        Tournament t = new Tournament();
        t.setId(id);
        t.setName(name);
        t.setSeason(2026);
        t.setWeekNumber(week);
        return t;
    }
}
