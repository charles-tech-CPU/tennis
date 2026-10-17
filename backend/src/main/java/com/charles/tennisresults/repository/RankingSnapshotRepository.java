package com.charles.tennisresults.repository;

import com.charles.tennisresults.dto.RankingHistoryPointDto;
import com.charles.tennisresults.dto.RankingRowDto;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Photos hebdomadaires du classement (tables ranking_snapshot_week et
 * ranking_snapshot, voir V26). En JDBC plutot qu'en JPA : une semaine compte
 * plusieurs centaines de joueurs classes, inseres d'un seul lot.
 */
@Repository
public class RankingSnapshotRepository {

    private final JdbcTemplate jdbc;

    public RankingSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Semaines de cette saison deja calculees. */
    public Set<Integer> computedWeeks(int season) {
        return new HashSet<>(jdbc.queryForList(
                "SELECT week_number FROM ranking_snapshot_week WHERE season = ?", Integer.class, season));
    }

    /**
     * Enregistre le classement de cette semaine (ranking deja trie, 1er en
     * tete). Sans effet si la semaine a deja ete enregistree entre-temps.
     */
    public void saveWeek(int season, int week, List<RankingRowDto> ranking) {
        int inserted = jdbc.update(
                "INSERT INTO ranking_snapshot_week (season, week_number) VALUES (?, ?) ON CONFLICT DO NOTHING",
                season,
                week);
        if (inserted == 0 || ranking.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(
                "INSERT INTO ranking_snapshot (season, week_number, player_id, position, points) VALUES (?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        RankingRowDto row = ranking.get(i);
                        ps.setInt(1, season);
                        ps.setInt(2, week);
                        ps.setLong(3, row.playerId());
                        ps.setInt(4, i + 1);
                        ps.setInt(5, row.total());
                    }

                    @Override
                    public int getBatchSize() {
                        return ranking.size();
                    }
                });
    }

    /**
     * Efface les semaines influencees par un resultat de la semaine `week` de
     * la saison `season` : les suivantes de la meme saison et toutes celles
     * des saisons d'apres (classement glissant). Les joueurs suivent (cascade).
     */
    public void deleteAfter(int season, int week) {
        jdbc.update(
                "DELETE FROM ranking_snapshot_week WHERE season > ? OR (season = ? AND week_number > ?)",
                season,
                season,
                week);
    }

    public void deleteAll() {
        jdbc.update("DELETE FROM ranking_snapshot_week");
    }

    /** Classement semaine par semaine de ce joueur sur la saison (semaines ou il etait classe). */
    public List<RankingHistoryPointDto> historyOf(Long playerId, int season) {
        return jdbc.query(
                "SELECT season, week_number, position, points FROM ranking_snapshot"
                        + " WHERE player_id = ? AND season = ? ORDER BY week_number",
                (rs, i) -> new RankingHistoryPointDto(
                        rs.getInt("season"), rs.getInt("week_number"), rs.getInt("position"), rs.getInt("points")),
                playerId,
                season);
    }
}
