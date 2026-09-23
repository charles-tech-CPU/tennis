package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.TeamRubber;
import com.charles.tennisresults.domain.TeamTie;
import com.charles.tennisresults.dto.TeamBracketCreateDto;
import com.charles.tennisresults.dto.TeamRubberUpdateDto;
import com.charles.tennisresults.dto.TeamTieCreateDto;
import com.charles.tennisresults.dto.TeamTieDto;
import com.charles.tennisresults.dto.TeamTieUpdateDto;
import com.charles.tennisresults.repository.TeamTieRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coupe Davis / United Cup : lecture des rencontres d'une saison et saisie des
 * matchs. Le score d'une rencontre (nombre de matchs gagnes) et son vainqueur
 * sont toujours recalcules a partir de ses matchs ; dans une phase a elimination
 * directe, le vainqueur est reporte automatiquement dans la rencontre du tour
 * suivant (meme principe que BracketService pour les tournois).
 */
@Service
public class TeamCompetitionService {

    /** Phase a elimination directe -> phase suivante. */
    private static final Map<String, String> NEXT_STAGE = Map.of(
            "FINALS_QF", "FINALS_SF",
            "FINALS_SF", "FINALS_F",
            "QF", "SF",
            "SF", "F");

    private static final Set<String> RUBBER_STATUSES = Set.of("PENDING", "COMPLETED", "NOT_PLAYED");

    /** Phases ou l'on ajoute les rencontres une par une. */
    private static final Map<String, Set<String>> LIST_STAGES = Map.of(
            "DAVIS_CUP", Set.of("QUALIFIERS_R1", "QUALIFIERS_R2", "WORLD_GROUP_I_PO"),
            "UNITED_CUP", Set.of("GROUP"));

    /** Tableau final (quarts, demies, finale), cree d'un bloc. */
    private static final Map<String, List<String>> BRACKET_STAGES = Map.of(
            "DAVIS_CUP", List.of("FINALS_QF", "FINALS_SF", "FINALS_F"),
            "UNITED_CUP", List.of("QF", "SF", "F"));

    private final TeamTieRepository tieRepository;

    public TeamCompetitionService(TeamTieRepository tieRepository) {
        this.tieRepository = tieRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamTieDto> findBySeason(String competition, Integer season) {
        return tieRepository.findByCompetitionAndSeasonOrderByStageAscPositionAsc(competition, season).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> findSeasons(String competition) {
        return tieRepository.findSeasons(competition);
    }

    @Transactional
    public TeamTieDto create(TeamTieCreateDto dto) {
        Set<String> stages = LIST_STAGES.get(dto.competition());
        if (stages == null || !stages.contains(dto.stage())) {
            throw new IllegalArgumentException(
                    "Phase " + dto.stage() + " inconnue (ou tableau final : a creer d'un bloc).");
        }
        String groupName = blankToNull(dto.groupName());
        if ("GROUP".equals(dto.stage()) && groupName == null) {
            throw new IllegalArgumentException("Indiquez la poule de la rencontre.");
        }
        checkTeams(dto.team1(), dto.team2());
        TeamTie tie = newTie(
                dto.competition(),
                dto.season(),
                dto.stage(),
                tieRepository.maxPosition(dto.competition(), dto.season(), dto.stage()) + 1,
                dto.dates(),
                dto.city(),
                dto.venue(),
                dto.surface(),
                dto.rubberCount());
        tie.setGroupName(groupName == null ? null : groupName.toUpperCase());
        tie.setTeam1(dto.team1().trim());
        tie.setTeam2(dto.team2().trim());
        return toDto(tieRepository.save(tie));
    }

    @Transactional
    public List<TeamTieDto> createBracket(TeamBracketCreateDto dto) {
        List<String> stages = BRACKET_STAGES.get(dto.competition());
        if (stages == null) {
            throw new IllegalArgumentException("Competition inconnue: " + dto.competition());
        }
        if (!tieRepository
                .findByCompetitionAndSeasonAndStageIn(dto.competition(), dto.season(), stages)
                .isEmpty()) {
            throw new IllegalArgumentException(
                    "Le tableau final " + dto.season() + " existe deja (supprimez-le d'abord).");
        }
        for (TeamBracketCreateDto.Quarter q : dto.quarters()) {
            checkTeams(q.team1(), q.team2());
        }
        List<TeamTie> created = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TeamBracketCreateDto.Quarter q = dto.quarters().get(i);
            TeamTie tie = newTie(
                    dto.competition(),
                    dto.season(),
                    stages.get(0),
                    i + 1,
                    blankToNull(q.dates()) != null ? q.dates() : dto.dates(),
                    dto.city(),
                    dto.venue(),
                    dto.surface(),
                    dto.rubberCount());
            tie.setTeam1(q.team1().trim());
            tie.setTeam2(q.team2().trim());
            created.add(tie);
        }
        for (int i = 1; i <= 2; i++) {
            TeamTie tie = newTie(
                    dto.competition(),
                    dto.season(),
                    stages.get(1),
                    i,
                    dto.dates(),
                    dto.city(),
                    dto.venue(),
                    dto.surface(),
                    dto.rubberCount());
            tie.setTeam1Placeholder("Vainqueur QF" + (2 * i - 1));
            tie.setTeam2Placeholder("Vainqueur QF" + (2 * i));
            created.add(tie);
        }
        TeamTie finalTie = newTie(
                dto.competition(),
                dto.season(),
                stages.get(2),
                1,
                dto.dates(),
                dto.city(),
                dto.venue(),
                dto.surface(),
                dto.rubberCount());
        finalTie.setTeam1Placeholder("Vainqueur SF1");
        finalTie.setTeam2Placeholder("Vainqueur SF2");
        created.add(finalTie);
        return tieRepository.saveAll(created).stream().map(this::toDto).toList();
    }

    @Transactional
    public TeamTieDto update(Long tieId, TeamTieUpdateDto dto) {
        TeamTie tie = getOrThrow(tieId);
        String team1 = blankToNull(dto.team1());
        String team2 = blankToNull(dto.team2());
        if (team1 != null && team1.equals(team2)) {
            throw new IllegalArgumentException("Les deux equipes doivent etre differentes.");
        }
        if ("GROUP".equals(tie.getStage()) && blankToNull(dto.groupName()) == null) {
            throw new IllegalArgumentException("Indiquez la poule de la rencontre.");
        }
        tie.setTeam1(team1);
        tie.setTeam2(team2);
        if ("GROUP".equals(tie.getStage())) {
            tie.setGroupName(dto.groupName().trim().toUpperCase());
        }
        tie.setDates(blankToNull(dto.dates()));
        tie.setCity(blankToNull(dto.city()));
        tie.setVenue(blankToNull(dto.venue()));
        tie.setSurface(blankToNull(dto.surface()));
        // Un pays corrige dans un quart doit aussi l'etre la ou il a deja ete reporte.
        propagateWinner(tie);
        return toDto(tie);
    }

    /** Supprime une rencontre d'une phase "a liste" (un quart seul ne se supprime pas). */
    @Transactional
    public void delete(Long tieId) {
        TeamTie tie = getOrThrow(tieId);
        if (BRACKET_STAGES.get(tie.getCompetition()).contains(tie.getStage())) {
            throw new IllegalArgumentException("Rencontre du tableau final : supprimez le tableau entier.");
        }
        tieRepository.delete(tie);
    }

    @Transactional
    public void deleteBracket(String competition, Integer season) {
        List<String> stages = BRACKET_STAGES.get(competition);
        if (stages == null) {
            throw new IllegalArgumentException("Competition inconnue: " + competition);
        }
        tieRepository.deleteAll(tieRepository.findByCompetitionAndSeasonAndStageIn(competition, season, stages));
    }

    private TeamTie newTie(
            String competition,
            Integer season,
            String stage,
            int position,
            String dates,
            String city,
            String venue,
            String surface,
            Integer rubberCount) {
        if (rubberCount == null || (rubberCount != 3 && rubberCount != 5)) {
            throw new IllegalArgumentException("Une rencontre se joue en 3 ou 5 matchs.");
        }
        TeamTie tie = new TeamTie();
        tie.setCompetition(competition);
        tie.setSeason(season);
        tie.setStage(stage);
        tie.setPosition(position);
        tie.setStatus("SCHEDULED");
        tie.setDates(blankToNull(dates));
        tie.setCity(blankToNull(city));
        tie.setVenue(blankToNull(venue));
        tie.setSurface(blankToNull(surface));
        // Matchs vides a saisir ; le 3e est toujours le double (Coupe Davis :
        // 2 simples, double, 2 simples ; United Cup : 2 simples, double mixte).
        for (int order = 1; order <= rubberCount; order++) {
            TeamRubber r = new TeamRubber();
            r.setTie(tie);
            r.setRubberOrder(order);
            r.setDoubles(order == 3);
            r.setStatus("PENDING");
            tie.getRubbers().add(r);
        }
        return tie;
    }

    private static void checkTeams(String team1, String team2) {
        if (blankToNull(team1) == null || blankToNull(team2) == null) {
            throw new IllegalArgumentException("Choisissez les deux equipes.");
        }
        if (team1.trim().equals(team2.trim())) {
            throw new IllegalArgumentException("Les deux equipes doivent etre differentes (" + team1 + ").");
        }
    }

    private TeamTie getOrThrow(Long tieId) {
        return tieRepository
                .findById(tieId)
                .orElseThrow(() -> new EntityNotFoundException("Rencontre introuvable: " + tieId));
    }

    @Transactional
    public TeamTieDto updateRubber(Long tieId, Integer rubberOrder, TeamRubberUpdateDto dto) {
        TeamTie tie = getOrThrow(tieId);
        TeamRubber rubber = tie.getRubbers().stream()
                .filter(r -> r.getRubberOrder().equals(rubberOrder))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Match " + rubberOrder + " introuvable dans la rencontre " + tieId));

        if (!RUBBER_STATUSES.contains(dto.status())) {
            throw new IllegalArgumentException("Statut inconnu: " + dto.status());
        }
        boolean completed = "COMPLETED".equals(dto.status());
        if (completed && (dto.winner() == null || (dto.winner() != 1 && dto.winner() != 2))) {
            throw new IllegalArgumentException("Un match joue doit avoir un vainqueur (equipe 1 ou 2).");
        }

        rubber.setTeam1Players(blankToNull(dto.team1Players()));
        rubber.setTeam2Players(blankToNull(dto.team2Players()));
        rubber.setScore(blankToNull(dto.score()));
        rubber.setStatus(dto.status());
        rubber.setWinner(completed ? dto.winner() : null);

        recomputeTie(tie);
        propagateWinner(tie);
        return toDto(tie);
    }

    /**
     * Score = matchs gagnes par chaque equipe ; la rencontre est decidee des
     * qu'une equipe atteint la majorite des matchs prevus (3 sur 5, 2 sur 3) -
     * les matchs restants sont alors sans enjeu.
     */
    private void recomputeTie(TeamTie tie) {
        int s1 = 0;
        int s2 = 0;
        for (TeamRubber r : tie.getRubbers()) {
            if ("COMPLETED".equals(r.getStatus()) && r.getWinner() != null) {
                if (r.getWinner() == 1) s1++;
                else s2++;
            }
        }
        int needed = tie.getRubbers().size() / 2 + 1;
        tie.setTeam1Score(s1);
        tie.setTeam2Score(s2);
        tie.setWinner(s1 >= needed ? Integer.valueOf(1) : s2 >= needed ? Integer.valueOf(2) : null);
        tie.setStatus(tie.getWinner() != null ? "COMPLETED" : "SCHEDULED");
    }

    private void propagateWinner(TeamTie tie) {
        String nextStage = NEXT_STAGE.get(tie.getStage());
        if (nextStage == null) {
            return;
        }
        int nextPosition = (tie.getPosition() + 1) / 2;
        tieRepository
                .findByCompetitionAndSeasonAndStageAndPosition(
                        tie.getCompetition(), tie.getSeason(), nextStage, nextPosition)
                .ifPresent(next -> {
                    String team =
                            tie.getWinner() == null ? null : tie.getWinner() == 1 ? tie.getTeam1() : tie.getTeam2();
                    if (tie.getPosition() % 2 == 1) {
                        next.setTeam1(team);
                    } else {
                        next.setTeam2(team);
                    }
                });
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private TeamTieDto toDto(TeamTie t) {
        return new TeamTieDto(
                t.getId(),
                t.getCompetition(),
                t.getSeason(),
                t.getStage(),
                t.getGroupName(),
                t.getPosition(),
                t.getTeam1(),
                t.getTeam2(),
                t.getTeam1Placeholder(),
                t.getTeam2Placeholder(),
                t.getTeam1Score(),
                t.getTeam2Score(),
                t.getWinner(),
                t.getStatus(),
                t.getDates(),
                t.getCity(),
                t.getVenue(),
                t.getSurface(),
                t.getRubbers().stream()
                        .map(r -> new TeamTieDto.TeamRubberDto(
                                r.getId(),
                                r.getRubberOrder(),
                                r.isDoubles(),
                                r.getTeam1Players(),
                                r.getTeam2Players(),
                                r.getScore(),
                                r.getWinner(),
                                r.getStatus()))
                        .toList());
    }
}
