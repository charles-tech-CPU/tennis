package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.EntryCreateDto;
import com.charles.tennisresults.dto.EntryDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.PlayerRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class EntryService {

    private final EntryRepository entryRepository;
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final BracketService bracketService;

    public EntryService(EntryRepository entryRepository, TournamentRepository tournamentRepository,
                         PlayerRepository playerRepository, BracketService bracketService) {
        this.entryRepository = entryRepository;
        this.tournamentRepository = tournamentRepository;
        this.playerRepository = playerRepository;
        this.bracketService = bracketService;
    }

    @Transactional(readOnly = true)
    public List<EntryDto> findByTournament(Long tournamentId) {
        return entryRepository.findByTournamentIdOrderByDrawPositionAsc(tournamentId).stream()
                .sorted(Comparator.comparing(Entry::getDrawPosition))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EntryDto create(Long tournamentId, EntryCreateDto dto) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournoi introuvable: " + tournamentId));

        Entry entry = new Entry();
        entry.setTournament(tournament);
        entry.setDrawPosition(dto.drawPosition());
        entry.setSeed(dto.seed());
        entry.setEntryType(dto.entryType());
        entry.setBye(dto.bye());

        if (!dto.bye()) {
            if (dto.playerId() == null) {
                throw new IllegalArgumentException("playerId est requis pour une entree qui n'est pas un bye.");
            }
            Player player = playerRepository.findById(dto.playerId())
                    .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable: " + dto.playerId()));

            if (!entryRepository.findByTournamentIdAndPlayerId(tournamentId, dto.playerId()).isEmpty()) {
                throw new IllegalArgumentException(
                        player.getLastName() + " est deja inscrit dans ce tableau.");
            }
            checkSameWeekConflict(tournament, player);

            entry.setPlayer(player);
        }

        entry = entryRepository.save(entry);
        bracketService.syncRound1FromEntries(tournamentId);
        return toDto(entry);
    }

    @Transactional
    public void delete(Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entree introuvable: " + entryId));
        Long tournamentId = entry.getTournament().getId();
        bracketService.clearEntryFromMatches(entry);
        entryRepository.delete(entry);
        bracketService.syncRound1FromEntries(tournamentId);
    }

    /**
     * Un joueur ne peut pas etre inscrit dans 2 tournois differents la meme
     * semaine (meme saison) - un vrai joueur ne joue qu'un seul evenement a
     * la fois. Le tableau de qualifs et son tableau principal partagent la
     * meme semaine par construction et ne comptent pas comme "2 tournois"
     * (un qualifie promu au tableau principal y a legitimement sa propre
     * entree, en plus de celle des qualifs).
     *
     * Exception : Indian Wells, Miami, Madrid et Rome sont exemptes de cette
     * verification (Charles, 2026-09-20 - ces 4 Masters 1000 ont chacun leur
     * propre subtilite de calendrier - Sunshine Double sur 2 semaines pour
     * IW/Miami, qualifs a cheval sur la semaine d'avant pour Madrid/Rome - et
     * ont fini par generer trop de faux positifs bloquants ; plus simple et
     * plus fiable de ne plus du tout verifier les conflits de semaine pour
     * ces tournois-la que de continuer a chasser des cas particuliers).
     * Consequence acceptee : les points de classement d'un joueur peuvent
     * temporairement compter pour 2 tournois a la fois dans ces cas, a
     * corriger manuellement si besoin.
     *
     * Canada et Cincinnati ajoutes a la liste (Charles, 2026-09-23) : leurs
     * qualifs commencent en debut de semaine, un joueur peut donc legitimement
     * y figurer tout en etant inscrit a un autre tournoi de la meme semaine.
     */
    private void checkSameWeekConflict(Tournament tournament, Player player) {
        if (tournament.getWeekNumber() == null) {
            return; // semaine non renseignee : rien a comparer
        }
        if (isExemptFromWeekConflict(tournament)) {
            return;
        }
        Long siblingId = tournament.isQualifying()
                ? tournament.getMainTournamentId()
                : tournamentRepository.findByMainTournamentId(tournament.getId()).map(Tournament::getId).orElse(null);

        for (Entry existing : entryRepository.findByPlayerId(player.getId())) {
            Tournament other = existing.getTournament();
            if (other.getId().equals(tournament.getId()) || other.getId().equals(siblingId)) {
                continue;
            }
            if (isExemptFromWeekConflict(other)) {
                continue;
            }
            if (Objects.equals(other.getSeason(), tournament.getSeason())
                    && Objects.equals(other.getWeekNumber(), tournament.getWeekNumber())) {
                throw new IllegalArgumentException(
                        player.getLastName() + " est deja inscrit a " + other.getName()
                                + " la meme semaine (semaine " + tournament.getWeekNumber() + ").");
            }
        }
    }

    private static final Set<String> WEEK_CONFLICT_EXEMPT_NAMES = Set.of(
            "MIAMI", "MIAMI - QUALIFS",
            "INDIAN WELLS", "INDIAN WELLS - QUALIFS",
            "MADRID", "MADRID - QUALIFS",
            "ROME", "ROME - QUALIFS",
            "CANADA", "CANADA - QUALIFS",
            "CINCINNATI", "CINCINNATI - QUALIFS");

    private static boolean isExemptFromWeekConflict(Tournament t) {
        return t.getName() != null && WEEK_CONFLICT_EXEMPT_NAMES.contains(t.getName().toUpperCase());
    }

    private EntryDto toDto(Entry e) {
        return new EntryDto(
                e.getId(), e.getTournament().getId(),
                e.getPlayer() != null ? e.getPlayer().getId() : null,
                e.getPlayer() != null ? e.getPlayer().getLastName() : (e.isBye() ? "BYE" : null),
                e.getPlayer() != null ? e.getPlayer().getFirstName() : null,
                e.getPlayer() != null ? e.getPlayer().getNationality() : null,
                e.getDrawPosition(), e.getSeed(), e.getEntryType(), e.isBye()
        );
    }
}
