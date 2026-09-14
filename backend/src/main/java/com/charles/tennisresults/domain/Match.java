package com.charles.tennisresults.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un match du tableau. roundOrder + positionInRound identifient sa place dans le
 * bracket : le vainqueur du match (tournament, roundOrder, positionInRound) alimente
 * automatiquement l'entree "slot1" ou "slot2" (selon la parite de positionInRound)
 * du match (tournament, roundOrder + 1, ceil(positionInRound / 2)).
 */
@Entity
@Table(name = "match_entry")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private Integer roundOrder;

    /** Position du match au sein de son tour (1-based). */
    @Column(nullable = false)
    private Integer positionInRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry1_id")
    private Entry entry1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry2_id")
    private Entry entry2;

    /** Score libre, tel qu'on le note a la main : "6-3 6-4", "7-6(4) 3-6 6-2", "w.o.", etc. */
    private String score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_entry_id")
    private Entry winnerEntry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;
}
