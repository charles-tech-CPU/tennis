package com.charles.tennisresults.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une case du tableau d'un tournoi : soit un joueur (avec eventuellement une tete
 * de serie ou un statut WC/Q/LL), soit un "bye" (player == null, bye == true) que
 * Charles place lui-meme aux positions qui n'ont pas de vrai participant.
 */
@Entity
@Table(name = "entry")
@Getter
@Setter
@NoArgsConstructor
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    /** Position dans le tableau du 1er tour (1..drawSlots), fixee a la main par Charles. */
    @Column(nullable = false)
    private Integer drawPosition;

    /** Tete de serie (1, 2, 3...). Null si non tete de serie. */
    private Integer seed;

    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    /** true si cette position est un "bye" (pas de joueur, avance automatiquement). */
    @Column(nullable = false)
    private boolean bye = false;
}
