package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.TeamRubber;
import com.charles.tennisresults.domain.TeamTie;
import com.charles.tennisresults.dto.TeamBracketCreateDto;
import com.charles.tennisresults.dto.TeamRubberUpdateDto;
import com.charles.tennisresults.dto.TeamTieCreateDto;
import com.charles.tennisresults.dto.TeamTieDto;
import com.charles.tennisresults.dto.TeamTieUpdateDto;
import com.charles.tennisresults.repository.TeamTieRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamCompetitionServiceTest {

    private static final Long TIE_ID = 1L;

    @Mock
    private TeamTieRepository tieRepository;

    @InjectMocks
    private TeamCompetitionService service;

    @Test
    void creerUneRencontreDePouleUnitedCup() {
        when(tieRepository.maxPosition("UNITED_CUP", 2026, "GROUP")).thenReturn(2);
        when(tieRepository.save(any(TeamTie.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamTieDto dto = service.create(new TeamTieCreateDto(
                "UNITED_CUP", 2026, "GROUP", " a ", " FRANCE ", "ITALIE", "2-4 jan", "Perth", "RAC Arena", "Dur", 3));

        assertThat(dto.groupName()).isEqualTo("A");
        assertThat(dto.team1()).isEqualTo("FRANCE");
        assertThat(dto.position()).isEqualTo(3);
        assertThat(dto.status()).isEqualTo("SCHEDULED");
        assertThat(dto.city()).isEqualTo("Perth");
        assertThat(dto.rubbers()).hasSize(3);
        assertThat(dto.rubbers()).extracting(TeamTieDto.TeamRubberDto::doubles).containsExactly(false, false, true);
    }

    @Test
    void uneRencontreDePouleSansPouleEstRefusee() {
        TeamTieCreateDto dto =
                new TeamTieCreateDto("UNITED_CUP", 2026, "GROUP", " ", "FRANCE", "ITALIE", null, null, null, null, 3);

        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unePhaseInconnueEstRefusee() {
        TeamTieCreateDto dto = new TeamTieCreateDto(
                "DAVIS_CUP", 2026, "FINALS_QF", null, "FRANCE", "ITALIE", null, null, null, null, 5);

        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uneRencontreSeJoueEn3Ou5Matchs() {
        when(tieRepository.maxPosition("DAVIS_CUP", 2026, "QUALIFIERS_R1")).thenReturn(0);
        TeamTieCreateDto dto = new TeamTieCreateDto(
                "DAVIS_CUP", 2026, "QUALIFIERS_R1", null, "FRANCE", "ITALIE", null, null, null, null, 4);

        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creerLeTableauFinalDeLaCoupeDavis() {
        when(tieRepository.findByCompetitionAndSeasonAndStageIn(any(), any(), any()))
                .thenReturn(List.of());
        when(tieRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        List<TeamBracketCreateDto.Quarter> quarters = List.of(
                new TeamBracketCreateDto.Quarter("FRANCE", "ITALIE", "20 nov"),
                new TeamBracketCreateDto.Quarter("ESPAGNE", "ALLEMAGNE", null),
                new TeamBracketCreateDto.Quarter("USA", "CANADA", null),
                new TeamBracketCreateDto.Quarter("ARGENTINE", "SERBIE", null));

        List<TeamTieDto> ties = service.createBracket(
                new TeamBracketCreateDto("DAVIS_CUP", 2026, quarters, "19-24 nov", "Bologne", null, "Dur", 3));

        assertThat(ties)
                .extracting(TeamTieDto::stage)
                .containsExactly(
                        "FINALS_QF", "FINALS_QF", "FINALS_QF", "FINALS_QF", "FINALS_SF", "FINALS_SF", "FINALS_F");
        assertThat(ties.get(0).dates()).isEqualTo("20 nov");
        assertThat(ties.get(1).dates()).isEqualTo("19-24 nov");
        assertThat(ties.get(4).team1Placeholder()).isEqualTo("Vainqueur QF1");
        assertThat(ties.get(6).team2Placeholder()).isEqualTo("Vainqueur SF2");
    }

    @Test
    void leTableauFinalNeSeCreeQuUneFois() {
        when(tieRepository.findByCompetitionAndSeasonAndStageIn(any(), any(), any()))
                .thenReturn(List.of(new TeamTie()));
        TeamBracketCreateDto dto = new TeamBracketCreateDto("UNITED_CUP", 2026, List.of(), null, null, null, null, 3);

        assertThatThrownBy(() -> service.createBracket(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laRencontreEstGagneeDesQuUneEquipeALaMajorite() {
        TeamTie tie = givenTie("FINALS_QF", 1, 3);

        service.updateRubber(TIE_ID, 1, new TeamRubberUpdateDto("Humbert", "Sinner", "6-4 6-4", 1, "COMPLETED"));
        assertThat(tie.getWinner()).isNull();
        assertThat(tie.getStatus()).isEqualTo("SCHEDULED");

        TeamTieDto dto =
                service.updateRubber(TIE_ID, 2, new TeamRubberUpdateDto("Fils", "Musetti", "7-6 6-3", 1, "COMPLETED"));

        assertThat(dto.team1Score()).isEqualTo(2);
        assertThat(dto.team2Score()).isZero();
        assertThat(dto.winner()).isEqualTo(1);
        assertThat(dto.status()).isEqualTo("COMPLETED");
    }

    @Test
    void leVainqueurDUnQuartEstReporteEnDemie() {
        TeamTie quarter = givenTie("FINALS_QF", 2, 3);
        TeamTie semi = new TeamTie();
        when(tieRepository.findByCompetitionAndSeasonAndStageAndPosition("DAVIS_CUP", 2026, "FINALS_SF", 1))
                .thenReturn(Optional.of(semi));

        service.updateRubber(TIE_ID, 1, new TeamRubberUpdateDto(null, null, "4-6 4-6", 2, "COMPLETED"));
        service.updateRubber(TIE_ID, 2, new TeamRubberUpdateDto(null, null, "3-6 3-6", 2, "COMPLETED"));

        assertThat(quarter.getWinner()).isEqualTo(2);
        assertThat(semi.getTeam2()).isEqualTo("ITALIE");
        assertThat(semi.getTeam1()).isNull();
    }

    @Test
    void unMatchJoueSansVainqueurEstRefuse() {
        givenTie("FINALS_QF", 1, 3);
        TeamRubberUpdateDto dto = new TeamRubberUpdateDto(null, null, "6-4", null, "COMPLETED");

        assertThatThrownBy(() -> service.updateRubber(TIE_ID, 1, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unStatutDeMatchInconnuEstRefuse() {
        givenTie("FINALS_QF", 1, 3);
        TeamRubberUpdateDto dto = new TeamRubberUpdateDto(null, null, null, null, "ABANDON");

        assertThatThrownBy(() -> service.updateRubber(TIE_ID, 1, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void corrigerUneRencontreDePoule() {
        TeamTie tie = givenTie("GROUP", 1, 3);

        TeamTieDto dto = service.update(TIE_ID, new TeamTieUpdateDto("FRANCE", "ESPAGNE", " b ", "", "Sydney", "", ""));

        assertThat(dto.team2()).isEqualTo("ESPAGNE");
        assertThat(tie.getGroupName()).isEqualTo("B");
        assertThat(dto.dates()).isNull();
        assertThat(dto.city()).isEqualTo("Sydney");
    }

    @Test
    void lesDeuxEquipesDoiventEtreDifferentes() {
        givenTie("GROUP", 1, 3);
        TeamTieUpdateDto dto = new TeamTieUpdateDto("FRANCE", "FRANCE", "A", null, null, null, null);

        assertThatThrownBy(() -> service.update(TIE_ID, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unQuartNeSeSupprimePasSeul() {
        givenTie("FINALS_QF", 1, 3);

        assertThatThrownBy(() -> service.delete(TIE_ID)).isInstanceOf(IllegalArgumentException.class);
        verify(tieRepository, never()).delete(any());
    }

    @Test
    void uneRencontreDeQualifsSeSupprime() {
        TeamTie tie = givenTie("QUALIFIERS_R1", 1, 5);

        service.delete(TIE_ID);

        verify(tieRepository).delete(tie);
    }

    private TeamTie givenTie(String stage, int position, int rubberCount) {
        TeamTie tie = new TeamTie();
        tie.setId(TIE_ID);
        tie.setCompetition("DAVIS_CUP");
        tie.setSeason(2026);
        tie.setStage(stage);
        tie.setGroupName("A");
        tie.setPosition(position);
        tie.setTeam1("FRANCE");
        tie.setTeam2("ITALIE");
        tie.setStatus("SCHEDULED");
        for (int order = 1; order <= rubberCount; order++) {
            TeamRubber rubber = new TeamRubber();
            rubber.setTie(tie);
            rubber.setRubberOrder(order);
            rubber.setDoubles(order == 3);
            rubber.setStatus("PENDING");
            tie.getRubbers().add(rubber);
        }
        when(tieRepository.findById(TIE_ID)).thenReturn(Optional.of(tie));
        return tie;
    }
}
