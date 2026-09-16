package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.*;
import com.charles.tennisresults.dto.LiveTournamentDto;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.dto.TournamentPointsDto;
import com.charles.tennisresults.dto.TournamentStatus;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcule automatiquement le classement glissant de chaque joueur, en
 * reproduisant la logique de la formule Excel de Charles :
 *   total = (4 Grand Chelem + ATP Finals + 8 des 9 Masters 1000, hors Monte-Carlo)
 *         + (somme des 5 meilleurs "autres" tournois)
 *         + max(points a Monte-Carlo, 6e meilleur "autre" tournoi)
 *
 * Le classement n'est plus fige par saison (annee civile) : il est glissant,
 * comme le vrai classement ATP sur 52 semaines. Pour chaque tournoi RECURRENT
 * (identifie par sa case obligatoire - Grand Chelem/Masters 1000 - ou par son
 * nom + numero de semaine ATP pour les autres), seule l'edition (saison) la
 * plus recente qui a REELLEMENT commence (au moins un match COMPLETED/BYE,
 * tableau principal ou qualifs) compte - l'edition de l'annee precedente est
 * alors automatiquement exclue. Tant que l'edition de la nouvelle saison n'a
 * pas commence, celle de l'an dernier reste comptee. Voir
 * {@link #activeTournamentIds} : identifier par le NOM seul aurait ete faux
 * (plusieurs tournois differents peuvent partager un nom, ex. "MADRID" ATP75
 * une semaine et Madrid Masters 1000 une autre, ou "NOTTINGHAM" deux fois la
 * meme saison a des semaines differentes) - la case obligatoire (stable d'une
 * annee sur l'autre meme si la semaine calendaire bouge un peu, ex. Shanghai)
 * est le seul identifiant fiable pour les 14 cases, le nom+semaine sert de
 * repli pour tout le reste.
 *
 * Seuls les matchs COMPLETED/BYE comptent - jamais de resultat devine. Mais un
 * joueur qui a deja gagne son dernier tour joue (tournoi encore en cours) est
 * credite EN DIRECT du minimum garanti (ce qu'il toucherait au pire si le tour
 * suivant, pas encore joue, tournait mal) : ce montant est deja acquis, seul
 * son detail exact (jusqu'ou il ira) reste incertain. Ce joueur est alors
 * marque "encore en jeu" (liveTournaments) pour etre repere/colore dans le
 * classement - les qualifs partagent la couleur/le statut de leur tournoi
 * principal (memes joueurs, meme evenement).
 */
@Service
public class RankingService {

    private static final Set<MandatorySlot> MANDATORY_EXCLUDING_MC = EnumSet.complementOf(
            EnumSet.of(MandatorySlot.MONTE_CARLO));

    private final EntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final TournamentRoundRepository tournamentRoundRepository;
    private final TournamentRepository tournamentRepository;

    public RankingService(EntryRepository entryRepository, MatchRepository matchRepository,
                           TournamentRoundRepository tournamentRoundRepository,
                           TournamentRepository tournamentRepository) {
        this.entryRepository = entryRepository;
        this.matchRepository = matchRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.tournamentRepository = tournamentRepository;
    }

    private record EntryOutcome(int points, boolean stillAlive) {
    }

    /**
     * Parmi toutes les editions (saisons) d'un meme tournoi RECURRENT, ne
     * retient que celle qui compte reellement dans le classement glissant :
     * la plus recente a avoir commence, sinon (si elle n'a pas encore
     * commence) la precedente.
     *
     * Identite d'un tournoi recurrent : sa case obligatoire (
     * {@link MandatorySlot}) si elle en a une - stable d'une saison a l'autre
     * meme si la semaine calendaire bouge legerement (ex. Shanghai) - sinon
     * son nom + son numero de semaine ATP (deux tournois differents peuvent
     * partager un nom sans etre le meme evenement, ex. "MADRID" ATP75 semaine
     * 14 vs Madrid Masters 1000 semaine 17 ; ou le meme nom peut designer deux
     * evenements distincts la meme saison a des semaines differentes, ex.
     * "NOTTINGHAM" ATP50 semaine 1 puis ATP125 semaine 25 - le numero de
     * semaine les distingue). Bug trouve par Charles (2026-09-16) : grouper
     * par semaine seule regroupait a tort tous les tournois SIMULTANES d'une
     * meme semaine (5 a 13 tournois differents chaque semaine en temps normal)
     * et n'en gardait qu'un seul, faisant disparaitre la plupart des joueurs
     * du classement.
     */
    private Set<Long> activeTournamentIds(List<Tournament> allMains, TournamentProgress progress) {
        Map<String, List<Tournament>> byIdentity = new HashMap<>();
        for (Tournament t : allMains) {
            String key = t.getMandatorySlot() != null
                    ? "SLOT:" + t.getMandatorySlot()
                    : "NAME_WEEK:" + t.getName().trim().toUpperCase() + "@"
                            + (t.getWeekNumber() != null ? t.getWeekNumber() : "id" + t.getId());
            byIdentity.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        Set<Long> active = new HashSet<>();
        for (List<Tournament> group : byIdentity.values()) {
            List<Tournament> byRecentSeasonFirst = group.stream()
                    .sorted(Comparator.comparing(Tournament::getSeason).reversed())
                    .toList();
            Tournament chosen = byRecentSeasonFirst.stream()
                    .filter(t -> progress.statusOf(t) != TournamentStatus.NOT_STARTED)
                    .findFirst()
                    .orElse(byRecentSeasonFirst.get(0));
            active.add(chosen.getId());
        }
        return active;
    }

    @Transactional(readOnly = true)
    public List<RankingRowDto> computeRanking() {
        Map<Long, Tournament> tournamentsById = tournamentRepository.findAll().stream()
                .collect(Collectors.toMap(Tournament::getId, t -> t));

        List<Tournament> allMains = tournamentsById.values().stream().filter(t -> !t.isQualifying()).toList();
        TournamentProgress progress = TournamentProgress.compute(tournamentRepository, matchRepository, allMains);
        Map<Long, Integer> hueByTournamentId = progress.hueByTournamentId(allMains);

        Set<Long> activeTournamentIds = activeTournamentIds(allMains, progress);

        List<Entry> entries = entryRepository.findByPlayerIsNotNull().stream()
                .filter(entry -> {
                    Tournament t = entry.getTournament();
                    Tournament effective = t.isQualifying()
                            ? tournamentsById.getOrDefault(t.getMainTournamentId(), t)
                            : t;
                    return activeTournamentIds.contains(effective.getId());
                })
                .toList();

        Map<Player, List<Entry>> byPlayer = entries.stream()
                .collect(Collectors.groupingBy(Entry::getPlayer));

        List<RankingRowDto> rows = new ArrayList<>();
        for (Map.Entry<Player, List<Entry>> e : byPlayer.entrySet()) {
            Player player = e.getKey();

            // Fusionne les points d'un tournoi principal et de ses qualifs (meme
            // tournoi pour le classement, comme dans le fichier Excel).
            Map<Long, Integer> pointsByTournamentId = new LinkedHashMap<>();
            Map<Long, Tournament> tournamentByKey = new HashMap<>();
            Map<Long, LiveTournamentDto> liveByTournamentId = new LinkedHashMap<>();

            for (Entry entry : e.getValue()) {
                EntryOutcome outcome = pointsEarned(entry);
                Tournament t = entry.getTournament();
                Tournament effective = t.isQualifying()
                        ? tournamentsById.getOrDefault(t.getMainTournamentId(), t)
                        : t;

                if (outcome.points() > 0) {
                    pointsByTournamentId.merge(effective.getId(), outcome.points(), Integer::sum);
                    tournamentByKey.putIfAbsent(effective.getId(), effective);
                }
                if (outcome.stillAlive() && progress.statusOf(effective) == TournamentStatus.IN_PROGRESS) {
                    Integer hue = hueByTournamentId.get(effective.getId());
                    if (hue != null) {
                        liveByTournamentId.putIfAbsent(effective.getId(),
                                new LiveTournamentDto(effective.getId(), effective.getName(), hue));
                    }
                }
            }

            Map<MandatorySlot, TournamentPointsDto> mandatorySlots = new EnumMap<>(MandatorySlot.class);
            TournamentPointsDto monteCarlo = null;
            List<TournamentPointsDto> othersAll = new ArrayList<>();

            for (Map.Entry<Long, Integer> pe : pointsByTournamentId.entrySet()) {
                Tournament tournament = tournamentByKey.get(pe.getKey());
                TournamentPointsDto dto = new TournamentPointsDto(tournament.getId(), tournament.getName(), pe.getValue());
                MandatorySlot slot = tournament.getMandatorySlot();
                if (slot == MandatorySlot.MONTE_CARLO) {
                    monteCarlo = dto;
                } else if (slot != null) {
                    mandatorySlots.put(slot, dto);
                } else {
                    othersAll.add(dto);
                }
            }

            othersAll.sort(Comparator.comparingInt(TournamentPointsDto::points).reversed());
            int mandatoryTotal = mandatorySlots.values().stream().mapToInt(TournamentPointsDto::points).sum();
            List<TournamentPointsDto> bestOthers = othersAll.stream().limit(5).toList();
            int best5 = bestOthers.stream().mapToInt(TournamentPointsDto::points).sum();
            TournamentPointsDto sixth = othersAll.size() > 5 ? othersAll.get(5) : null;

            int monteCarloPts = monteCarlo != null ? monteCarlo.points() : 0;
            int sixthPts = sixth != null ? sixth.points() : 0;
            TournamentPointsDto replacement = monteCarloPts >= sixthPts ? monteCarlo : sixth;
            int replacementValue = Math.max(monteCarloPts, sixthPts);
            int total = mandatoryTotal + best5 + replacementValue;
            if (total == 0) {
                continue; // aucun resultat acte encore comptabilisable : inutile dans le classement
            }

            List<TournamentPointsDto> nonCounted = new ArrayList<>();
            if (othersAll.size() > 6) {
                nonCounted.addAll(othersAll.subList(6, othersAll.size()));
            }
            if (monteCarlo != null && monteCarlo != replacement) {
                nonCounted.add(monteCarlo);
            }
            if (sixth != null && sixth != replacement) {
                nonCounted.add(sixth);
            }

            rows.add(new RankingRowDto(player.getId(), player.getLastName(), player.getFirstName(),
                    player.getNationality(), mandatorySlots, monteCarlo, bestOthers, replacement, nonCounted,
                    mandatoryTotal, best5, replacementValue, total, new ArrayList<>(liveByTournamentId.values())));
        }

        rows.sort(Comparator.comparingInt(RankingRowDto::total).reversed());
        return rows;
    }

    /**
     * Resultat (points + encore en jeu ou non) de cette entree, uniquement
     * d'apres des matchs decides - jamais de resultat devine. "Encore en jeu"
     * = pas encore elimine (n'a pas perdu) et le tournoi n'est pas fini pour
     * lui ; dans ce cas les points refletent le minimum garanti par son
     * dernier tour gagne (le tour suivant, pas encore joue, reste incertain
     * mais ne peut plus lui faire perdre ce qu'il a deja gagne).
     */
    private EntryOutcome pointsEarned(Entry entry) {
        Tournament tournament = entry.getTournament();
        Map<Integer, Integer> pointsByRound = tournamentRoundRepository
                .findByTournamentIdOrderByRoundOrderAsc(tournament.getId())
                .stream()
                .collect(Collectors.toMap(TournamentRound::getRoundOrder, TournamentRound::getPoints));

        // Un tableau de qualifications n'a pas de "finale" unique : plusieurs
        // groupes independants produisent chacun un qualifie au dernier tour
        // configure (pointsByRound.size()), contrairement au tableau principal ou
        // totalRounds se deduit de la taille du tableau (une seule finale).
        int totalRounds = tournament.isQualifying()
                ? pointsByRound.size()
                : (tournament.getDrawSize() == null ? 0
                        : RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(tournament.getDrawSize())));
        if (totalRounds == 0) {
            return new EntryOutcome(0, false);
        }

        List<Match> matches = matchRepository.findByEntry1_IdOrEntry2_Id(entry.getId(), entry.getId());

        Match deepest = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED || m.getStatus() == MatchStatus.BYE)
                .max(Comparator.comparingInt(Match::getRoundOrder))
                .orElse(null);

        if (deepest == null) {
            return new EntryOutcome(0, true); // pas encore de resultat, mais pas elimine
        }

        boolean won = deepest.getWinnerEntry() != null && deepest.getWinnerEntry().getId().equals(entry.getId());
        int r = deepest.getRoundOrder();

        if (won) {
            if (r == totalRounds) {
                if (isGrandSlamQualifying(tournament)) {
                    return new EntryOutcome(GS_QUALIFYING_QUALIFIED_POINTS, false); // qualifie pour le tableau principal
                }
                return new EntryOutcome(pointsByRound.getOrDefault(totalRounds, 0), false); // champion (ou qualifie)
            }
            // Encore en jeu : credite du minimum garanti, mais uniquement d'apres
            // le dernier tour REELLEMENT gagne sur le court (COMPLETED) - un bye
            // (BYE, uniquement possible au 1er tour) ne rapporte aucun point et
            // ne declenche aucune garantie tant qu'aucun vrai match n'a ete gagne.
            Match deepestRealWin = matches.stream()
                    .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                    .filter(m -> m.getWinnerEntry() != null && m.getWinnerEntry().getId().equals(entry.getId()))
                    .max(Comparator.comparingInt(Match::getRoundOrder))
                    .orElse(null);
            int guaranteed = deepestRealWin != null
                    ? eliminationValue(tournament, pointsByRound, deepestRealWin.getRoundOrder() + 1, totalRounds)
                    : 0;
            return new EntryOutcome(guaranteed, true);
        } else {
            return new EntryOutcome(eliminationValue(tournament, pointsByRound, r, totalRounds), false);
        }
    }

    // Bareme officiel ATP des qualifications de Grand Chelem (3 tours, valeurs
    // fixes) - impose ici plutot que laisse a la merci d'un bareme de qualifs
    // mal saisi tournoi par tournoi (Charles, 2026-09-15).
    private static final int GS_QUALIFYING_Q2_POINTS = 8;
    private static final int GS_QUALIFYING_Q3_POINTS = 16;
    private static final int GS_QUALIFYING_QUALIFIED_POINTS = 25;

    private boolean isGrandSlamQualifying(Tournament tournament) {
        return tournament.isQualifying() && tournament.getCategory() == TournamentCategory.GRAND_SLAM;
    }

    /** Points pour une elimination (actee ou hypothetique) au tour `round`. */
    private int eliminationValue(Tournament tournament, Map<Integer, Integer> pointsByRound, int round, int totalRounds) {
        // Un elimine au 1er tour des qualifs (Q1) touche toujours 0, quel que
        // soit le bareme configure pour ce tour - meme regle que le 1er tour
        // du tableau principal (R32 = 0), mais imposee ici plutot que laissee
        // a la merci d'un bareme de qualifs mal saisi (Charles, 2026-09-15).
        if (tournament.isQualifying() && round == 1) {
            return 0;
        }
        if (isGrandSlamQualifying(tournament)) {
            if (round == 2) {
                return GS_QUALIFYING_Q2_POINTS;
            }
            if (round == 3) {
                return GS_QUALIFYING_Q3_POINTS;
            }
        }
        if (round == totalRounds) {
            return tournament.getRunnerUpPoints() != null
                    ? tournament.getRunnerUpPoints()
                    : pointsByRound.getOrDefault(totalRounds - 1, 0);
        }
        return pointsByRound.getOrDefault(round, 0);
    }
}
