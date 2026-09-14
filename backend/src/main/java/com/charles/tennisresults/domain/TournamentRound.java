package com.charles.tennisresults.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un tour du tableau a elimination directe d'un tournoi, et les points attribues
 * a un joueur elimine (ou vainqueur, pour le dernier tour) a ce tour.
 * roundOrder = 1 pour le premier tour (le plus grand), jusqu'a N pour la finale.
 */
@Entity
@Table(name = "tournament_round")
@Getter
@Setter
@NoArgsConstructor
public class TournamentRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private Integer roundOrder;

    /** Libelle lisible : "R32", "R16", "QF", "SF", "F"... */
    @Column(nullable = false)
    private String roundLabel;

    @Column(nullable = false)
    private Integer points;

    public TournamentRound(Tournament tournament, Integer roundOrder, String roundLabel, Integer points) {
        this.tournament = tournament;
        this.roundOrder = roundOrder;
        this.roundLabel = roundLabel;
        this.points = points;
    }
}
