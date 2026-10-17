package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.RankingHistoryPointDto;
import com.charles.tennisresults.dto.TournamentStatus;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.RankingSnapshotRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evolution du classement d'un joueur semaine par semaine, reconstituee a
 * partir du classement de l'appli lui-meme (RankingService) - jamais d'une
 * source externe.
 *
 * Le point de la semaine W est le classement "du lundi" de cette semaine :
 * celui calcule avec les seuls tournois des semaines precedentes (comme
 * Entry.rankingAtEntry, voir {@link RankingService#computeRankingBefore}).
 * Recalculer tout le classement pour chaque semaine de l'annee a chaque
 * affichage serait trop lent : chaque semaine est donc calculee une seule
 * fois puis stockee (RankingSnapshotRepository), et effacee - pour etre
 * recalculee a la demande suivante - des qu'un resultat d'une semaine
 * anterieure change ({@link #invalidateFrom}).
 */
@Service
public class RankingSnapshotService {

    private final RankingService rankingService;
    private final RankingSnapshotRepository snapshotRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

    public RankingSnapshotService(
            RankingService rankingService,
            RankingSnapshotRepository snapshotRepository,
            TournamentRepository tournamentRepository,
            MatchRepository matchRepository) {
        this.rankingService = rankingService;
        this.snapshotRepository = snapshotRepository;
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
    }

    /**
     * Classement de ce joueur au debut de chaque semaine de la saison, de la
     * semaine 2 (apres les premiers tournois) jusqu'a la semaine qui suit le
     * dernier tournoi commence. Les semaines manquantes sont calculees d'abord.
     */
    @Transactional
    public List<RankingHistoryPointDto> history(Long playerId, int season) {
        ensureSnapshots(season);
        return snapshotRepository.historyOf(playerId, season);
    }

    // synchronized : deux fiches ouvertes en meme temps ne calculent pas deux
    // fois les memes semaines (l'insertion tolere de toute facon un doublon).
    private synchronized void ensureSnapshots(int season) {
        Integer lastStartedWeek = lastStartedWeek(season);
        if (lastStartedWeek == null) {
            return;
        }
        Set<Integer> computed = snapshotRepository.computedWeeks(season);
        for (int week = 2; week <= lastStartedWeek + 1; week++) {
            if (!computed.contains(week)) {
                snapshotRepository.saveWeek(season, week, rankingService.computeRankingBefore(season, week));
            }
        }
    }

    /** Semaine du dernier tournoi principal commence (qualifs comprises) de la saison, null si aucun. */
    private Integer lastStartedWeek(int season) {
        List<Tournament> mains = tournamentRepository.findBySeason(season).stream()
                .filter(t -> !t.isQualifying())
                .filter(t -> t.getWeekNumber() != null)
                .toList();
        if (mains.isEmpty()) {
            return null;
        }
        TournamentProgress progress = TournamentProgress.compute(tournamentRepository, matchRepository, mains);
        return mains.stream()
                .filter(t -> progress.statusOf(t) != TournamentStatus.NOT_STARTED)
                .map(Tournament::getWeekNumber)
                .max(Integer::compare)
                .orElse(null);
    }

    /**
     * A appeler quand un resultat (ou un tableau) de ce tournoi change : les
     * photos des semaines suivantes sont effacees. Des qualifs comptent pour
     * la semaine de leur tournoi principal ; sans semaine connue, tout est
     * efface (impossible de situer le changement).
     */
    @Transactional
    public void invalidateFrom(Tournament tournament) {
        Tournament main = tournament;
        if (tournament.isQualifying() && tournament.getMainTournamentId() != null) {
            main = tournamentRepository
                    .findById(tournament.getMainTournamentId())
                    .orElse(tournament);
        }
        if (main.getSeason() == null || main.getWeekNumber() == null) {
            snapshotRepository.deleteAll();
        } else {
            snapshotRepository.deleteAfter(main.getSeason(), main.getWeekNumber());
        }
    }

    /** Efface toutes les photos (ex: apres une correction faite directement en base). */
    @Transactional
    public void invalidateAll() {
        snapshotRepository.deleteAll();
    }
}
