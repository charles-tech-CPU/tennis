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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcule automatiquement le classement glissant de chaque joueur, en
 * reproduisant la logique de la formule Excel de Charles :
 *   total = (4 Grand Chelem + 8 des 9 Masters 1000, hors Monte-Carlo)
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
 *
 * L'ATP Finals ne compte PAS dans le total (Charles, 2026-09-25) : ce n'est
 * pas une case a remplir mais le tournoi des qualifies de fin de saison. Ses
 * points eventuels sont renvoyes a part ({@link RankingRowDto#atpFinals()}),
 * et aucun resultat excedentaire n'y est recycle.
 */
@Service
public class RankingService {

    private static final Set<MandatorySlot> MANDATORY_EXCLUDING_MC =
            EnumSet.complementOf(EnumSet.of(MandatorySlot.MONTE_CARLO, MandatorySlot.ATP_FINALS));

    private final EntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final TournamentRoundRepository tournamentRoundRepository;
    private final TournamentRepository tournamentRepository;

    public RankingService(
            EntryRepository entryRepository,
            MatchRepository matchRepository,
            TournamentRoundRepository tournamentRoundRepository,
            TournamentRepository tournamentRepository) {
        this.entryRepository = entryRepository;
        this.matchRepository = matchRepository;
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.tournamentRepository = tournamentRepository;
    }

    private record EntryOutcome(int points, boolean stillAlive) {}

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
            byIdentity.computeIfAbsent(identityKey(t), k -> new ArrayList<>()).add(t);
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

    /**
     * Contexte commun a tous les joueurs d'un meme calcul de classement. Les
     * baremes et les matchs decides (COMPLETED/BYE) de toutes les entrees sont
     * charges en une requete chacun puis indexes en memoire, plutot que
     * requetes entree par entree (N+1).
     */
    private record RankingContext(
            Map<Long, Tournament> tournamentsById,
            TournamentProgress progress,
            Map<Long, Integer> hueByTournamentId,
            Map<Long, Map<Integer, Integer>> pointsByRoundByTournamentId,
            Map<Long, List<Match>> decidedMatchesByEntryId) {

        /** Des qualifs comptent pour leur tournoi principal (meme tournoi pour le classement). */
        Tournament effective(Tournament t) {
            return RankingService.effective(tournamentsById, t);
        }
    }

    private static Tournament effective(Map<Long, Tournament> tournamentsById, Tournament t) {
        return t.isQualifying() ? tournamentsById.getOrDefault(t.getMainTournamentId(), t) : t;
    }

    /**
     * Resultats d'un joueur, fusionnes par tournoi (principal + qualifs, comme
     * dans le fichier Excel), et tournois ou il est encore en jeu.
     */
    private record PlayerResults(
            Map<Long, Integer> pointsByTournamentId,
            Map<Long, Tournament> tournamentById,
            Map<Long, LiveTournamentDto> liveByTournamentId) {}

    /** Tournois d'un joueur repartis entre cases obligatoires, Monte-Carlo, ATP Finals et "autres" (tries par points). */
    private record Buckets(
            Map<MandatorySlot, TournamentPointsDto> mandatorySlots,
            TournamentPointsDto monteCarlo,
            TournamentPointsDto atpFinals,
            List<TournamentPointsDto> others) {}

    private static String identityKey(Tournament t) {
        if (t.getMandatorySlot() != null) {
            return "SLOT:" + t.getMandatorySlot();
        }
        String week = t.getWeekNumber() != null ? String.valueOf(t.getWeekNumber()) : "id" + t.getId();
        return "NAME_WEEK:" + t.getName().trim().toUpperCase() + "@" + week;
    }

    @Transactional(readOnly = true)
    public List<RankingRowDto> computeRanking() {
        return computeRanking(t -> true);
    }

    /**
     * Classement tel qu'il etait au debut de la semaine `week` de la saison
     * `season` (comme le classement ATP du lundi) : seuls comptent les
     * tournois (et leurs qualifs) des semaines precedentes - ceux de cette
     * semaine-la et des suivantes sont ignores. Un tournoi sans numero de
     * semaine est ignore (impossible de le situer).
     */
    @Transactional(readOnly = true)
    public List<RankingRowDto> computeRankingBefore(int season, int week) {
        return computeRanking(t -> t.getSeason() != null
                && t.getWeekNumber() != null
                && (t.getSeason() < season || (t.getSeason() == season && t.getWeekNumber() < week)));
    }

    /** Classement calcule uniquement a partir des tournois principaux retenus par ce filtre (et de leurs qualifs). */
    private List<RankingRowDto> computeRanking(Predicate<Tournament> mainFilter) {
        Map<Long, Tournament> tournamentsById =
                tournamentRepository.findAll().stream().collect(Collectors.toMap(Tournament::getId, t -> t));

        List<Tournament> allMains = tournamentsById.values().stream()
                .filter(t -> !t.isQualifying())
                .filter(mainFilter)
                .toList();
        TournamentProgress progress = TournamentProgress.compute(tournamentRepository, matchRepository, allMains);

        Set<Long> activeTournamentIds = activeTournamentIds(allMains, progress);

        List<Entry> activeEntries = entryRepository.findByPlayerIsNotNull().stream()
                .filter(entry -> activeTournamentIds.contains(
                        effective(tournamentsById, entry.getTournament()).getId()))
                .toList();

        // Tournois reellement disputes par ces entrees (qualifs comprises, pas
        // seulement leur tournoi principal) : chaque tableau a son propre bareme.
        Set<Long> entryTournamentIds = activeEntries.stream()
                .map(entry -> entry.getTournament().getId())
                .collect(Collectors.toSet());

        RankingContext context = new RankingContext(
                tournamentsById,
                progress,
                progress.hueByTournamentId(allMains),
                pointsByRoundByTournamentId(entryTournamentIds),
                decidedMatchesByEntryId(entryTournamentIds));

        Map<Player, List<Entry>> byPlayer = activeEntries.stream().collect(Collectors.groupingBy(Entry::getPlayer));

        List<RankingRowDto> rows = new ArrayList<>();
        for (Map.Entry<Player, List<Entry>> e : byPlayer.entrySet()) {
            RankingRowDto row = rankingRow(e.getKey(), collectResults(e.getValue(), context));
            if (row != null) {
                rows.add(row);
            }
        }

        rows.sort(Comparator.comparingInt(RankingRowDto::total).reversed());
        return rows;
    }

    /** Bareme (tour -> points) de chaque tournoi, en une seule requete. */
    private Map<Long, Map<Integer, Integer>> pointsByRoundByTournamentId(Set<Long> tournamentIds) {
        if (tournamentIds.isEmpty()) {
            return Map.of();
        }
        return tournamentRoundRepository.findByTournamentIdIn(tournamentIds).stream()
                .collect(Collectors.groupingBy(
                        round -> round.getTournament().getId(),
                        Collectors.toMap(TournamentRound::getRoundOrder, TournamentRound::getPoints)));
    }

    /**
     * Matchs decides (COMPLETED/BYE - les seuls pris en compte par
     * {@link #pointsEarned}) de ces tournois, en une seule requete, indexes
     * par entree (entry1 comme entry2).
     */
    private Map<Long, List<Match>> decidedMatchesByEntryId(Set<Long> tournamentIds) {
        if (tournamentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Match>> byEntryId = new HashMap<>();
        for (Match m : matchRepository.findByTournament_IdInAndStatusIn(
                new ArrayList<>(tournamentIds), List.of(MatchStatus.COMPLETED, MatchStatus.BYE))) {
            for (Entry side : Arrays.asList(m.getEntry1(), m.getEntry2())) {
                if (side != null) {
                    byEntryId
                            .computeIfAbsent(side.getId(), k -> new ArrayList<>())
                            .add(m);
                }
            }
        }
        return byEntryId;
    }

    private PlayerResults collectResults(List<Entry> playerEntries, RankingContext context) {
        Map<Long, Integer> pointsByTournamentId = new LinkedHashMap<>();
        Map<Long, Tournament> tournamentById = new HashMap<>();
        Map<Long, LiveTournamentDto> liveByTournamentId = new LinkedHashMap<>();

        for (Entry entry : playerEntries) {
            EntryOutcome outcome = pointsEarned(entry, context);
            Tournament effective = context.effective(entry.getTournament());

            if (outcome.points() > 0) {
                pointsByTournamentId.merge(effective.getId(), outcome.points(), Integer::sum);
                tournamentById.putIfAbsent(effective.getId(), effective);
            }
            Integer hue = context.hueByTournamentId().get(effective.getId());
            if (outcome.stillAlive()
                    && hue != null
                    && context.progress().statusOf(effective) == TournamentStatus.IN_PROGRESS) {
                liveByTournamentId.putIfAbsent(
                        effective.getId(), new LiveTournamentDto(effective.getId(), effective.getName(), hue));
            }
        }
        return new PlayerResults(pointsByTournamentId, tournamentById, liveByTournamentId);
    }

    private Buckets classify(PlayerResults results) {
        Map<MandatorySlot, TournamentPointsDto> mandatorySlots = new EnumMap<>(MandatorySlot.class);
        TournamentPointsDto monteCarlo = null;
        TournamentPointsDto atpFinals = null;
        List<TournamentPointsDto> others = new ArrayList<>();

        for (Map.Entry<Long, Integer> pe : results.pointsByTournamentId().entrySet()) {
            Tournament tournament = results.tournamentById().get(pe.getKey());
            TournamentPointsDto dto = new TournamentPointsDto(tournament.getId(), tournament.getName(), pe.getValue());
            MandatorySlot slot = tournament.getMandatorySlot();
            if (slot == MandatorySlot.MONTE_CARLO) {
                monteCarlo = dto;
            } else if (slot == MandatorySlot.ATP_FINALS) {
                atpFinals = dto; // hors total
            } else if (slot != null) {
                mandatorySlots.put(slot, dto);
            } else {
                others.add(dto);
            }
        }
        others.sort(Comparator.comparingInt(TournamentPointsDto::points).reversed());
        return new Buckets(mandatorySlots, monteCarlo, atpFinals, others);
    }

    /** Ligne de classement du joueur, ou null s'il n'a encore aucun point comptabilisable. */
    private RankingRowDto rankingRow(Player player, PlayerResults results) {
        Buckets buckets = classify(results);
        Map<MandatorySlot, TournamentPointsDto> mandatorySlots = buckets.mandatorySlots();
        TournamentPointsDto monteCarlo = buckets.monteCarlo();
        List<TournamentPointsDto> othersAll = buckets.others();

        List<TournamentPointsDto> bestOthers = othersAll.stream().limit(5).toList();
        int best5 = bestOthers.stream().mapToInt(TournamentPointsDto::points).sum();
        TournamentPointsDto sixth = othersAll.size() > 5 ? othersAll.get(5) : null;

        int monteCarloPts = pointsOf(monteCarlo);
        int sixthPts = pointsOf(sixth);
        TournamentPointsDto replacement = monteCarloPts >= sixthPts ? monteCarlo : sixth;
        int replacementValue = Math.max(monteCarloPts, sixthPts);
        if (sumOf(mandatorySlots) + best5 + replacementValue == 0) {
            return null; // aucun resultat acte encore comptabilisable : inutile dans le classement
        }

        List<TournamentPointsDto> nonCounted = new ArrayList<>();
        if (othersAll.size() > 6) {
            nonCounted.addAll(othersAll.subList(6, othersAll.size()));
        }
        Stream.of(monteCarlo, sixth)
                .filter(candidate -> candidate != null && candidate != replacement)
                .forEach(nonCounted::add);
        nonCounted.sort(Comparator.comparingInt(TournamentPointsDto::points).reversed());

        substituteMissingMandatorySlots(mandatorySlots, nonCounted);
        int mandatoryTotal = sumOf(mandatorySlots);

        return new RankingRowDto(
                player.getId(),
                player.getLastName(),
                player.getFirstName(),
                player.getNationality(),
                mandatorySlots,
                monteCarlo,
                buckets.atpFinals(),
                bestOthers,
                replacement,
                nonCounted,
                mandatoryTotal,
                best5,
                replacementValue,
                mandatoryTotal + best5 + replacementValue,
                new ArrayList<>(results.liveByTournamentId().values()));
    }

    private static int pointsOf(TournamentPointsDto dto) {
        return dto != null ? dto.points() : 0;
    }

    private static int sumOf(Map<MandatorySlot, TournamentPointsDto> slots) {
        return slots.values().stream().mapToInt(TournamentPointsDto::points).sum();
    }

    /**
     * Recycle les tournois "non comptabilises" (au-dela des 5 meilleurs +
     * remplacement Monte-Carlo/6e) dans les cases obligatoires jamais jouees
     * par ce joueur (Grand Chelem/Masters 1000 ou il n'avait pas le
     * classement pour etre accepte) - Charles, 2026-09-18, ex: Moro Canas,
     * pas assez bien classe pour disputer le moindre Grand Chelem/Masters
     * 1000, dont le resultat excedentaire a Murcia est materialise dans la
     * case Australian Open plutot que de rester non comptabilise. Le meilleur
     * tournoi disponible va dans la case vide la plus prestigieuse (ordre de
     * {@link MandatorySlot}, Monte-Carlo exclu car deja gere a part via son
     * propre mecanisme de remplacement), et ainsi de suite. Consomme les
     * candidats utilises dans `nonCounted` (deja trie par points decroissants).
     */
    private void substituteMissingMandatorySlots(
            Map<MandatorySlot, TournamentPointsDto> mandatorySlots, List<TournamentPointsDto> nonCounted) {
        Iterator<TournamentPointsDto> candidates = nonCounted.iterator();
        for (MandatorySlot slot : MANDATORY_EXCLUDING_MC) {
            if (!mandatorySlots.containsKey(slot) && candidates.hasNext()) {
                TournamentPointsDto candidate = candidates.next();
                candidates.remove();
                mandatorySlots.put(
                        slot,
                        new TournamentPointsDto(
                                candidate.tournamentId(), candidate.tournamentName(), candidate.points(), true));
            }
        }
    }

    /** Resultat de cette entree d'apres le contexte du classement (voir {@link #outcome}). */
    private EntryOutcome pointsEarned(Entry entry, RankingContext context) {
        return outcome(
                entry,
                context.pointsByRoundByTournamentId()
                        .getOrDefault(entry.getTournament().getId(), Map.of()),
                context.decidedMatchesByEntryId().getOrDefault(entry.getId(), List.of()));
    }

    /**
     * Points gagnes par une entree, exactement comme dans le classement (voir
     * {@link #outcome}) - expose pour la fiche joueur, qui charge elle-meme
     * bareme et matchs plutot que de recalculer tout le classement.
     *
     * @param pointsByRound bareme du tournoi de l'entree (tour -> points)
     * @param decidedMatches matchs COMPLETED/BYE de cette entree
     */
    public int pointsEarned(Entry entry, Map<Integer, Integer> pointsByRound, List<Match> decidedMatches) {
        return outcome(entry, pointsByRound, decidedMatches).points();
    }

    /**
     * Resultat (points + encore en jeu ou non) de cette entree, uniquement
     * d'apres des matchs decides - jamais de resultat devine. "Encore en jeu"
     * = pas encore elimine (n'a pas perdu) et le tournoi n'est pas fini pour
     * lui ; dans ce cas les points refletent le minimum garanti par son
     * dernier tour gagne (le tour suivant, pas encore joue, reste incertain
     * mais ne peut plus lui faire perdre ce qu'il a deja gagne).
     */
    private EntryOutcome outcome(Entry entry, Map<Integer, Integer> pointsByRound, List<Match> matches) {
        Tournament tournament = entry.getTournament();

        // Un tableau de qualifications n'a pas de "finale" unique : plusieurs
        // groupes independants produisent chacun un qualifie au dernier tour
        // configure (pointsByRound.size()), contrairement au tableau principal ou
        // totalRounds se deduit de la taille du tableau (une seule finale).
        int totalRounds = totalRounds(tournament, pointsByRound);
        if (totalRounds == 0) {
            return new EntryOutcome(0, false);
        }

        Match deepest = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED || m.getStatus() == MatchStatus.BYE)
                .max(Comparator.comparingInt(Match::getRoundOrder))
                .orElse(null);

        if (deepest == null) {
            return new EntryOutcome(0, true); // pas encore de resultat, mais pas elimine
        }

        boolean won = deepest.getWinnerEntry() != null
                && deepest.getWinnerEntry().getId().equals(entry.getId());
        int r = deepest.getRoundOrder();

        if (won) {
            if (r == totalRounds) {
                if (isGrandSlamQualifying(tournament)) {
                    return new EntryOutcome(
                            GS_QUALIFYING_QUALIFIED_POINTS, false); // qualifie pour le tableau principal
                }
                return new EntryOutcome(pointsByRound.getOrDefault(totalRounds, 0), false); // champion (ou qualifie)
            }
            // Encore en jeu : credite du minimum garanti, mais uniquement d'apres
            // le dernier tour REELLEMENT gagne sur le court (COMPLETED) - un bye
            // (BYE, uniquement possible au 1er tour) ne rapporte aucun point et
            // ne declenche aucune garantie tant qu'aucun vrai match n'a ete gagne.
            Match deepestRealWin = matches.stream()
                    .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                    .filter(m -> m.getWinnerEntry() != null
                            && m.getWinnerEntry().getId().equals(entry.getId()))
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

    static int totalRounds(Tournament tournament, Map<Integer, Integer> pointsByRound) {
        if (tournament.isQualifying()) {
            return pointsByRound.size();
        }
        if (tournament.getDrawSize() == null) {
            return 0;
        }
        return RoundLabels.roundCount(RoundLabels.nextPowerOfTwo(tournament.getDrawSize()));
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
    private int eliminationValue(
            Tournament tournament, Map<Integer, Integer> pointsByRound, int round, int totalRounds) {
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
