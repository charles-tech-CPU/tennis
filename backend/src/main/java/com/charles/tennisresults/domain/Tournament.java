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

    /** Surface de jeu, null si non renseignee. */
    @Enumerated(EnumType.STRING)
    private Surface surface;

    /** true = indoor, false = outdoor, null si non renseigne. */
    private Boolean indoor;

    /**
     * True une fois le classement de chaque joueur du tableau fige
     * (Entry.rankingAtEntry), au premier match saisi - voir EntryRankingService.
     */
    @Column(nullable = false)
    private boolean rankingsFrozen = false;

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

    /**
     * True pour un tournoi en poules (ATP Finals) : voir RoundRobinService.
     * Entrees 1-4 = poule A, 5-8 = poule B, 9+ = remplacants ; tour 1 = poules,
     * tour 2 = demi-finales, tour 3 = finale ; le bareme d'un tour = points
     * par victoire a ce tour (cumules).
     */
    @Column(name = "round_robin", nullable = false)
    private boolean roundRobin = false;

    /** Nom affiche des poules (ex. "John McEnroe"), null = "Groupe A" / "Groupe B". */
    @Column(name = "group_a_name")
    private String groupAName;

    @Column(name = "group_b_name")
    private String groupBName;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundOrder ASC")
    private List<TournamentRound> rounds = new ArrayList<>();
}
