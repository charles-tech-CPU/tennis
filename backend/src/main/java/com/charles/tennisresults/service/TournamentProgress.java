package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.TournamentStatus;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calcule le statut "pas commence / en cours / termine" d'un tournoi, en
 * tenant compte a la fois de son tableau principal ET de ses qualifs - les
 * deux partagent le meme statut (les qualifs sont juste le debut du meme
 * evenement) : si les qualifs ont commence, le tournoi est "en cours" meme
 * si le tableau principal n'a pas encore de joueurs places.
 *
 * Utilise a la fois pour colorer la liste des tournois et pour reperer, dans
 * le classement, les joueurs encore en jeu dans un tournoi non termine.
 */
public final class TournamentProgress {

    /** Angle d'or (en degres) : espace au mieux N teintes HSL distinctes quel que soit N. */
    private static final int HUE_STEP = 137;

    private final Map<Long, List<Match>> decidedByTournamentId;
    private final Map<Long, Tournament> qualifyingByMainId;

    public static TournamentProgress compute(TournamentRepository tournamentRepository,
                                              MatchRepository matchRepository,
                                              List<Tournament> mains) {
        List<Tournament> allTournaments = tournamentRepository.findAll();
        Map<Long, Tournament> qualifyingByMainId = allTournaments.stream()
                .filter(Tournament::isQualifying)
                .filter(t -> t.getMainTournamentId() != null)
                .collect(Collectors.toMap(Tournament::getMainTournamentId, t -> t, (a, b) -> a));

        List<Long> relevantIds = new ArrayList<>(mains.stream().map(Tournament::getId).toList());
        qualifyingByMainId.values().forEach(q -> relevantIds.add(q.getId()));

        List<Match> decided = matchRepository.findByTournament_IdInAndStatusIn(
                relevantIds, List.of(MatchStatus.COMPLETED, MatchStatus.BYE));
        Map<Long, List<Match>> decidedByTournamentId = decided.stream()
                .collect(Collectors.groupingBy(m -> m.getTournament().getId()));

        return new TournamentProgress(decidedByTournamentId, qualifyingByMainId);
    }

    private TournamentProgress(Map<Long, List<Match>> decidedByTournamentId, Map<Long, Tournament> qualifyingByMainId) {
        this.decidedByTournamentId = decidedByTournamentId;
        this.qualifyingByMainId = qualifyingByMainId;
    }

    public TournamentStatus statusOf(Tournament main) {
        List<Match> ownDecided = decidedByTournamentId.getOrDefault(main.getId(), List.of());
        Tournament qualifying = qualifyingByMainId.get(main.getId());
        List<Match> qualifyingDecided = qualifying != null
                ? decidedByTournamentId.getOrDefault(qualifying.getId(), List.of())
                : List.of();

        if (ownDecided.isEmpty() && qualifyingDecided.isEmpty()) {
            return TournamentStatus.NOT_STARTED;
        }
        int totalRounds = RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(main.getDrawSize()));
        boolean finalDecided = ownDecided.stream().anyMatch(m -> m.getRoundOrder() == totalRounds);
        return finalDecided ? TournamentStatus.COMPLETED : TournamentStatus.IN_PROGRESS;
    }

    /**
     * Une teinte HSL stable par tournoi en cours (0-359, espacees via l'angle
     * d'or), triees par id pour etre identiques partout ou elles sont
     * utilisees (liste des tournois, classement) - c'est bien la MEME couleur
     * pour un tournoi donne, ou qu'on la regarde.
     */
    public Map<Long, Integer> hueByTournamentId(List<Tournament> mains) {
        List<Tournament> inProgress = mains.stream()
                .filter(t -> statusOf(t) == TournamentStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(Tournament::getId))
                .toList();
        Map<Long, Integer> hues = new HashMap<>();
        for (int i = 0; i < inProgress.size(); i++) {
            hues.put(inProgress.get(i).getId(), (i * HUE_STEP) % 360);
        }
        return hues;
    }
}
