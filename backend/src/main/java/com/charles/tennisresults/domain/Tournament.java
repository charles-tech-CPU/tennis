package com.charles.tennisresults.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tournament")
@Getter
@Setter
@NoArgsConstructor
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentCategory category;

    /** Saison (annee) du tournoi. */
    @Column(nullable = false)
    private Integer season;

    /** Numero de semaine ATP (1 a 48+), informatif. */
    private Integer weekNumber;

    private String country;

    /**
     * Case "obligatoire" du classement que ce tournoi occupe pour cette saison
     * (uniquement pertinent pour GRAND_SLAM et MASTERS_1000). Null si le tournoi
     * ne compte que dans le pool des "autres" tournois.
     */
    @Enumerated(EnumType.STRING)
    private MandatorySlot mandatorySlot;

    /**
     * Nombre reel de participants au tableau principal (peut ne pas etre une
     * puissance de 2 : ex. 96 -> 32 "byes" pour les tetes de serie qui entrent
     * directement au 2e tour). Le tableau (bracket) est toujours genere avec
     * drawSlots = plus petite puissance de 2 superieure ou egale a drawSize.
     */
    @Column(nullable = false)
    private Integer drawSize;

    /** Points de qualification (informatif uniquement, non comptes dans le classement). */
    @Column(name = "qualifying_round1_points")
    private Integer qualifyingRound1Points;

    @Column(name = "qualifying_round2_points")
    private Integer qualifyingRound2Points;

    /**
     * Points du finaliste battu (perdant de la finale). Si null, on retombe sur les
     * points du tour precedent (demi-finale) - cf RankingService. A renseigner pour
     * etre fidele au bareme reel (le vainqueur et le finaliste n'ont jamais le meme
     * nombre de points dans le circuit ATP).
     */
    private Integer runnerUpPoints;

    /** True si ce tournoi EST un tableau de qualifications (relie via mainTournamentId). */
    @Column(name = "is_qualifying", nullable = false)
    private boolean qualifying = false;

    /** Renseigne uniquement sur le tableau de qualifications : id du tournoi principal. */
    @Column(name = "main_tournament_id")
    private Long mainTournamentId;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundOrder ASC")
    private List<TournamentRound> rounds = new ArrayList<>();
}
