package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classement de chaque joueur d'un tableau au debut du tournoi, comme celui
 * que l'ATP affiche a cote des joueurs : classement de l'appli au debut de la
 * semaine du tournoi (tournois des semaines precedentes uniquement, voir
 * {@link RankingService#computeRankingBefore}). Fige au premier score saisi,
 * aucun historique hebdomadaire stocke.
 */
@Service
public class EntryRankingService {

    private final RankingService rankingService;
    private final EntryRepository entryRepository;
    private final TournamentRepository tournamentRepository;

    public EntryRankingService(
            RankingService rankingService, EntryRepository entryRepository, TournamentRepository tournamentRepository) {
        this.rankingService = rankingService;
        this.entryRepository = entryRepository;
        this.tournamentRepository = tournamentRepository;
    }

    /** Semaine de reference (saison, semaine) d'un tournoi : celle de son tableau principal pour des qualifs. */
    private record Cutoff(Integer season, Integer week) {}

    /**
     * A appeler avant d'enregistrer le premier resultat du tournoi. Sans
     * effet si le tournoi est deja fige.
     */
    @Transactional
    public void freezeIfNeeded(Tournament tournament) {
        if (tournament.isRankingsFrozen()) {
            return;
        }
        Map<Long, Integer> positions = positions(cutoff(tournament, Map.of()));
        for (Entry entry : entryRepository.findByTournamentId(tournament.getId())) {
            fill(entry, positions);
        }
        tournament.setRankingsFrozen(true);
    }

    /**
     * Rattrapage : remplit le classement des joueurs encore sans classement
     * dans tous les tournois deja figes (demarres). Un seul calcul du
     * classement par semaine de reference. Renvoie le nombre d'entrees remplies.
     */
    @Transactional
    public int backfillFrozenTournaments() {
        List<Tournament> all = tournamentRepository.findAll();
        Map<Long, Tournament> byId = all.stream().collect(Collectors.toMap(Tournament::getId, Function.identity()));
        Map<Long, Tournament> frozen = all.stream()
                .filter(Tournament::isRankingsFrozen)
                .collect(Collectors.toMap(Tournament::getId, Function.identity()));
        if (frozen.isEmpty()) {
            return 0;
        }

        Map<Cutoff, List<Entry>> entriesByCutoff = new HashMap<>();
        for (Entry entry : entryRepository.findByTournament_IdIn(new ArrayList<>(frozen.keySet()))) {
            if (entry.getPlayer() != null && entry.getRankingAtEntry() == null) {
                entriesByCutoff
                        .computeIfAbsent(cutoff(entry.getTournament(), byId), k -> new ArrayList<>())
                        .add(entry);
            }
        }

        int filled = 0;
        for (Map.Entry<Cutoff, List<Entry>> group : entriesByCutoff.entrySet()) {
            Map<Long, Integer> positions = positions(group.getKey());
            for (Entry entry : group.getValue()) {
                if (fill(entry, positions)) {
                    filled++;
                }
            }
        }
        return filled;
    }

    private Cutoff cutoff(Tournament t, Map<Long, Tournament> byId) {
        Tournament main = t;
        if (t.isQualifying() && t.getMainTournamentId() != null) {
            main = byId.containsKey(t.getMainTournamentId())
                    ? byId.get(t.getMainTournamentId())
                    : tournamentRepository.findById(t.getMainTournamentId()).orElse(t);
        }
        return new Cutoff(main.getSeason(), main.getWeekNumber());
    }

    /** Position au classement (1 = premier) par joueur, au debut de la semaine de reference. */
    private Map<Long, Integer> positions(Cutoff cutoff) {
        // Sans semaine connue, impossible de situer le tournoi : classement du moment.
        List<RankingRowDto> ranking = cutoff.season() == null || cutoff.week() == null
                ? rankingService.computeRanking()
                : rankingService.computeRankingBefore(cutoff.season(), cutoff.week());
        Map<Long, Integer> positions = new HashMap<>();
        for (int i = 0; i < ranking.size(); i++) {
            positions.put(ranking.get(i).playerId(), i + 1);
        }
        return positions;
    }

    /** Renseigne le classement de l'entree ; true si le joueur etait classe. */
    private static boolean fill(Entry entry, Map<Long, Integer> positions) {
        if (entry.getPlayer() == null) {
            return false;
        }
        entry.setRankingAtEntry(positions.get(entry.getPlayer().getId()));
        return Objects.nonNull(entry.getRankingAtEntry());
    }
}
