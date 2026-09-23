package com.charles.tennisresults.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoundLabelsTest {

    @Test
    void nextPowerOfTwoArrondiALaPuissanceDeDeuxSuperieure() {
        assertThat(RoundLabels.nextPowerOfTwo(1)).isEqualTo(1);
        assertThat(RoundLabels.nextPowerOfTwo(28)).isEqualTo(32);
        assertThat(RoundLabels.nextPowerOfTwo(32)).isEqualTo(32);
        assertThat(RoundLabels.nextPowerOfTwo(96)).isEqualTo(128);
    }

    @Test
    void roundCountDonneLeNombreDeTours() {
        assertThat(RoundLabels.roundCount(128)).isEqualTo(7);
        assertThat(RoundLabels.roundCount(32)).isEqualTo(5);
        assertThat(RoundLabels.roundCount(2)).isEqualTo(1);
    }

    @Test
    void libellesDesToursDUnTableauDe128() {
        assertThat(RoundLabels.labelFor(1, 7)).isEqualTo("R128");
        assertThat(RoundLabels.labelFor(4, 7)).isEqualTo("R16");
        assertThat(RoundLabels.labelFor(5, 7)).isEqualTo("QF");
        assertThat(RoundLabels.labelFor(6, 7)).isEqualTo("SF");
        assertThat(RoundLabels.labelFor(7, 7)).isEqualTo("F");
    }

    @Test
    void libellesDesQualifications() {
        assertThat(RoundLabels.qualifyingLabelFor(1)).isEqualTo("Q1");
        assertThat(RoundLabels.qualifyingLabelFor(3)).isEqualTo("Q3");
    }
}
