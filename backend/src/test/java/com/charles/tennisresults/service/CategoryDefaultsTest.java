package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.charles.tennisresults.domain.TournamentCategory;
import org.junit.jupiter.api.Test;

class CategoryDefaultsTest {

    @Test
    void baremeConnuPourUnGrandChelemDe128() {
        assertThat(CategoryDefaults.pointsFor(TournamentCategory.GRAND_SLAM, 128))
                .containsExactly(10, 45, 90, 180, 360, 720, 2000);
    }

    @Test
    void baremeConnuPourUnAtp250De32() {
        assertThat(CategoryDefaults.pointsFor(TournamentCategory.ATP_250, 32)).containsExactly(25, 50, 100, 165, 250);
    }

    @Test
    void baremeGeneriqueDiviseParDeuxAChaqueTourQuandAucunDefautConnu() {
        // ATP 500 en 16 cases : pas de bareme connu -> 4 tours, points du vainqueur divises par 2 a chaque tour
        assertThat(CategoryDefaults.pointsFor(TournamentCategory.ATP_500, 16)).containsExactly(62, 125, 250, 500);
    }

    @Test
    void baremeGeneriqueNeDescendJamaisSousUnPoint() {
        assertThat(CategoryDefaults.pointsFor(TournamentCategory.ATP_50, 256))
                .first()
                .isEqualTo(1);
    }
}
