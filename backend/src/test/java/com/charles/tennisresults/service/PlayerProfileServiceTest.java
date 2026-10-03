package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.CategoryBestResultDto;
import com.charles.tennisresults.dto.PlayerProfileDto;
import com.charles.tennisresults.dto.PlayerTournamentResultDto;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.PlayerRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le calcul des points est celui, reel, de RankingService (espionne) : seul
 * le classement complet (computeRanking) est remplace par une liste fixe.
 */
class PlayerProfileServiceTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final EntryRepository entryRepository = mock(EntryRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final TournamentRepository tournamentRepository = mock(TournamentRepository.class);
    private final TournamentRoundRepository tournamentRoundRepository = mock(TournamentRoundRepository.class);
    private RankingService rankingService;
    private PlayerProfileService service;

    private final List<Tournament> tournaments = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private final List<Match> matches = new ArrayList<>();
    private final List<TournamentRound> rounds = new ArrayList<>();
    private long nextId = 1;

    private Player alcaraz;
    private Player sinner;

    @BeforeEach
    void setUp() {
        rankingService = spy(
                new RankingService(entryRepository, matchRepository, tournamentRoundRepository, tournamentRepository));
        service = new PlayerProfileService(
                playerRepository,
                entryRepository,
                matchRepository,
                tournamentRepository,
                tournamentRoundRepository,
                rankingService);

        alcaraz = player("ALCARAZ");
        sinner = player("SINNER");
        doReturn(List.of(row(sinner, 1200), row(alcaraz, 900)))
                .when(rankingService)
                .computeRanking();

        lenient()
                .when(entryRepository.findByPlayerId(alcaraz.getId()))
                .thenAnswer(inv ->
                        entries.stream().filter(e -> e.getPlayer() == alcaraz).toList());
        lenient()
                .when(matchRepository.findByEntryIdsAndStatusIn(anyCollection(), anyList()))
                .thenAnswer(inv -> {
                    Collection<Long> ids = inv.getArgument(0);
                    List<MatchStatus> statuses = inv.getArgument(1);
                    return matches.stream()
                            .filter(m -> statuses.contains(m.getStatus()))
                            .filter(m -> ids.contains(m.getEntry1().getId())
                                    || ids.contains(m.getEntry2().getId()))
                            .toList();
                });
        lenient()
                .when(tournamentRoundRepository.findByTournamentIdIn(anyCollection()))
                .thenAnswer(inv -> {
                    Collection<Long> ids = inv.getArgument(0);
                    return rounds.stream()
                            .filter(r -> ids.contains(r.getTournament().getId()))
                            .toList();
                });
        lenient().when(tournamentRepository.findAllById(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return tournaments.stream().filter(t -> ids.contains(t.getId())).toList();
        });
    }

    @Test
    void ficheComplete() {
        // Doha 2026 : titre
        Tournament doha = tournament("DOHA", 2026, 7, TournamentCategory.ATP_500, 4, 100, 250);
        Entry a1 = entry(doha, alcaraz);
        played(doha, 1, a1, entry(doha, sinner), MatchStatus.COMPLETED);
        played(doha, 2, a1, entry(doha, sinner), MatchStatus.COMPLETED);

        // Wimbledon 2026 : bye au 1er tour puis battu en demi (ne compte pas dans le bilan)
        Tournament wimbledon = tournament("WIMBLEDON", 2026, 27, TournamentCategory.GRAND_SLAM, 8, 0, 50, 400, 1000);
        Entry a2 = entry(wimbledon, alcaraz);
        Entry bye = entry(wimbledon, null);
        played(wimbledon, 1, a2, bye, MatchStatus.BYE);
        Entry s2 = entry(wimbledon, sinner);
        played(wimbledon, 2, s2, a2, MatchStatus.COMPLETED);

        // Madrid 2027 : sorti des qualifs, battu au 1er tour du tableau principal
        Tournament madrid = tournament("MADRID", 2027, 17, TournamentCategory.MASTERS_1000, 4, 10, 1000);
        Tournament madridQ = tournament("MADRID", 2027, 17, TournamentCategory.MASTERS_1000, 8, 0, 20);
        madridQ.setQualifying(true);
        madridQ.setMainTournamentId(madrid.getId());
        Entry aq = entry(madridQ, alcaraz);
        played(madridQ, 1, aq, entry(madridQ, sinner), MatchStatus.COMPLETED);
        played(madridQ, 2, aq, entry(madridQ, sinner), MatchStatus.COMPLETED);
        Entry am = entry(madrid, alcaraz);
        played(madrid, 1, entry(madrid, sinner), am, MatchStatus.COMPLETED);

        PlayerProfileDto profile = service.profile(alcaraz.getId());

        assertThat(profile.rankingPosition()).isEqualTo(2);
        assertThat(profile.rankingTotal()).isEqualTo(900);
        assertThat(profile.rankedPlayers()).isEqualTo(2);

        assertThat(profile.record().played()).isEqualTo(6);
        assertThat(profile.record().wins()).isEqualTo(4);
        assertThat(profile.record().losses()).isEqualTo(2);

        assertThat(profile.tournaments())
                .extracting(r -> r.tournamentName() + " " + r.season() + " " + r.roundLabel() + " " + r.points())
                .containsExactly("MADRID 2027 SF 30", "WIMBLEDON 2026 SF 50", "DOHA 2026 F 250");
        assertThat(profile.tournaments().get(0).viaQualifying()).isTrue();

        assertThat(profile.titles())
                .extracting(PlayerTournamentResultDto::tournamentName)
                .containsExactly("DOHA");

        assertThat(profile.bestByCategory())
                .extracting(b -> b.category() + " " + b.best().roundLabel() + " "
                        + b.best().champion())
                .containsExactly("GRAND_SLAM SF false", "MASTERS_1000 SF false", "ATP_500 F true");
    }

    @Test
    void leMeilleurResultatParCategorieCompareLesToursRestantsAvantLaFinale() {
        // QF dans un tableau de 8 (3 tours) vaut mieux qu'un 2e tour dans un tableau de 32 (5 tours)
        Tournament small = tournament("PETIT", 2026, 10, TournamentCategory.ATP_250, 8, 0, 10, 20, 40);
        Entry a1 = entry(small, alcaraz);
        played(small, 1, entry(small, sinner), a1, MatchStatus.COMPLETED);
        Tournament big = tournament("GRAND", 2026, 11, TournamentCategory.ATP_250, 32, 0, 5, 10, 20, 40, 80);
        Entry a2 = entry(big, alcaraz);
        played(big, 1, a2, entry(big, sinner), MatchStatus.COMPLETED);
        played(big, 2, entry(big, sinner), a2, MatchStatus.COMPLETED);

        CategoryBestResultDto best =
                service.profile(alcaraz.getId()).bestByCategory().get(0);

        assertThat(best.best().tournamentName()).isEqualTo("PETIT");
        assertThat(best.best().roundLabel()).isEqualTo("QF");
    }

    @Test
    void joueurSansTournoiNiClassement() {
        Player newcomer = player("NOUVEAU");
        when(entryRepository.findByPlayerId(newcomer.getId())).thenReturn(List.of());

        PlayerProfileDto profile = service.profile(newcomer.getId());

        assertThat(profile.rankingPosition()).isNull();
        assertThat(profile.record().played()).isZero();
        assertThat(profile.record().winRate()).isNull();
        assertThat(profile.tournaments()).isEmpty();
    }

    @Test
    void joueurInconnu() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.profile(99L)).isInstanceOf(EntityNotFoundException.class);
    }

    /** pointsByRound[i] = points d'une elimination au tour i+1 ; le dernier = points du vainqueur. */
    private Tournament tournament(
            String name, int season, int week, TournamentCategory category, int drawSize, int... pointsByRound) {
        Tournament t = new Tournament();
        t.setId(nextId++);
        t.setName(name);
        t.setSeason(season);
        t.setWeekNumber(week);
        t.setCategory(category);
        t.setDrawSize(drawSize);
        for (int i = 0; i < pointsByRound.length; i++) {
            TournamentRound round = new TournamentRound();
            round.setTournament(t);
            round.setRoundOrder(i + 1);
            round.setPoints(pointsByRound[i]);
            round.setRoundLabel(t.getDrawSize() == null ? "?" : RoundLabels.labelFor(t, i + 1));
            rounds.add(round);
        }
        tournaments.add(t);
        return t;
    }

    private Entry entry(Tournament t, Player p) {
        Entry e = new Entry();
        e.setId(nextId++);
        e.setTournament(t);
        e.setPlayer(p);
        entries.add(e);
        return e;
    }

    private void played(Tournament t, int round, Entry winner, Entry loser, MatchStatus status) {
        Match m = new Match();
        m.setId(nextId++);
        m.setTournament(t);
        m.setRoundOrder(round);
        m.setEntry1(winner);
        m.setEntry2(loser);
        m.setWinnerEntry(winner);
        m.setStatus(status);
        matches.add(m);
    }

    private Player player(String lastName) {
        Player p = new Player();
        p.setId(nextId++);
        p.setLastName(lastName);
        lenient().when(playerRepository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private static RankingRowDto row(Player p, int total) {
        return new RankingRowDto(
                p.getId(),
                p.getLastName(),
                null,
                null,
                Map.of(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                0,
                0,
                0,
                total,
                List.of());
    }
}
