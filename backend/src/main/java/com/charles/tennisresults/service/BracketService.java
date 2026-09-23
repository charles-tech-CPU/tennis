package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.*;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genere le squelette du tableau (tous les matchs, tour par tour) et gere
 * l'avancement automatique d'un vainqueur vers le tour suivant, y compris les
 * "byes" (avancement immediat sans match a jouer).
 */
@Service
public class BracketService {

    private final MatchRepository matchRepository;
    private final EntryRepository entryRepository;
    private final TournamentRepository tournamentRepository;

    public BracketService(
            MatchRepository matchRepository,
            EntryRepository entryRepository,
            TournamentRepository tournamentRepository) {
        this.matchRepository = matchRepository;
        this.entryRepository = entryRepository;
        this.tournamentRepository = tournamentRepository;
    }

    /** Cree tous les matchs (vides) du tournoi, du 1er tour a la finale. */
    @Transactional
    public void initializeSkeleton(Tournament tournament, int drawSlots) {
        initializeSkeleton(tournament, drawSlots, RoundLabels.roundCount(drawSlots));
    }

    /**
     * Variante avec un nombre de tours explicite, plus petit que
     * roundCount(drawSlots) : utilisee pour un tableau de qualifications, qui
     * s'arrete volontairement avant la finale (plusieurs "vainqueurs de tour"
     * simultanes = les qualifies, chacun dans son groupe independant).
     */
    @Transactional
    public void initializeSkeleton(Tournament tournament, int drawSlots, int totalRounds) {
        for (int round = 1; round <= totalRounds; round++) {
            int matchesInRound = drawSlots >> round; // round=1 -> drawSlots/2 matchs
            for (int pos = 1; pos <= matchesInRound; pos++) {
                Match m = new Match();
                m.setTournament(tournament);
                m.setRoundOrder(round);
                m.setPositionInRound(pos);
                m.setStatus(MatchStatus.PENDING);
                matchRepository.save(m);
            }
        }
    }

    /**
     * A appeler apres tout ajout/modification d'entree : remplit (ou met a jour)
     * les matchs du 1er tour a partir des entrees presentes, et propage
     * automatiquement les "byes".
     */
    @Transactional
    public void syncRound1FromEntries(Long tournamentId) {
        Tournament tournament = tournamentRepository
                .findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournoi introuvable: " + tournamentId));
        List<Entry> entries = entryRepository.findByTournamentIdOrderByDrawPositionAsc(tournamentId);
        // Un tableau de qualifications garde sa taille reelle (groupes de 4, pas de
        // "byes" pour completer jusqu'a la puissance de 2 superieure) ; seul le
        // tableau principal arrondit au plus proche 2^n avec des byes.
        int drawSlots = tournament.isQualifying()
                ? tournament.getDrawSize()
                : RoundLabels.nextPowerOfTwo(Math.max(
                        2,
                        entries.stream().mapToInt(Entry::getDrawPosition).max().orElse(0)));

        for (int pos = 1; pos * 2 <= drawSlots; pos++) {
            int posA = pos * 2 - 1;
            int posB = pos * 2;
            Entry entryA = findAtPosition(entries, posA);
            Entry entryB = findAtPosition(entries, posB);

            // Les matchs du 1er tour sont normalement deja tous crees par
            // initializeSkeleton a la creation du tournoi ; ce fallback ne sert que
            // si le tableau a ete elargi depuis (securite, ne devrait pas arriver en v1).
            Match match = round1Match(tournament, pos);

            match.setEntry1(entryA);
            match.setEntry2(entryB);

            boolean byeA = entryA != null && entryA.isBye();
            boolean byeB = entryB != null && entryB.isBye();

            if (byeA != byeB && entryA != null && entryB != null) {
                // exactement un des deux cotes est un bye (deux byes adjacents n'ont pas
                // de sens et ne sont pas geres automatiquement - a corriger a la main).
                Entry winner = byeA ? entryB : entryA;
                match.setStatus(MatchStatus.BYE);
                match.setWinnerEntry(winner);
                matchRepository.save(match);
                advanceWinner(match, winner);
            } else if (entryA != null && entryB != null) {
                if (match.getStatus() == MatchStatus.PENDING) {
                    match.setStatus(MatchStatus.SCHEDULED);
                }
                matchRepository.save(match);
            } else {
                matchRepository.save(match);
            }
        }
    }

    private Match round1Match(Tournament tournament, int position) {
        return matchRepository
                .findByTournamentIdAndRoundOrderAndPositionInRound(tournament.getId(), 1, position)
                .orElseGet(() -> {
                    Match m = new Match();
                    m.setTournament(tournament);
                    m.setRoundOrder(1);
                    m.setPositionInRound(position);
                    m.setStatus(MatchStatus.PENDING);
                    return m;
                });
    }

    private Entry findAtPosition(List<Entry> entries, int position) {
        return entries.stream()
                .filter(e -> e.getDrawPosition() == position)
                .findFirst()
                .orElse(null);
    }

    /** Place le vainqueur dans le match du tour suivant, et sauvegarde ce match. */
    @Transactional
    public void advanceWinner(Match completedMatch, Entry winner) {
        Optional<Match> parentOpt = matchRepository.findByTournamentIdAndRoundOrderAndPositionInRound(
                completedMatch.getTournament().getId(),
                completedMatch.getRoundOrder() + 1,
                (completedMatch.getPositionInRound() + 1) / 2);
        if (parentOpt.isEmpty()) {
            return; // completedMatch etait la finale
        }
        Match parent = parentOpt.get();
        boolean isFirstSlot = completedMatch.getPositionInRound() % 2 != 0;
        if (isFirstSlot) {
            parent.setEntry1(winner);
        } else {
            parent.setEntry2(winner);
        }
        if (parent.getEntry1() != null && parent.getEntry2() != null && parent.getStatus() == MatchStatus.PENDING) {
            parent.setStatus(MatchStatus.SCHEDULED);
        }
        matchRepository.save(parent);
    }

    /**
     * Annule la propagation issue de ce match (utilise quand on corrige un score
     * deja saisi) : vide le slot correspondant du tour suivant, et si ce tour
     * suivant avait lui-meme deja ete joue avec ce joueur, annule recursivement
     * son propre resultat.
     */
    @Transactional
    public void resetDownstream(Match match) {
        Optional<Match> parentOpt = matchRepository.findByTournamentIdAndRoundOrderAndPositionInRound(
                match.getTournament().getId(), match.getRoundOrder() + 1, (match.getPositionInRound() + 1) / 2);
        if (parentOpt.isEmpty()) {
            return;
        }
        Match parent = parentOpt.get();
        boolean wasCompleted = parent.getStatus() == MatchStatus.COMPLETED || parent.getStatus() == MatchStatus.BYE;
        boolean isFirstSlot = match.getPositionInRound() % 2 != 0;
        if (isFirstSlot) {
            parent.setEntry1(null);
        } else {
            parent.setEntry2(null);
        }
        if (wasCompleted) {
            // le resultat de parent reposait sur l'ancien vainqueur : on l'annule aussi
            resetDownstream(parent);
            parent.setScore(null);
            parent.setWinnerEntry(null);
        }
        parent.setStatus(MatchStatus.PENDING);
        matchRepository.save(parent);
    }

    /**
     * Retire une entree de tous les matchs qui la referencent : a appeler
     * AVANT de supprimer l'entree elle-meme (la table match_entry a une cle
     * etrangere vers entry, sans cascade - sans ca, la suppression echoue).
     * Si l'entree avait deja gagne un match, annule d'abord en cascade tout
     * ce qui en decoulait plus loin dans le tableau (resetDownstream), pour
     * ne jamais laisser un tour suivant referencer un resultat perime.
     */
    @Transactional
    public void clearEntryFromMatches(Entry entry) {
        for (Match match : matchRepository.findByEntry1_IdOrEntry2_Id(entry.getId(), entry.getId())) {
            if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.BYE) {
                resetDownstream(match);
                match.setScore(null);
                match.setWinnerEntry(null);
            }
            if (match.getEntry1() != null && match.getEntry1().getId().equals(entry.getId())) {
                match.setEntry1(null);
            }
            if (match.getEntry2() != null && match.getEntry2().getId().equals(entry.getId())) {
                match.setEntry2(null);
            }
            match.setStatus(MatchStatus.PENDING);
            matchRepository.save(match);
        }
    }
}
