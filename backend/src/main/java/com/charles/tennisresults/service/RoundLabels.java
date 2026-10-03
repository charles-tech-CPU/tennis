package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Tournament;

public final class RoundLabels {

    private RoundLabels() {}

    /** Plus petite puissance de 2 superieure ou egale a n. */
    public static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    /** Nombre de tours pour un tableau de drawSlots cases (drawSlots = puissance de 2). */
    public static int roundCount(int drawSlots) {
        return Integer.numberOfTrailingZeros(drawSlots);
    }

    /** Libelle du tour roundOrder (1-based) parmi totalRounds : "R32", "R16", "QF", "SF", "F". */
    public static String labelFor(int roundOrder, int totalRounds) {
        int remaining = totalRounds - roundOrder;
        if (remaining == 0) {
            return "F";
        }
        if (remaining == 1) {
            return "SF";
        }
        if (remaining == 2) {
            return "QF";
        }
        return "R" + (1 << (remaining + 1));
    }

    /** Libelle d'un tour de qualification (roundOrder 1-based) : "Q1", "Q2"... */
    public static String qualifyingLabelFor(int roundOrder) {
        return "Q" + roundOrder;
    }

    /**
     * Libelle du tour roundOrder deduit du tableau (qualifs ou taille du
     * tableau principal), quand le bareme du tournoi n'en fournit pas ; null
     * si la taille du tableau est inconnue.
     */
    public static String labelFor(Tournament t, int roundOrder) {
        if (t.isQualifying()) {
            return qualifyingLabelFor(roundOrder);
        }
        if (t.getDrawSize() == null) {
            return null;
        }
        return labelFor(roundOrder, roundCount(nextPowerOfTwo(t.getDrawSize())));
    }
}
