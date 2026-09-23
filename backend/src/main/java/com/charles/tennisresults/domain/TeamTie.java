package com.charles.tennisresults.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une rencontre entre deux equipes nationales (Coupe Davis, United Cup). team1 /
 * team2 sont des noms de pays au format des nationalites joueurs ("FRANCE",
 * "HOLLANDE"...) pour reutiliser les drapeaux ; null tant que l'equipe n'est pas
 * connue (demi-finale a venir), avec alors un libelle d'attente dans
 * team1Placeholder / team2Placeholder ("Vainqueur QF1").
 */
@Entity
@Table(name = "team_tie")
@Getter
@Setter
@NoArgsConstructor
public class TeamTie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DAVIS_CUP ou UNITED_CUP. */
    @Column(nullable = false)
    private String competition;

    @Column(nullable = false)
    private Integer season;

    /** Phase : QUALIFIERS_R1, WORLD_GROUP_I_PO, QUALIFIERS_R2, FINALS_QF/SF/F, GROUP, QF, SF, F. */
    @Column(nullable = false)
    private String stage;

    /** Poule (United Cup uniquement). */
    private String groupName;

    /** Ordre dans la phase (1-based) ; sert aussi de place dans le tableau a elimination directe. */
    @Column(nullable = false)
    private Integer position;

    private String team1;
    private String team2;

    @Column(name = "team1_placeholder")
    private String team1Placeholder;

    @Column(name = "team2_placeholder")
    private String team2Placeholder;

    @Column(name = "team1_score", nullable = false)
    private Integer team1Score = 0;

    @Column(name = "team2_score", nullable = false)
    private Integer team2Score = 0;

    /** 1 ou 2, null tant que la rencontre n'est pas decidee. */
    private Integer winner;

    /** SCHEDULED ou COMPLETED. */
    @Column(nullable = false)
    private String status;

    private String dates;
    private String city;
    private String venue;
    private String surface;

    @OneToMany(mappedBy = "tie", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rubberOrder ASC")
    private List<TeamRubber> rubbers = new ArrayList<>();
}
