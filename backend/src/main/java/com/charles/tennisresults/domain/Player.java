package com.charles.tennisresults.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom de famille, tel qu'ecrit dans le fichier d'origine (souvent en majuscules). */
    @Column(nullable = false)
    private String lastName;

    /** Prenom. Peut etre inconnu (null) pour un joueur cree a la volee depuis un tableau. */
    private String firstName;

    private String nationality;

    /**
     * Points de classement importes depuis le fichier Excel au moment de l'import
     * (photo figee, purement informative - le classement "vivant" de l'appli, lui,
     * est toujours recalcule par RankingService a partir des tournois saisis dans
     * l'appli, et part de zero pour un tournoi qui n'a pas ete saisi ici).
     */
    private Integer legacySnapshotPoints;

    public Player(String lastName, String firstName, String nationality) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.nationality = nationality;
    }
}
