package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.QualifyingCreateDto;
import com.charles.tennisresults.dto.RoundPointsDto;
import com.charles.tennisresults.dto.TournamentCreateDto;
import com.charles.tennisresults.dto.TournamentDto;
import com.charles.tennisresults.dto.TournamentStatus;
import com.charles.tennisresults.dto.TournamentUpdateDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentRoundRepository tournamentRoundRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private BracketService bracketService;

    @InjectMocks
    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        lenient().when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> {
            Tournament t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(99L);
            }
            return t;
        });
    }

    // --- creation ---

    @Test
    void sansBaremeSaisiLeTournoiRecoitLeBaremeParDefautDeSaCategorie() {
        TournamentDto dto = tournamentService.create(create(28, null));

        assertThat(savedRoundPoints()).containsExactly(25, 50, 100, 165, 250);
        assertThat(dto.drawSlots()).isEqualTo(32);
        assertThat(dto.status()).isEqualTo(TournamentStatus.NOT_STARTED);
        verify(bracketService).initializeSkeleton(any(Tournament.class), eq(32));
    }

    @Test
    void unBaremeIncompletRepeteSonDernierMontantPourLesToursRestants() {
        tournamentService.create(create(8, List.of(new RoundPointsDto(1, "QF", 10), new RoundPointsDto(2, "SF", 20))));

        assertThat(savedRoundPoints()).containsExactly(10, 20, 20);
    }

    // --- lecture ---

    @Test
    void laListeExclutLesQualifsEtTrieParSaisonPuisSemainePuisCategorie() {
        Tournament doha = tournament(1L, "DOHA", TournamentCategory.ATP_250, 7);
        Tournament rotterdam = tournament(2L, "ROTTERDAM", TournamentCategory.ATP_500, 7);
        Tournament brisbane = tournament(3L, "BRISBANE", TournamentCategory.ATP_250, 1);
        Tournament qualifs = tournament(4L, "DOHA - Qualifs", TournamentCategory.ATP_250, 7);
        qualifs.setQualifying(true);
        qualifs.setMainTournamentId(1L);
        when(tournamentRepository.findAll()).thenReturn(List.of(doha, rotterdam, brisbane, qualifs));

        List<TournamentDto> list = tournamentService.findAll();

        assertThat(list).extracting(TournamentDto::name).containsExactly("BRISBANE", "ROTTERDAM", "DOHA");
    }

    @Test
    void unTournoiInconnuLeveUneErreur() {
        when(tournamentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tournamentService.findOne(5L)).isInstanceOf(EntityNotFoundException.class);
    }

    // --- modification ---

    @Test
    void modifierLeBaremeEtDeplacerLesQualifsDUnMasters1000() {
        Tournament madrid = givenTournament(1L, "MADRID", TournamentCategory.MASTERS_1000, 17);
        TournamentRound r1 = new TournamentRound(madrid, 1, "R64", 10);
        madrid.getRounds().add(r1);
        Tournament qualifs = tournament(2L, "MADRID - Qualifs", TournamentCategory.MASTERS_1000, 17);
        when(tournamentRepository.findByMainTournamentId(1L)).thenReturn(Optional.of(qualifs));

        tournamentService.update(1L, update(18, List.of(new RoundPointsDto(1, "R64", 25)), null));

        assertThat(madrid.getWeekNumber()).isEqualTo(18);
        assertThat(r1.getPoints()).isEqualTo(25);
        // qualifs d'un Masters 1000 : la semaine precedant le tableau principal
        assertThat(qualifs.getWeekNumber()).isEqualTo(17);
    }

    @Test
    void redimensionnerUnTableauVideRegenereSesTours() {
        Tournament rotterdam = givenTournament(1L, "ROTTERDAM", TournamentCategory.ATP_500, 7);
        rotterdam.setDrawSize(48);

        tournamentService.update(1L, update(7, null, 32));

        assertThat(rotterdam.getDrawSize()).isEqualTo(32);
        verify(matchRepository).deleteByTournamentId(1L);
        assertThat(savedRoundPoints()).containsExactly(45, 90, 180, 500, 500);
        verify(bracketService).initializeSkeleton(rotterdam, 32);
    }

    @Test
    void impossibleDeRedimensionnerUnTableauDejaRempli() {
        givenTournament(1L, "ROTTERDAM", TournamentCategory.ATP_500, 7).setDrawSize(48);
        when(entryRepository.countByTournamentId(1L)).thenReturn(3L);
        TournamentUpdateDto dto = update(7, null, 32);

        assertThatThrownBy(() -> tournamentService.update(1L, dto)).isInstanceOf(IllegalArgumentException.class);
        verify(matchRepository, never()).deleteByTournamentId(any());
    }

    @Test
    void impossibleDeRedimensionnerDesQualifs() {
        Tournament qualifs = givenTournament(1L, "DOHA - Qualifs", TournamentCategory.ATP_250, 7);
        qualifs.setQualifying(true);
        qualifs.setDrawSize(16);
        TournamentUpdateDto dto = update(7, null, 24);

        assertThatThrownBy(() -> tournamentService.update(1L, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- qualifications ---

    @Test
    void lesQualifsDUnGrandChelemSeJouentLaSemainePrecedente() {
        givenTournament(1L, "ROLAND GARROS", TournamentCategory.GRAND_SLAM, 22);

        TournamentDto dto = tournamentService.createQualifying(
                1L,
                new QualifyingCreateDto(
                        128,
                        List.of(
                                new RoundPointsDto(1, "Q1", 0),
                                new RoundPointsDto(2, "Q2", 8),
                                new RoundPointsDto(3, "Q3", 16))));

        assertThat(dto.name()).isEqualTo("ROLAND GARROS - Qualifs");
        assertThat(dto.weekNumber()).isEqualTo(21);
        assertThat(dto.qualifying()).isTrue();
        assertThat(savedRoundPoints()).containsExactly(0, 8, 16);
        verify(bracketService).initializeSkeleton(any(Tournament.class), eq(128), eq(3));
    }

    @Test
    void lesQualifsDUnAtp250SeJouentLaMemeSemaine() {
        givenTournament(1L, "DOHA", TournamentCategory.ATP_250, 7);

        TournamentDto dto = tournamentService.createQualifying(
                1L, new QualifyingCreateDto(16, List.of(new RoundPointsDto(1, "Q1", 0))));

        assertThat(dto.weekNumber()).isEqualTo(7);
    }

    @Test
    void unTournoiNAQuUnSeulTableauDeQualifs() {
        givenTournament(1L, "DOHA", TournamentCategory.ATP_250, 7);
        when(tournamentRepository.findByMainTournamentId(1L)).thenReturn(Optional.of(new Tournament()));
        QualifyingCreateDto dto = new QualifyingCreateDto(16, List.of(new RoundPointsDto(1, "Q1", 0)));

        assertThatThrownBy(() -> tournamentService.createQualifying(1L, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void desQualifsNOntPasDeQualifs() {
        givenTournament(1L, "DOHA - Qualifs", TournamentCategory.ATP_250, 7).setQualifying(true);
        QualifyingCreateDto dto = new QualifyingCreateDto(16, List.of(new RoundPointsDto(1, "Q1", 0)));

        assertThatThrownBy(() -> tournamentService.createQualifying(1L, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- suppression ---

    @Test
    void supprimerUnTournoiVideSupprimeAussiSesQualifs() {
        Tournament doha = givenTournament(1L, "DOHA", TournamentCategory.ATP_250, 7);
        Tournament qualifs = tournament(2L, "DOHA - Qualifs", TournamentCategory.ATP_250, 7);
        when(tournamentRepository.findByMainTournamentId(1L)).thenReturn(Optional.of(qualifs));

        tournamentService.delete(1L);

        verify(tournamentRepository).delete(qualifs);
        verify(tournamentRepository).delete(doha);
    }

    @Test
    void impossibleDeSupprimerUnTournoiAvecDesJoueurs() {
        givenTournament(1L, "DOHA", TournamentCategory.ATP_250, 7);
        when(entryRepository.countByTournamentId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> tournamentService.delete(1L)).isInstanceOf(IllegalArgumentException.class);
        verify(tournamentRepository, never()).delete(any());
    }

    @Test
    void impossibleDeSupprimerUnTournoiDontLesQualifsOntDesJoueurs() {
        givenTournament(1L, "DOHA", TournamentCategory.ATP_250, 7);
        Tournament qualifs = tournament(2L, "DOHA - Qualifs", TournamentCategory.ATP_250, 7);
        when(tournamentRepository.findByMainTournamentId(1L)).thenReturn(Optional.of(qualifs));
        when(entryRepository.countByTournamentId(1L)).thenReturn(0L);
        when(entryRepository.countByTournamentId(2L)).thenReturn(4L);

        assertThatThrownBy(() -> tournamentService.delete(1L)).isInstanceOf(IllegalArgumentException.class);
        verify(tournamentRepository, never()).delete(any());
    }

    // --- outils ---

    private List<Integer> savedRoundPoints() {
        ArgumentCaptor<TournamentRound> rounds = ArgumentCaptor.forClass(TournamentRound.class);
        verify(tournamentRoundRepository, atLeastOnce()).save(rounds.capture());
        return rounds.getAllValues().stream().map(TournamentRound::getPoints).toList();
    }

    private Tournament givenTournament(Long id, String name, TournamentCategory category, int week) {
        Tournament t = tournament(id, name, category, week);
        when(tournamentRepository.findById(id)).thenReturn(Optional.of(t));
        return t;
    }

    private static Tournament tournament(Long id, String name, TournamentCategory category, int week) {
        Tournament t = new Tournament();
        t.setId(id);
        t.setName(name);
        t.setCategory(category);
        t.setSeason(2026);
        t.setWeekNumber(week);
        t.setDrawSize(32);
        return t;
    }

    private static TournamentCreateDto create(int drawSize, List<RoundPointsDto> rounds) {
        return new TournamentCreateDto(
                "DOHA", TournamentCategory.ATP_250, 2026, 7, "QATAR", null, drawSize, null, null, null, rounds);
    }

    private static TournamentUpdateDto update(int week, List<RoundPointsDto> rounds, Integer drawSize) {
        return new TournamentUpdateDto(null, week, null, null, null, null, null, rounds, drawSize);
    }
}
