package com.charles.tennisresults.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un match d'une rencontre par equipes. Les joueurs sont du texte libre ("Arthur
 * Rinderknech", "Benjamin Bonzi / Pierre-Hugues Herbert") : beaucoup ne sont pas
 * dans la table player (joueuses de United Cup, specialistes du double), et ces
 * matchs ne comptent pas au classement. Le score est ecrit du point de vue
 * team1 - team2 de la rencontre ("6-7(4) 6-3 6-4").
 */
@Entity
@Table(name = "team_rubber")
@Getter
@Setter
@NoArgsConstructor
public class TeamRubber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tie_id", nullable = false)
    private TeamTie tie;

    @Column(nullable = false)
    private Integer rubberOrder;

    @Column(nullable = false)
    private boolean doubles;

    @Column(name = "team1_players")
    private String team1Players;

    @Column(name = "team2_players")
    private String team2Players;

    private String score;

    /** 1 ou 2, null si pas encore joue ou non dispute. */
    private Integer winner;

    /** PENDING, COMPLETED ou NOT_PLAYED (match sans enjeu non dispute). */
    @Column(nullable = false)
    private String status;
}
