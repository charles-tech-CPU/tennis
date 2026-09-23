package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.MandatorySlot;
import com.charles.tennisresults.domain.Match;
import com.charles.tennisresults.domain.MatchStatus;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.domain.TournamentCategory;
import com.charles.tennisresults.domain.TournamentRound;
import com.charles.tennisresults.dto.RankingRowDto;
import com.charles.tennisresults.dto.TournamentPointsDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.MatchRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import com.charles.tennisresults.repository.TournamentRoundRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Les repositories sont branches sur des listes en memoire : chaque test
 * decrit un petit circuit (tournois, entrees, matchs joues) puis verifie le
 * classement calcule.
 */
@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentRoundRepository tournamentRoundRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private RankingService rankingService;

    private final List<Tournament> tournaments = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private final List<Match> matches = new ArrayList<>();
    private final Map<Long, List<TournamentRound>> roundsByTournamentId = new HashMap<>();
    private long nextId = 1;

    private Player alcaraz;
    private Player sinner;
    private Player zverev;
    private Player fritz;

    @BeforeEach
    void setUp() {
        alcaraz = player("ALCARAZ");
        sinner = player("SINNER");
        zverev = player("ZVEREV");
        fritz = player("FRITZ");

        lenient().when(tournamentRepository.findAll()).thenReturn(tournaments);
        lenient().when(entryRepository.findByPlayerIsNotNull()).thenReturn(entries);
        lenient()
                .when(tournamentRoundRepository.findByTournamentIdOrderByRoundOrderAsc(anyLong()))
                .thenAnswer(inv -> roundsByTournamentId.getOrDefault(inv.<Long>getArgument(0), List.of()));
        lenient()
                .when(matchRepository.findByEntry1_IdOrEntry2_Id(anyLong(), anyLong()))
                .thenAnswer(inv -> matches.stream()
                        .filter(m -> involves(m, inv.getArgument(0)))
                        .toList());
        lenient()
                .when(matchRepository.findByTournament_IdInAndStatusIn(anyList(), anyList()))
                .thenAnswer(inv -> {
                    List<Long> ids = inv.getArgument(0);
                    List<MatchStatus> statuses = inv.getArgument(1);
                    return matches.stream()
                            .filter(m -> ids.contains(m.getTournament().getId()))
                            .filter(m -> statuses.contains(m.getStatus()))
                            .toList();
                });
    }

    @Test
    void chaqueJoueurRecoitLesPointsDeSonDernierTour() {
        Tournament doha = tournament("DOHA", 2026, 7, null, 4, 100, 250);
        doha.setRunnerUpPoints(165);
        Entry a = entry(doha, alcaraz);
        Entry s = entry(doha, sinner);
        Entry z = entry(doha, zverev);
        Entry f = entry(doha, fritz);
        played(doha, 1, 1, a, s);
        played(doha, 1, 2, z, f);
        played(doha, 2, 1, a, z);

        List<RankingRowDto> ranking = rankingService.computeRanking();

        assertThat(ranking).extracting(RankingRowDto::lastName).startsWith("ALCARAZ", "ZVEREV");
        assertThat(totalOf(ranking, "ALCARAZ")).isEqualTo(250);
        assertThat(totalOf(ranking, "ZVEREV")).isEqualTo(165);
        assertThat(totalOf(ranking, "SINNER")).isEqualTo(100);
        assertThat(totalOf(ranking, "FRITZ")).isEqualTo(100);
        assertThat(row(ranking, "ALCARAZ").liveTournaments()).isEmpty();
    }

    @Test
    void unJoueurEncoreEnJeuEstCrediteDuMinimumGaranti() {
        Tournament doha = tournament("DOHA", 2026, 7, null, 4, 100, 250);
        doha.setRunnerUpPoints(165);
        Entry a = entry(doha, alcaraz);
        Entry s = entry(doha, sinner);
        Entry z = entry(doha, zverev);
        Entry f = entry(doha, fritz);
        played(doha, 1, 1, a, s);
        played(doha, 1, 2, z, f);
        // finale pas encore jouee

        List<RankingRowDto> ranking = rankingService.computeRanking();

        assertThat(totalOf(ranking, "ALCARAZ")).isEqualTo(165);
        assertThat(row(ranking, "ALCARAZ").liveTournaments())
                .singleElement()
                .satisfies(live -> assertThat(live.tournamentName()).isEqualTo("DOHA"));
        assertThat(row(ranking, "SINNER").liveTournaments()).isEmpty();
    }

    @Test
    void monteCarloRemplaceLeSixiemeMeilleurTournoiQuandIlRapportePlus() {
        winOthers(alcaraz, 500, 400, 300, 200, 100, 90);
        win(alcaraz, tournament("MONTE-CARLO", 2026, 15, MandatorySlot.MONTE_CARLO, 2, 1000));

        RankingRowDto row = row(rankingService.computeRanking(), "ALCARAZ");

        assertThat(row.othersTotal()).isEqualTo(1500);
        assertThat(row.replacement().tournamentName()).isEqualTo("MONTE-CARLO");
        assertThat(row.replacementValue()).isEqualTo(1000);
        // le 6e tournoi (90), non comptabilise, est recycle dans la case vide de l'Open d'Australie
        TournamentPointsDto australianOpen = row.mandatorySlots().get(MandatorySlot.AUSTRALIAN_OPEN);
        assertThat(australianOpen.points()).isEqualTo(90);
        assertThat(australianOpen.substituted()).isTrue();
        assertThat(row.total()).isEqualTo(90 + 1500 + 1000);
    }

    @Test
    void leSixiemeMeilleurTournoiRemplaceMonteCarloQuandIlRapportePlus() {
        winOthers(alcaraz, 500, 400, 300, 200, 100, 90);
        win(alcaraz, tournament("MONTE-CARLO", 2026, 15, MandatorySlot.MONTE_CARLO, 2, 50));

        RankingRowDto row = row(rankingService.computeRanking(), "ALCARAZ");

        assertThat(row.replacementValue()).isEqualTo(90);
        assertThat(row.mandatorySlots().get(MandatorySlot.AUSTRALIAN_OPEN).points())
                .isEqualTo(50);
        assertThat(row.total()).isEqualTo(50 + 1500 + 90);
    }

    @Test
    void lesCasesObligatoiresJoueesComptentToutes() {
        win(alcaraz, tournament("ROLAND GARROS", 2026, 22, MandatorySlot.ROLAND_GARROS, 2, 2000));
        win(alcaraz, tournament("MADRID", 2026, 17, MandatorySlot.MADRID, 2, 1000));

        RankingRowDto row = row(rankingService.computeRanking(), "ALCARAZ");

        assertThat(row.mandatorySlots()).containsOnlyKeys(MandatorySlot.ROLAND_GARROS, MandatorySlot.MADRID);
        assertThat(row.mandatoryTotal()).isEqualTo(3000);
        assertThat(row.total()).isEqualTo(3000);
    }

    @Test
    void laNouvelleEditionRemplaceLAncienneDesQuElleACommence() {
        win(alcaraz, tournament("DOHA", 2026, 7, null, 2, 250));
        Tournament doha2027 = tournament("DOHA", 2027, 7, null, 4, 100, 250);
        Entry a = entry(doha2027, alcaraz);
        Entry s = entry(doha2027, sinner);
        played(doha2027, 1, 1, s, a);

        assertThat(totalOf(rankingService.computeRanking(), "ALCARAZ")).isEqualTo(100);
    }

    @Test
    void lAncienneEditionCompteTantQueLaNouvelleNAPasCommence() {
        win(alcaraz, tournament("DOHA", 2026, 7, null, 2, 250));
        Tournament doha2027 = tournament("DOHA", 2027, 7, null, 4, 100, 250);
        entry(doha2027, alcaraz);

        assertThat(totalOf(rankingService.computeRanking(), "ALCARAZ")).isEqualTo(250);
    }

    @Test
    void deuxTournoisDuMemeNomADesSemainesDifferentesComptentTousLesDeux() {
        win(alcaraz, tournament("NOTTINGHAM", 2026, 1, null, 2, 50));
        win(alcaraz, tournament("NOTTINGHAM", 2026, 25, null, 2, 125));

        assertThat(totalOf(rankingService.computeRanking(), "ALCARAZ")).isEqualTo(175);
    }

    @Test
    void unQualifieDeGrandChelemTouche25PointsDansLaCaseDuTournoi() {
        Tournament rolandGarros = tournament("ROLAND GARROS", 2026, 22, MandatorySlot.ROLAND_GARROS, 128, 10, 2000);
        rolandGarros.setCategory(TournamentCategory.GRAND_SLAM);
        Tournament qualifs = tournament("ROLAND GARROS Q", 2026, 21, null, 4, 0, 0, 0);
        qualifs.setCategory(TournamentCategory.GRAND_SLAM);
        qualifs.setQualifying(true);
        qualifs.setMainTournamentId(rolandGarros.getId());
        Entry a = entry(qualifs, alcaraz);
        Entry s = entry(qualifs, sinner);
        played(qualifs, 1, 1, a, null);
        played(qualifs, 2, 1, a, s);
        played(qualifs, 3, 1, a, null);

        List<RankingRowDto> ranking = rankingService.computeRanking();

        assertThat(row(ranking, "ALCARAZ")
                        .mandatorySlots()
                        .get(MandatorySlot.ROLAND_GARROS)
                        .points())
                .isEqualTo(25);
        assertThat(totalOf(ranking, "SINNER")).isEqualTo(8);
    }

    @Test
    void unJoueurSansAucunPointNApparaitPasAuClassement() {
        Tournament doha = tournament("DOHA", 2026, 7, null, 2, 250);
        Entry a = entry(doha, alcaraz);
        Entry s = entry(doha, sinner);
        played(doha, 1, 1, a, s);

        assertThat(rankingService.computeRanking())
                .extracting(RankingRowDto::lastName)
                .containsExactly("ALCARAZ");
    }

    // --- Construction du circuit de test ---

    /** Un tournoi "autre" gagne par joueur pour chaque montant de points, chacun a une semaine differente. */
    private void winOthers(Player player, int... points) {
        for (int i = 0; i < points.length; i++) {
            win(player, tournament("ATP " + i, 2026, 30 + i, null, 2, points[i]));
        }
    }

    /** Le joueur gagne la finale (unique match) d'un tableau de 2. */
    private void win(Player player, Tournament tournament) {
        Entry winner = entry(tournament, player);
        Entry loser = entry(tournament, player("ADVERSAIRE " + nextId));
        played(tournament, 1, 1, winner, loser);
    }

    /** pointsByRound[i] = points d'une elimination au tour i+1 ; le dernier = points du vainqueur. */
    private Tournament tournament(
            String name, int season, int week, MandatorySlot slot, int drawSize, int... pointsByRound) {
        Tournament tournament = new Tournament();
        tournament.setId(nextId++);
        tournament.setName(name);
        tournament.setSeason(season);
        tournament.setWeekNumber(week);
        tournament.setMandatorySlot(slot);
        tournament.setDrawSize(drawSize);
        tournament.setCategory(TournamentCategory.ATP_250);
        List<TournamentRound> rounds = new ArrayList<>();
        for (int i = 0; i < pointsByRound.length; i++) {
            TournamentRound round = new TournamentRound();
            round.setTournament(tournament);
            round.setRoundOrder(i + 1);
            round.setPoints(pointsByRound[i]);
            rounds.add(round);
        }
        roundsByTournamentId.put(tournament.getId(), rounds);
        tournaments.add(tournament);
        return tournament;
    }

    private Entry entry(Tournament tournament, Player player) {
        Entry entry = new Entry();
        entry.setId(nextId++);
        entry.setTournament(tournament);
        entry.setPlayer(player);
        entries.add(entry);
        return entry;
    }

    private void played(Tournament tournament, int round, int position, Entry winner, Entry loser) {
        Match match = new Match();
        match.setId(nextId++);
        match.setTournament(tournament);
        match.setRoundOrder(round);
        match.setPositionInRound(position);
        match.setEntry1(winner);
        match.setEntry2(loser);
        match.setWinnerEntry(winner);
        match.setScore("6-4 6-4");
        match.setStatus(MatchStatus.COMPLETED);
        matches.add(match);
    }

    private Player player(String lastName) {
        Player player = new Player();
        player.setId(nextId++);
        player.setLastName(lastName);
        return player;
    }

    private static boolean involves(Match match, Long entryId) {
        return (match.getEntry1() != null && match.getEntry1().getId().equals(entryId))
                || (match.getEntry2() != null && match.getEntry2().getId().equals(entryId));
    }

    private static RankingRowDto row(List<RankingRowDto> ranking, String lastName) {
        return ranking.stream()
                .filter(r -> r.lastName().equals(lastName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(lastName + " absent du classement"));
    }

    private static int totalOf(List<RankingRowDto> ranking, String lastName) {
        return row(ranking, lastName).total();
    }
}
