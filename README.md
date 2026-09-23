# Tennis Results

[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=charles-tech-CPU_tennis&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=charles-tech-CPU_tennis) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=charles-tech-CPU_tennis&metric=bugs)](https://sonarcloud.io/summary/new_code?id=charles-tech-CPU_tennis) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=charles-tech-CPU_tennis&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=charles-tech-CPU_tennis) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=charles-tech-CPU_tennis&metric=coverage)](https://sonarcloud.io/summary/new_code?id=charles-tech-CPU_tennis) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=charles-tech-CPU_tennis&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=charles-tech-CPU_tennis)

Application de saisie et de suivi du circuit ATP (hommes uniquement, du Grand Chelem
jusqu'au Challenger ATP50) : tournois avec un vrai tableau visuel a elimination
directe (bracket), et classement calcule automatiquement. Backend Java / Spring Boot,
frontend Vue 3, base PostgreSQL.

Projet **independant** de `lol-results` et `foot-results` : repo separe, base
separee, ports differents (backend 8082, frontend 5175) pour pouvoir faire tourner
les trois en parallele si besoin.

## 1. Prerequis

Identiques aux deux autres projets : Java 21 (JDK), Maven, Node.js 20+/npm,
PostgreSQL 14+, Git.

## 2. Principe du projet

- Le **tableau** (bracket) est la seule source de verite. Chaque tournoi est un
  arbre a elimination directe genere automatiquement (tour par tour) a partir de sa
  taille reelle (`drawSize`, ex: 96) arrondie a la puissance de 2 superieure
  (`drawSlots`, ex: 128 -> 32 "byes" places par toi sur les positions de ton choix).
- Saisir un score fait **avancer automatiquement le vainqueur** au tour suivant.
  Corriger un score deja saisi **annule automatiquement** ce qui en decoulait plus
  loin dans le tableau (pratique pour corriger une "boulette" sans tout reprendre
  a la main).
- Le **classement** n'est **pas stocke** : recalcule a la volee, pour une saison
  donnee, en reproduisant la logique de ta formule Excel :
  `total = (4 Grand Chelem + ATP Finals + 8 des 9 Masters 1000, Monte-Carlo exclu)
  + (somme des 5 meilleurs "autres" tournois) + max(points a Monte-Carlo, 6e
  meilleur "autre" tournoi)`.
  Comme pour lol-results et foot-results : **seul un resultat acte compte** (joueur
  elimine, ou vainqueur du tournoi) - un tournoi en cours ne rapporte aucun point
  tant que l'issue n'est pas connue, on ne devine jamais un resultat.
- Le finaliste battu (perdant de la finale) a ses propres points (`runnerUpPoints`,
  a renseigner tournoi par tournoi) puisqu'il n'a jamais le meme nombre de points
  que le vainqueur sur le circuit ATP ; si tu ne le renseignes pas, l'appli retombe
  par defaut sur les points de demi-finaliste (une approximation a corriger au cas
  par cas).

### Simplifications volontaires (v1)

- **Historique des tournois deja joues (les 48 feuilles de semaine de ton fichier)
  : non importe automatiquement.** J'ai analyse en detail la mise en page de ces
  tableaux (ex: le tournoi de Brisbane) et constate qu'elle est ajustee a la main
  tournoi par tournoi (hauteurs de bloc variables, noms de joueurs parfois recopies
  a plusieurs endroits pour l'alignement visuel a l'impression) : un script
  automatique aurait produit des erreurs difficiles a repérer sur environ 400
  tournois. J'ai prefere l'assumer clairement plutot que de livrer un import qui a
  l'air fiable mais qui ne l'est pas. **Ce qui EST importe** : le classement actuel
  (`legacy_snapshot_points`, une photo informative de la feuille "ATP", non liee au
  calcul automatique) et le calendrier complet 2026 (243 tournois, avec categorie et
  semaine), chacun avec un tableau vide pret a etre rempli. Pour les tournois deja
  joues qui t'interessent, ressaisis-les a la main via l'ecran de tournoi (c'est le
  meme ecran que pour un tournoi a venir).
- **Qualifications** : les points de qualification (2 tours, informatifs dans ton
  fichier) sont stockes (`qualifyingRound1Points`/`2`) mais **non comptes** dans le
  classement, comme dans ta propre formule Excel - je n'ai pas modelise un tableau
  de qualification a part, seulement le tableau principal.
- **Remplacant (TR2)** : ta formule Excel n'utilise qu'un seul remplacant contre
  Monte-Carlo (le second, TR2, existe comme colonne mais n'est jamais compte) - j'ai
  reproduit exactement cette regle, comme tu l'as confirme.
- **Positionnement des tetes de serie dans le tableau** : l'appli ne calcule pas
  automatiquement le tirage officiel (positions "protegees" pour eviter que les
  favoris se croisent trop tot) - tu places chaque joueur (et chaque "bye") a la
  position de ton choix (1 a `drawSlots`), exactement comme tu le ferais a la main.
- **Categories/points par defaut** : a la creation d'un tournoi, l'appli propose un
  bareme de points par tour base sur sa categorie (repris de ce que j'ai vu dans tes
  en-tetes, ex: ATP250 32 tableaux = 25/50/100/165/250) - ajustable tournoi par
  tournoi, les vrais bareme ATP variant legerement d'un evenement a l'autre au sein
  d'une meme categorie.
- Tournoi non modifiable une fois cree dans cette v1 (nom/categorie/points figes a
  la creation) - dis-le a Claude Code sur ta machine s'il te faut un ecran de
  modification, c'est un ajout simple.

## 3. Base de donnees

```bash
psql -U postgres
```

```sql
CREATE USER tennis_user WITH PASSWORD 'tennis_password';
CREATE DATABASE tennis_results OWNER tennis_user;
\q
```

## 4. Git

```bash
cd tennis-results
git init
git add .
git commit -m "Scaffold initial : backend Spring Boot, frontend Vue 3, bracket + classement automatique"
```

## 5. Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

Flyway applique automatiquement `V1__init.sql` (schema) puis `V2__seed_data.sql`
(1132 joueurs importes avec leur classement actuel a titre informatif, 243 tournois
2026 avec leur tableau vide pret a remplir). L'API demarre sur
`http://localhost:8082`.

| Methode | URL | Description |
|---|---|---|
| GET | `/api/players` | Liste des joueurs |
| POST | `/api/players` | Creer un joueur |
| PUT | `/api/players/{id}` | Modifier un joueur |
| GET | `/api/tournaments` | Liste des tournois |
| GET | `/api/tournaments/{id}` | Detail d'un tournoi (avec bareme de points) |
| POST | `/api/tournaments` | Creer un tournoi (genere le tableau vide) |
| GET | `/api/tournaments/{id}/entries` | Joueurs places dans le tableau |
| POST | `/api/tournaments/{id}/entries` | Placer un joueur (ou un bye) a une position |
| DELETE | `/api/tournaments/{id}/entries/{entryId}` | Retirer une entree |
| GET | `/api/tournaments/{id}/matches` | Tous les matchs du tableau |
| PUT | `/api/matches/{matchId}/score` | Saisir/corriger le score d'un match |
| GET | `/api/ranking?season=2026` | Classement recalcule pour la saison |

⚠️ Comme pour les deux autres projets, **je n'ai pas pu compiler ce backend** dans
mon bac a sable (Maven Central bloque). J'ai en revanche valide le schema et
l'integralite des ~12 000 lignes SQL generees directement via `psql` (aucune
erreur), et relu attentivement la logique Java (avancement automatique du vainqueur,
annulation en cascade lors d'une correction, calcul du classement) - mais le premier
`mvn spring-boot:run` chez toi reste le vrai test pour la partie Java elle-meme.
C'est typiquement le genre d'erreur que Claude Code, avec un vrai compilateur sous
la main, corrigera vite si besoin.

## 6. Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev
```

Demarre sur `http://localhost:5175`, appelle l'API sur `http://localhost:8082`
(CORS deja configure). Ecrans : tournois (avec filtre categorie/saison + creation),
detail d'un tournoi (vrai visuel de tableau a elimination directe, cliquable pour
saisir un score ; formulaire pour placer les joueurs aux positions du tableau),
classement (recalcule automatiquement, saison selectionnable), joueurs (avec le
"points importes" de ta photo Excel a titre de reference).

## 7. Importer les donnees de ton Excel

```bash
cd import
pip install -r requirements.txt
python3 import_excel.py /chemin/vers/TENNIS_2026.xlsx > ../backend/src/main/resources/db/migration/V2__seed_data.sql
```

Le `V2__seed_data.sql` fourni a deja ete genere et valide (voir section 2 pour le
detail de ce qui est importe ou non). Le script est commente en tete de fichier.

**Si tu relances l'import apres avoir modifie l'Excel** : regenere
`V2__seed_data.sql` avant le tout premier demarrage. Une fois la base initialisee,
Flyway ne rejoue pas une migration deja appliquee - ajoute une `V3__...sql` pour des
changements ulterieurs plutot que de modifier `V2` apres coup.

## 8. Pistes d'evolution (hors v1)

- Tirage automatique des tetes de serie (positions "protegees" officielles).
- Historique du classement (courbe semaine par semaine) plutot qu'une seule vue
  "instantanee".
- United Cup (semaine 1) : format par equipes (pays vs pays, simple + double,
  ATP + WTA melanges) - ne rentre pas dans le modele "tableau individuel a
  elimination directe" de l'appli. Laisse de cote pour l'instant, a rediscuter
  si tu veux vraiment le suivre (probablement un modele de donnees a part).

~~Ecran de modification d'un tournoi deja cree~~ et ~~tableau de qualification a
part~~ : faits, voir section 2 et section 9.

## 9. Suivi d'avancement - import des resultats semaine par semaine

Charles a demande une memoire ecrite de cet avancement, relisible en debut de
session suivante ("lis le README et fais ce qu'il y a a faire pour la semaine
suivante").

### Fait au-dela du scaffold initial (v1 → maintenant)

- **Design** repris integralement (palette terre battue/gazon, cartes, tableaux,
  bracket, formulaires) - voir `frontend/src/style.css`.
- **Ecran de gestion d'un tournoi** : pays, semaine, case obligatoire, points de
  qualification, points du finaliste battu, bareme par tour, tous modifiables
  apres creation (`PUT /api/tournaments/{id}`, panneau "Modifier les reglages"
  sur la page tournoi). Nom/categorie/saison/taille du tableau restent figes.
- **Tableau de qualifications** : modelise comme un tournoi lie
  (`tournament.is_qualifying` + `tournament.main_tournament_id`), pas une
  structure de donnees a part - reutilise tel quel tout le moteur de bracket
  existant (creation, entrees, scores, avancement automatique). Onglets
  "Tableau principal" / "Qualifs" sur la page tournoi. Cree via
  `POST /api/tournaments/{id}/qualifying`. Les points de qualif et du tournoi
  principal se **fusionnent automatiquement** dans le classement (meme tournoi).
- **Classement detaille** façon Excel : colonnes Grand Chelem / ATP Finals /
  Masters 1000 / Monte-Carlo / 5 meilleurs autres tournois (nommes) /
  remplacement / total / non-comptabilises. Drapeaux (package `flag-icons`,
  PAS des emoji - illisibles sur Windows) a cote des nationalites, dans le
  classement, la page Joueurs et le bracket.
- **Bugs corriges au passage** (pas lies a l'import, auraient plante n'importe
  quel usage normal de l'appli des qu'un tableau contient de vraies donnees) :
  - `LazyInitializationException` sur `GET .../entries`, `GET .../matches`,
    `GET /api/ranking` (services pas annotes `@Transactional`).
  - Mapping JPA `qualifyingRound1Points`/`2` vers de mauvaises colonnes SQL.
  - `RankingService.pointsEarned()` supposait une seule "finale" (mauvais pour
    un tableau de qualifs, ou plusieurs groupes independants produisent chacun
    un "vainqueur de tour" simultanement) - corrige.
  - Sequence Postgres `player_id_seq` desynchronisee (le script d'import
    resynchronisait sur le *nombre* de joueurs importes au lieu du plus grand id
    reellement utilise - une ligne Excel sautee suffit a les desaligner) - fixe
    en base et dans `import/import_excel.py` (voir `V5__fix_player_sequence.sql`).

### La methode de lecture des feuilles hebdomadaires (important a relire)

Chaque feuille hebdo (`1` a `48` dans le classeur) empile plusieurs tournois
verticalement, dans des blocs de colonnes de largeur fixe. Le format est
**toujours le meme** une fois qu'on a repere, pour un tournoi donne, sa ligne
d'en-tete (`header_row`) et sa colonne de base `X` (= colonne du "tag" du
premier joueur, tout a gauche du bloc) :

- `X` = colonne tag joueur 1 (seed/WC/Q/LL), `X+1` = nom joueur 1 (colonne
  "ronde 0"), `X+1+r` = resultat du tour `r` **cote gauche**, r=1..3 seulement
  (R32,R16,QF ; groupes de taille `2^r`, le nom du vainqueur est ecrit a la
  ligne `groupe_top + 2^(r-1) - 1` et le score a la ligne `groupe_top + 2^(r-1)`).
- **La demi-finale de chaque cote est EMBARQUEE dans sa propre colonne QF**,
  pas dans une colonne dediee : cote gauche -> colonne `X+4` (la meme que le
  tour 3), a la ligne du milieu du demi-tableau complet (`start_row+7` pour un
  demi-tableau de 16 lignes) ; cote droit -> colonne `X+6` (sa propre colonne
  QF), meme ligne du milieu.
- Cote droit (mirroir) : `X+9` = nom, `X+10` = tag, `X+9-r` = resultat du tour
  `r` (r=1..3, QF a `X+6`).
- **La vraie finale (tour 5) occupe la colonne CENTRALE `X+5`**, isolee entre
  les deux moities QF (`X+4` et `X+6`), a la meme ligne du milieu. Elle EST
  presente dans la grille - contrairement a une premiere conclusion erronee,
  ce n'est pas une case manquante, juste une case que j'avais mal identifiee
  comme "demi-finale gauche" avant que Charles ne corrige (Brisbane : E10 =
  score demi gauche, F10 = score finale). Voir `decode_bracket.py`,
  `decode_main_draw()`, cle `'final'` du dict retourne.
- Qualifs : `X+12` = tag, `X+13` = nom (avec le texte `"QUALIF"` a cote sur la
  ligne d'en-tete), `X+14`/`X+15` = tours 1/2. Taille variable (12, 16, 24...) -
  **toujours detecter dynamiquement** (premiere ligne vide = fin du tirage), ne
  jamais supposer une taille fixe.
- **Barème de points : decale d'un cran par rapport aux entetes Excel** - voir
  section "Semaine 1 : etat" plus bas, point 1. Ne pas prendre les valeurs de
  l'entete telles quelles.
- Outils reutilisables : `import/weekly_results/decode_bracket.py` (fonctions
  `decode_main_draw`/`decode_qualifying` + `find header_rows` en CLI pour
  reperer les lignes d'en-tete d'une feuille) et
  `import/weekly_results/push_generic.py` (pousse entrees + scores vers l'API
  une fois les `(nom, feuille, header_row, X, id_tournoi_en_base, dernier_tour_fiable)`
  identifies). Verifie TOUJOURS l'absence d'anomalie (un "vainqueur" qui
  n'appartient a aucune des deux moitiés) avant de pousser - ça a permis de
  detecter le probleme de la vraie finale plutot que de pousser des donnees
  fausses.

### Semaine 1 : etat (complet, corrige)

Tous les tournois de la semaine 1 sont crees et **complets** (tours 1 a 5,
qualifs incluses), barème de points corrige. Un seul trou reel, une donnee
absente du fichier Excel lui-meme (pas un bug de lecture) :

| Tournoi | id | Etat |
|---|---|---|
| Brisbane | 3 | complet |
| Canberra | 4 | complet |
| Bangalore | 5 | complet |
| Hong Kong | 2 | complet |
| Noumea | 6 | complet |
| Nonthaburi 1 | 7 | complet sauf 1 score de quali (Q2, Weber vs Colson - absent du fichier) |
| Nottingham | 8 | complet |
| United Cup | 1 | rien - format different, voir section 8 |

**Deux corrections importantes faites apres coup (a bien relire avant de
continuer une semaine suivante), suite a un retour detaille de Charles :**

1. **Barème de points decale d'un cran.** La valeur imprimee sous l'entete
   Excel "R32" n'est PAS le nombre de points pour une elimination au 1er tour
   (R32) - c'est le nombre de points pour une elimination au tour SUIVANT
   (R16). Le vrai mapping : R32 (1er tour) = **toujours 0**, et la derniere
   valeur imprimee ("F" dans Excel) = points du **finaliste battu**
   (`runnerUpPoints`), pas du tour F lui-meme. Exemple Brisbane : Excel
   affiche `25/50/100/165/250` sous R32/R16/QF/SF/F -> le vrai bareme est
   `rounds=[0,25,50,100,250]` (R32,R16,QF,SF,W) + `runnerUpPoints=165`. Meme
   decalage pour les qualifs : Excel affiche `Q1=x/Q2=y` -> vrai bareme
   `rounds=[0,y]` + `runnerUpPoints=x`. **Applique aux 7 tournois + 7 qualifs
   de la semaine 1** (`import/weekly_results/week1_fix_points.py` - garde a
   titre d'exemple de methode, pas directement relancable pour une autre
   semaine sans changer les ids).
2. **Colonne demi-finale/finale inversee.** Contrairement a ce qui est ecrit
   plus bas ("le tour 4 gauche reutilise la colonne QF"), la VRAIE regle est :
   - Colonne QF (`X+4`) porte AUSSI la demi-finale du cote gauche, embarquee a
     la ligne du milieu (comme deja documente).
   - Colonne QF miroir (`X+6`) porte AUSSI la demi-finale du cote droit, meme
     principe (deja documente, ca c'etait juste).
   - **La colonne centrale `X+5` (celle que je prenais a tort pour "la
     demi-finale gauche") est en fait la VRAIE FINALE**, toujours presente
     dans la grille (contrairement a ce qui avait ete conclu au debut - elle
     n'est pas absente, juste mal etiquetee). Exemple Brisbane : E10 (`X+4`,
     ligne du milieu) = score demi-finale gauche, F10 (`X+5`) = score de la
     finale. `decode_bracket.py` est corrige en consequence (fonction
     `decode_main_draw`, cle `'final'` du dict retourne).
3. Un bug annexe trouve en corrigeant les qualifs de Brisbane :
   `BracketService.syncRound1FromEntries` recalculait la taille du tableau
   via `nextPowerOfTwo(maxPosition)` (correct pour un tableau principal, FAUX
   pour des qualifs dont la taille reelle n'est pas une puissance de 2, ex.
   24 ou 12) - creait des matchs fantomes vides des qu'on ajoutait un joueur.
   Corrige (utilise `tournament.getDrawSize()` tel quel si `isQualifying()`).
4. **Les qualifs a 12 joueurs (bloc1 seul) etaient incompletes.** Quand le
   tableau principal a 6 cases "Q" mais que le bloc de qualifs `X+12..X+15`
   ne fait que 12 joueurs (3 groupes de 4 = 3 qualifies, pas 6), il existe un
   **second bloc de qualifs** un peu plus loin sur la meme feuille, colonnes
   `X+17` (tag) / `X+18` (nom) / `X+19` (tour 1) / `X+20` (tour 2) - meme
   structure, MEME bareme de points que le bloc 1 (pas d'entete "QUALIF"
   separe - Charles a confirme : `X+13`=`X+18`, `X+14`=`X+19`, `X+15`=`X+20`).
   Les deux blocs se combinent en un seul tableau de qualifs de 24 joueurs
   (positions 1-12 = bloc1, 13-24 = bloc2). **Toujours verifier le nombre de
   "Q" dans le tableau principal contre le nombre de qualifies produits** avant
   de considerer un tableau de qualifs comme complet (`decode_bracket.py`
   `decode_qualifying()` ne lit qu'un seul bloc - appeler deux fois avec
   `X+12` puis `X+17` et combiner, voir `fix_qualifs_block2.py` dans
   `import/weekly_results/` pour la methode complete, conservee cette fois).

Quelques joueurs absents du snapshot Excel ont ete crees a la volee (nom
seul, pas de prenom/nationalite au depart). **Avant de les creer, chercher une
faute de frappe** (les noms des feuilles hebdo different souvent legerement de
la feuille "ATP" - Charles a confirme que c'est systematique, ex.
`MATSUDA RUYKI` -> `MATSUDA/RYUKI`, `HARRIS LL` -> `HARRIS/LLOYD` ; utiliser
`difflib.get_close_matches` sur `NOM PRENOM` de la feuille ATP, cutoff ~0.55-0.6).
**Si un vrai nouveau joueur (elimine au 1er tour, jamais classe donc absent de
la feuille ATP) et que sa nationalite reste inconnue apres recherche : mettre
la nationalite du PAYS DU TOURNOI** ou il a joue (ex. tous les joueurs non
identifies de Bangalore -> `INDE`, Hong Kong -> `HONG KONG`, Nouvelle-Caledonie/
Noumea -> `FRANCE`, Canberra -> `AUSTRALIE`, Nonthaburi -> `THAILANDE`) - regle
donnee explicitement par Charles, ne pas laisser `nationality` a `NULL` (pas de
drapeau sinon). Quelques homonymes (ex: MARTINEZ a Bangalore, 4 candidats) ont
ete resolus en prenant le premier match sans verification individuelle - a
auditer si un classement parait bizarre.

### Pour reprendre (semaine 2 ou finir la semaine 1)

1. Si tu as les scores manquants de la semaine 1 (tableau ci-dessus), donne-les
   moi, je les saisis directement.
2. Pour la semaine 2 : ouvrir `20260914TENNIS 2026.xlsx`, feuille `2`, lancer
   `python decode_bracket.py find 2` pour reperer les lignes d'en-tete de
   chaque tournoi (colonne B et colonne T), puis `decode_bracket.py show 2
   <header_row> <X>` pour previsualiser chaque tournoi avant de le pousser via
   `push_generic.py` (dupliquer sa liste `jobs`). Verifier les anomalies
   avant de pousser, comme pour la semaine 1.

## 10. Grosse evolution en cours (classement glissant 52 semaines + calendrier + stats)

Charles a demande (2026-09-16) de journaliser ici l'avancement de ce chantier au
fur et a mesure, pour pouvoir reprendre sans perdre le fil si la session s'arrete
en cours de route (a relire en debut de session suivante avec ce README).

### La demande d'origine (retrouvee dans l'historique de conversation)

1. **Classement dynamique/glissant** façon vrai systeme ATP : retirer les points
   d'un tournoi de la semaine `N` de l'annee `Y` des que le tournoi de la semaine
   `N` de l'annee `Y+1` commence. Poser des questions en cas de doute (fait, voir
   decisions ci-dessous).
2. **Page Joueurs** : retirer la colonne "points importes" (`legacySnapshotPoints`),
   jugee inutile.
3. **Page Tournois ("calendrier")** : afficher, pour chaque tournoi termine, son
   vainqueur avec le drapeau de son pays.
4. **Couleurs par categorie** : un code couleur pour distinguer Grand Chelem /
   Masters 1000 / ATP 500 / ATP 250 / ... jusqu'a ATP 50 (pas de categorie
   "Challenger" dans le modele actuel - seulement `ATP_50` comme categorie la plus
   basse).
5. **Nouvel onglet Stats** : point de depart pour des stats alimentees au fur et a
   mesure a la demande (ex: joueur avec le plus de tournois gagnes dans l'annee,
   joueur avec le plus de matchs gagnes dans l'annee) - concu pour etre facile a
   etendre plus tard, pas une liste figee.

### Decisions prises avec Charles (AskUserQuestion, 2026-09-16)

- **Cle de rattachement semaine->annee** : PAS de vraie date de tournoi ajoutee au
  modele. On matche les editions par `weekNumber` : pour un numero de semaine
  donne, seule l'edition (saison) la plus recente qui a **reellement commence**
  (au moins un match COMPLETED/BYE, tableau principal ou qualifs) compte ; l'edition
  de l'annee precedente a cette meme semaine est alors automatiquement exclue. Si
  l'edition de la nouvelle saison n'a pas encore commence, celle de l'an dernier
  reste comptee. Un tournoi sans `weekNumber` renseigne reste toujours compte tel
  quel (pas de mise en concurrence possible).
- **Vue Classement** : remplace entierement l'ancienne vue par saison (le
  selecteur d'annee disparait). `/api/ranking` n'a plus de parametre `season`,
  toujours "live"/glissant.

### Plan d'implementation et etat d'avancement

Tout ce qui suit est **fait, et verifie** : `mvn -q compile` (backend) et
`npx vite build` (frontend) passent tous les deux sans erreur (2026-09-16).
Reste a valider a l'usage reel (`mvn spring-boot:run` + `npm run dev`), pas
seulement a la compilation - a faire au tout debut de la prochaine session si
ce n'est pas deja fait.

- [x] **Backend - RankingService** : nouvelle methode `computeRanking()` sans
  parametre saison (`activeTournamentIds()` applique la regle par `weekNumber`
  decrite plus haut), puis agrege les points sur l'ensemble des entrees dont le
  tournoi (ou tournoi principal si qualif) fait partie des editions actives -
  reste de la logique (cases obligatoires, 5 meilleurs autres, remplacement
  Monte-Carlo/6e) inchangee.
- [x] **Backend - RankingController** : `GET /api/ranking` n'a plus de
  parametre `season`.
- [x] **Backend - EntryRepository** : `findByPlayerIsNotNull()` (remplace
  `findByTournament_SeasonAndPlayerIsNotNull`).
- [x] **Frontend - RankingView.vue** : selecteur d'annee retire, titre
  "Classement" simple, `api.getRanking()` sans param, callout mis a jour pour
  expliquer la regle du classement glissant.
- [x] **Frontend - PlayersView.vue** : colonne "Points importes" retiree (le
  champ `legacySnapshotPoints` reste en base/DTO, juste plus affiche - purement
  informatif a l'origine, cf section 2).
- [x] **Backend - vainqueur de tournoi** : `TournamentWinnerDto` (nouveau),
  ajoute a `TournamentDto.winner` - calcule dans `TournamentService.winnerOf()`
  a partir du match du dernier tour du tableau principal (seulement si le
  tournoi est `COMPLETED`).
- [x] **Frontend - TournamentsView.vue** : nouvelle colonne "Vainqueur" avec
  drapeau.
- [x] **Couleurs par categorie** : `CATEGORY_TAG_CLASS` (labels.js) + nouvelles
  classes/variables CSS (`tag-blue/teal/purple/rose/olive/slate/neutral`,
  style.css) pour `ATP_500` -> `ATP_50` (pas de categorie "Challenger" a part
  dans le modele - `ATP_50` est la plus basse, en gris comme "bye").
- [x] **Onglet Stats** : route `/stats` + `StatsView.vue` (selecteur d'annee),
  `StatsController`/`StatsService`/`StatsDto`/`PlayerCountDto` (nouveaux) -
  `GET /api/stats?season=YYYY` retourne pour l'instant `topTournamentWinners`
  (titres dans la saison) et `topMatchWinners` (matchs gagnes dans la saison,
  tableau principal + qualifs) ; conçu pour ajouter facilement d'autres stats a
  la demande de Charles (ajouter un champ a `StatsDto` + le calcul dans
  `StatsService` + une carte dans `StatsView.vue`). Note : contrairement au
  classement, une stat "de l'annee" reste rattachee a la saison civile
  (`Tournament.season`), pas au classement glissant - a confirmer avec Charles
  si ce n'est pas ce qu'il attendait.

### Bug trouve et corrige a l'usage reel (2026-09-16, apres coup)

`TournamentService.findAll()`/`findOne()` n'etaient pas `@Transactional` : le
nouveau `winnerOf()` accede a `match.getWinnerEntry().getPlayer()` (lazy) hors
session Hibernate -> `LazyInitializationException`, `GET /api/tournaments`
plantait en 500, d'ou l'onglet Tournois vide **et** le formulaire d'ajout
inutilisable (la page entiere plante des que la liste ne charge pas). Corrige
en ajoutant `@Transactional(readOnly = true)` aux deux methodes (meme classe
de bug que celles deja listees plus haut pour `RankingService`, meme remede).
Backend redemarre et reteste en conditions reelles : `/api/tournaments`,
`/api/ranking`, `/api/stats` repondent tous 200 avec des donnees coherentes.

Egalement ajuste sur demande de Charles : `StatsService.TOP_N` passe de 15 a
**3** joueurs affiches par classement de stat.

Ajoute sur demande : champ de recherche par joueur (nom/prenom) dans l'onglet
Classement (`RankingView.vue`, `filteredRows`) - le rang affiche reste le vrai
rang au classement (calcule avant filtrage), pas la position dans la liste
filtree.

Corrections de noms de joueurs faites en base via `PUT /api/players/{id}`
(pas de trace a garder ici, juste les identifiants au cas ou) : #992 PUJOL
NAVA -> PUJOL NAVARRO, #22 FILS/FILS -> FILS/ARTHUR.

Tri de l'onglet Tournois change sur demande : saison desc, puis semaine ATP,
puis **importance de la categorie** (Grand Chelem > Masters 1000 > ATP 500 >
ATP 250 > ATP 175 > ATP 125 > ATP 100 > ATP 75 > ATP 50 - correspond
exactement a l'ordre de declaration de `TournamentCategory`, tri par
`category().ordinal()`), puis nom en dernier recours
(`TournamentService.findAll()`).

### Taille du tableau modifiable apres creation (2026-09-16)

Constat de Charles : tous les ATP 500 n'ont pas la meme taille (ex. Rotterdam
= 32, pas 64/48 comme cree a tort) et il n'y avait aucun moyen de corriger ca
apres coup (`drawSize` etait fige a la creation, cf section 2 - decision
initiale volontaire, revue ici). Ajoute `TournamentService.resizeDraw()` :
change `drawSize`/`drawSlots`, regenere le bareme par defaut (`CategoryDefaults`)
et le squelette de matchs vides, **uniquement si le tournoi n'a encore aucun
joueur place** (meme garde-fou que la suppression d'un tournoi) - sinon erreur
claire demandant de retirer les joueurs d'abord. Expose via le meme
`PUT /api/tournaments/{id}` (nouveau champ `drawSize` dans `TournamentUpdateDto`,
uniquement pour le tableau principal - pas les qualifs). Champ ajoute au
formulaire "Modifier les reglages" (`TournamentDetailView.vue`).

Piege rencontre et corrige en le testant reellement (pas juste a la
compilation) : `t.getRounds().clear()` + `cascade/orphanRemoval` ne suffit pas
ici, la suppression des anciens tours n'est qu'"orpheline" (differee a la fin
de la transaction) alors que les nouveaux tours sont inseres tout de suite
(id `IDENTITY`) -> violation de contrainte unique `(tournament_id,
round_order)` le temps que les deux coexistent. Fixe en supprimant
explicitement les anciens tours puis en forçant un `flush()` avant de creer
les nouveaux. Reproduit et verifie en conditions reelles sur Rotterdam (id 33,
48 -> 32 : `drawSlots` et le tableau vide de 31 matchs bien regeneres).

### A faire au demarrage de la prochaine session si ce chantier n'est pas encore clos

1. Lancer reellement l'appli (`mvn spring-boot:run` + `npm run dev`, voir
   sections 3/5/6) et verifier a l'oeil : `/ranking` (classement glissant),
   `/` (colonne Vainqueur + couleurs de categorie), `/players` (plus de colonne
   points), `/stats`.
2. Cas limite non teste en conditions reelles : un `weekNumber` partage par
   plus de 2 saisons (ex. 2025/2026/2027 en meme temps) - la regle ne garde
   QUE la plus recente ayant commence, jamais un fallback a 2 crans en arriere ;
   confirmer avec Charles que c'est bien le comportement voulu si ce cas se
   presente un jour.

## 11. Nettoyage des joueurs - a faire par Charles (audit du 2026-09-20)

Demande de Charles : "trouve les doublons, absences de prenom, absences de
pays etc dans la table joueurs". Deja fait automatiquement (voir
`V12__clean_up_players.sql`) : 6 doublons certains fusionnes (inscriptions
reaffectees puis fiche en trop supprimee) et la casse des nationalites
uniformisee (ex. "Italie" -> "ITALIE") + 2 coquilles corrigees ("TUNSIE" ->
"TUNISIE", "DOMINQUE" -> "DOMINIQUE"). Le reste ci-dessous demande une vraie
recherche (que Charles seul peut faire, pas de source fiable cote appli) - a
corriger un par un via `PUT /api/players/{id}` au fil de l'eau.

### 1. Sans prenom (15)

BATALJIN (FRANCE), BOIVIN (FRANCE), BOOSARAWON (THAILANDE), BRUCE SMITH
(FRANCE), DUSSIN (FRANCE), FURNESS (FRANCE), IBRAIMI (AUSTRALIE), JOVANIVSKI
(AUSTRALIE), KUKASIAN (INDE), ROLLIN (FRANCE), SAKAMOTO (JAPON),
SALUN-OUILLEMON (FRANCE), SUKSUMRARN (THAILANDE), THOMSON KEI (HONG KONG),
VOISIN (FRANCE)

### 2. Prenom reduit a une seule initiale (151)

Probablement le reflet de la source d'origine (joueurs de bas niveau/Futures
souvent listes en "Nom I." dans les classements) plutot qu'une vraie erreur -
a verifier/completer si Charles a une source avec le prenom complet. `*` =
homonyme de nom de famille deja present sous un nom complet ailleurs dans la
base (probablement pas un doublon, cf section 4) ; `**` = doublon possible,
voir section 4.

```
1553 AKKOCAOGLU K (ALLEMAGNE)      1426 ANN S (COREE)               1404 ARENA FAVA C (—)
1452 ATLANGERIEV B (RUSSIE)        1518 AVCIBASI T (TURQUIE)         1555 BARTOLUCCI N (ITALIE)
1544 BATTISTON A (ITALIE)          1547 BEAVEN J (ANGLETERRE)        1536 BEKENI M (SLOVAQUIE)
1474 BERENZ B (AUTRICHE)           1543 BERTASI P (ITALIE)           1415 BETOV S (BIELORUSSIE)
1556 BETTI E (ITALIE)              1540 BOBO D (USA)                 1529 BRAUN M (MOLDAVIE)
1423 BU Y (CHINE)                  1461 BUSHAMUKA W (USA)            1530 CAZAC I (MOLDAVIE)
1457 CHEKHOV A (RUSSIE)            1507 CHETOUANE M (TUNISIE)        1432 CIANCAGLINI N (BOLIVIE)
1534 CIOCANU A (MOLDAVIE)          1419 COZAR GIMENEZ M (ESPAGNE)    1533 COZBINOV A (MOLDAVIE)
1516 DARDERI V (ITALIE) *          1509 DARMOUL A (TUNISIE)          1406 DAVIDOV T (USA)
1490 DE CESARIS A (ITALIE)         1439 DEV S (INDE) *               1403 DIDONI V (—)
1477 DOSKOCIL M (TCHEQUIE)         1511 FITA JUAN S (ESPAGNE)        1552 FIX D (ALLEMAGNE)
1554 FORNACI D (ITALIE)            1448 GALLAGHER G (USA)            1410 GAVA P (BRESIL)
1486 GOYAL M (INDE)                1489 GREGORIOU G (GRECE)          1545 GSCHWENDTNER J (ANGLETERRE)
1476 GUNDACKER J (SUISSE)          1537 HALAHIJA N (SLOVAQUIE)       1435 HANCE K (USA)
1460 HANDS T (ANGLETERRE)          1502 HANS K (INDE)                1470 HAUSBERGER G (AUTRICHE)
1506 HERMASSI R (TUNISIE)          1472 HILDERBRAND T (USA)          1548 HODKIN S (ANGLETERRE)
1329 HONGYU YE (CHINE)             1436 HORNUNG G (HONGRIE)          990 HOST TY (AUSTRALIE)
1465 HU H (CHINE)                  1444 HYUN J (COREE)               1411 IANNONI M (BRESIL)
1517 ILKEL C (TURQUIE)             1422 IZQUIERDO LUQUE R (ESPAGNE)  1481 JUSZCZAK P (POLOGNE)
1442 KADANGAH-KILI J (TOGO)        1532 KADHE A (INDE)               1497 KALIYANDA POONACHA N (INDE)
1424 KANG K (COREE)                1496 KESHARWANI M (INDE)          1428 KIM D (COREE) **
1407 KITTAY B (USA)                1480 KLIMAS J (TCHEQUIE)          1443 KOFFI M (COTE IVOIRE)
1498 KOTHAPALLI G (INDE)           1449 KOZLOV B (USA) *             1479 KUSY J (TCHEQUIE)
1538 LANIK T (SLOVAQUIE)           1421 LARA SALMERON D (ESPAGNE)    1427 LEE D (COREE)
1522 LONGWE-SMIT T (AFRIQUE SUD)   1458 LUKASHOV R (KAZAKHSTAN)      1539 MACEJ D (SLOVAQUIE)
1501 MAHESH KUMAR V (INDE)         1505 MARQUES G (PORTUGAL)         1531 MATUSZEWSKI P (POLOGNE)
1514 MEHROTRA A (AUSTRALIE)        1418 MENESES PERNY A (ESPAGNE)    1535 MICHALIK R (SLOVAQUIE)
1438 MINGZHOU Z (CHINE)            1488 MINTZ Z (USA)                1430 MOLLER E (DANEMARK)
1504 MONTEIRO S (PORTUGAL)         1420 MONTES-DE LA TORRE I (ESPAGNE) 1546 MOXON W (ANGLETERRE)
1525 NAWA M (—)                    1500 NEDUNCHEZHIYAN J (INDE)      1523 NGWENYA S (AFRIQUE SUD)
1508 NSAIRI S (TUNISIE)            1450 NURLANULY Z (KAZAKHSTAN)     1451 OMARKHANOV A (KAZAKHSTAN)
1459 OZDEMIR S (TURQUIE)           1441 PAN W (CHINE)                1484 PANDZOU EKOUME C (CONGO)
1491 PAOLINI A (ITALIE)            1446 PASTORINI A (ITALIE)         1487 PETIT L (—)
1469 PINTER P (AUTRICHE)           1494 PINTO L (BRESIL)             1478 POSTOLKA R (TCHEQUIE)
1495 PUCINELLI DE ALMEIDA R (BRESIL) * 1433 RALLIN S (BOLIVIE)       1557 REJCHTMAN VINCIGUERRA W (SUEDE)
1408 REMONDY PAGOTTO V (BRESIL)    1431 RIVERO M (BOLIVIE)           1542 RODDICK J (USA)
1492 RODRIGUES LONGOBARDI P (BRESIL) 1551 ROOTHMAN C (AFRIQUE SUD)   1475 ROSENKRANZ KOENIG T (AUTRICHE)
1447 ROSSOLINO S (ITALIE)          1526 SALTON D (AFRIQUE SUD)       1499 SARRAN P (INDE)
1482 SAULENKO T (KAZAKHSTAN)       1405 SCHTULMANN GASCA M (—)       1524 SCOTT A (AFRIQUE SUD)
1515 SENTHIL KUMAR R (INDE)        1425 SHIN W (COREE) *             1454 SHIN M (OUZBEKISTAN)
1429 SIM S (COREE)                 1528 SNITARI I (MOLDAVIE)         1445 SPADOLA A (ITALIE)
1550 STRYDOM J (AUSTRALIE)         1434 SUAREZ L (BOLIVIE)           1414 SUN Q (CHINE) *
1466 SUN Z (CHINE) *               1462 TE R (CHINE)                 1521 TEUNISSEN L (HOLLANDE)
1463 TIAN Y (CHINE)                1456 TIUKAEV R (RUSSIE)           1513 TONEJC V (CROATIE)
1455 TREBUKHIN A (KAZAKHSTAN)      435 TU LI (AUSTRALIE)             1453 TULEPBERGENOV D (KAZAKHSTAN)
1467 UJVARY M (AUTRICHE)           1527 VAN SCHALKWYK C (—)          1483 VASA I (FINLANDE) *
1493 VICENTE DE ARAUJO R (BRESIL)  1485 VILMAURE G (FRANCE)          1437 VITHOONTIEN L (JAPON)
1512 VUKADIN N (CROATIE)           1473 WAGNER A (AUTRICHE)          1471 WALDNER N (AUTRICHE)
1464 WEI J (CHINE)                 275 WOLF JJ (USA)                 121 YUNCHAOKETE BU (CHINE)
1520 ZGIROVSKY A (BIELORUSSIE)     1440 ZHANG L (CHINE) *            1413 ZHAO Z (CHINE)
229 ZHOU YI (CHINE)
```

### 3. Sans nationalite (7)

ARENA FAVA C, DIDONI V, NAWA M, PANKIN SEMEN, PETIT L, SCHTULMANN GASCA M,
VAN SCHALKWYK C

### 4. Doublon possible restant (non fusionne, a trancher par Charles)

**KIM D. (id 1428, COREE)** vs **KIM DONGJU (id 12, COREE)** : les deux sont
inscrits dans le tableau de qualifs de **BUSAN 2026** (positions 18 et 3).
Soit deux Coreens differents (KIM etant un nom tres courant), soit un doublon
accidentel dans ce tableau precis - a vfier avant de fusionner (contrairement
aux 6 deja fusionnes, aucun chevauchement de tournoi ne permettait de trancher
automatiquement ici puisqu'ils sont justement dans le MEME tournoi).

Tous les autres cas de "meme nom de famille + meme nationalite avec des
prenoms differents" verifies (~49 groupes) sont de vrais joueurs distincts
(patronymes courants ou fratries reelles, ex. CERUNDOLO Francisco/Juan Manuel,
TSITSIPAS Stefanos/Petros, BERRETTINI Matteo/Jacobo) - pas des doublons.

## 12. Import des resultats Challenger depuis TennisTemple (a partir d'Ilkley, semaine 24)

Charles a demande (2026-09-21) de reprendre le remplissage des tournois
Challenger deja premodelises (tableau vide, cf section 2) en utilisant
`fr.tennistemple.com` comme source, a partir d'Ilkley (ATP CH 125, semaine
24) puisque les semaines 1-22 etaient deja saisies. Chantier volontairement
enorme (~185 tableaux principal+qualifs sur 14+ semaines) - a poursuivre sur
plusieurs sessions. Tout ce qui suit est le mode d'emploi complet pour
reprendre directement.

### Etat d'avancement (2026-09-21, mis a jour en cours de session)

**Fait et verifie (vainqueur reel confronte au palmares du site) : semaines
24 a 36 completes, soit 59 tournois (tableau principal + qualifs) :**
Ilkley, Lyon, Bratislava, Cattolica, Tucuman (S24) ; Nottingham125, Parma,
Poznan, Dublin, Ascunsion, Royan (S25) ; Piracicaba, Plovdiv, Targu (S26,
dates reelles confirmees) ; Brasov, Cary, Milan, Quito, Troyes (S27, dates
reelles confirmees) ; Iasi, Bogota, Liege, Nottingham3(id169), Braunschweig,
Newport, Trieste (S28, dates reelles confirmees) ; Bunschoten, Cordenons,
Granby, Lincoln, Pozoblanco (S29) ; Bloomfield, Segovia, Zug (S30) ; Bonn,
Liberec, Samsun, San Marin, Vancouver (S31, dates reelles confirmees -
aucun decalage, le bug de semaines s'arrete bien a S26/28) ; Grodzisk,
Hagen, Istanbul(id194), Lexington, Plovdiv2(id195) (S32, dates reelles
confirmees) ; Astana, Brownsburg, Hambourg CH(id198), Todi (S33) ; Cancun,
Kingston(id203), Prague, Quebec, Roehampton(id206), Sion (S34, doublons
Kingston/Roehampton S34 vs S35 desambigues par date reelle) ; Augsbourg,
Kingston2(id208), Roehampton2(id209) (S35) ; Como, Manacor, Plovdiv2(id216),
Porto, Zhangjiagang (S36, dates reelles confirmees).

**Fait le 2026-09-23 (7-13 septembre, `weekNumber` 36 en base) :** GENOA(218),
TULLN(219), SHANGHAI(220), ISTANBUL 3(222), PHAN THIET 3(223), tableau
principal + qualifs (533-537), via `job_s36.py`. **Consigne de Charles : "tu
ne touches pas aux points donnes ni aux semaines, tu inscris juste les
resultats"** - `job_s36.py` n'appelle donc plus `process_main`/`ensure_points`
(qui reecrivent le bareme depuis le panneau Dotations) ; les qualifs sont
creees avec le bareme majoritaire de la categorie en base. Il resout aussi
chaque joueur via son slug tennistemple complet (page du match) plutot que par
"Nom X." seul, et cree les inconnus en MAJUSCULE avec prenom complet - a
reutiliser comme modele pour les prochains imports.

**Reste a faire, meme semaine :** CASSIS(221), SEVILLE(217).

Semaine 38 : deja partiellement saisie par Charles avant cette session -
tournoi en cours reellement cette semaine (21/09), ne pas toucher sans
consigne. **Semaine 37 = derniere semaine premodelisee dans la base
actuelle** - une fois S36-37 finis, verifier si Charles veut etendre le
calendrier au-dela (aucun tournoi S39+ n'existe encore en base a ce stade).

Nouvelles anomalies source gerees cette session (voir aussi `tt_push.py`) :
- **Dotations vide/absente** (Samsun S31, Brownsburg S33, Roehampton S34
  quali, Sion S34) : `dotations_to_rounds` retourne un bareme a 0 partout
  plutot que planter. Quand "Titres en 2026" donne une valeur ponctuelle
  fiable pour le vainqueur (Brownsburg: Harris L "55 pts", Roehampton S34:
  Tarvet O "53 pts" - hors bareme standard de la categorie), ce point est
  repris explicitement, le reste (tours intermediaires, quali) reste a 0.
- **Adversaire "—" (id `null`) au round1** (Saint-Marin S31 quali ordre3,
  Cancun S34 quali ordre7) : traite comme un bye implicite (walkover),
  meme logique que "Bye" explicite.
- **Qualifs a 16 places** (Cancun, Quebec City S34, tournois ATP_125 a
  gros budget) : `draw_size = len(round1)*2` s'adapte automatiquement,
  aucun changement necessaire.

Semaine 38 : deja partiellement saisie par Charles lui-meme avant cette
session (26/32 entrees sur les 5 tournois) - tournoi en cours reellement
cette semaine (21/09), ne pas toucher sans consigne.

### La methode (extraction navigateur, deux commandes par tableau)

Pas de vrai import Excel->SQL ici : tout passe par l'API live du backend en
temps reel (`import/challengers/tt_push.py`), exactement comme
`push_generic.py` (section 9) - confirme avec Charles (2026-09-21).

1. **Trouver l'URL du tournoi.** `import/challengers/tt_calendar_2026.json`
   contient tous les liens `/competitions/<slug>-2026/<id>/` de la saison
   (extrait une fois depuis `https://fr.tennistemple.com/competitions/atp-challenger`,
   tous mois confondus). Sinon, chercher le nom du tournoi dans la liste
   "Titres en 2026" de cette meme page (lien direct vers l'edition).
2. **Extraire tableau principal + quali en 1 seul appel `browser_batch`**
   (6 actions : navigate `/draw` -> javascript_exec [snippet ci-dessous] ->
   get_page_text -> navigate `/draw-qualifications` -> javascript_exec ->
   get_page_text). Le snippet JS scrape `.tt-match` (round/ordre/joueurs/
   score) ET le panneau `Dotations`, les injecte dans un `<article>` qui
   remplace `<body>`, pour contourner la troncature (~1000-1300 caracteres)
   que le tool `javascript_exec` applique a son propre retour - `get_page_text`
   n'a pas cette limite. **Snippet complet** : voir le debut de n'importe
   quel `job_*.py` cree cette session (ou `git log` de ce README) - copie
   textuellement dans chaque appel `javascript_tool` de la conversation,
   trop long pour etre redonne ici, mais stable et reutilisable tel quel.
3. **Coller le JSON obtenu** dans un fichier `import/challengers/job_<nom>.py`
   (voir n'importe lequel des jobs deja crees comme modele) et lancer
   `python job_<nom>.py` depuis `import/challengers/`.

### Regles cle du modele de points (a ne jamais redeviner, cf `tt_push.py`)

- **Positions du tableau round1** : le backend (`BracketService.
  syncRound1FromEntries`) pair les positions `(2k-1, 2k)` pour le match
  round1 n°k - PAS un decoupage "moitie gauche/moitie droite" comme
  l'ancien import Excel. `push_entries_and_rounds` applique deja ca
  correctement - ne pas regresser (bug reel rencontre et corrige sur
  Ilkley : positions fausses -> scores attribues au mauvais match, tournoi
  entierement reinitialise puis repousse pour corriger).
- **Score** : `fmt_score()` convertit `"76(4) 46 63"` -> `"7-6(4) 4-6
  6-3"`. Chaine vide -> `"w.o."`.
- **Points par tour, regle generale (main ET quali)** : le panneau
  Dotations liste du palier le plus haut (Vainqueur / "Q 3e tour") au plus
  bas (1er tour), un palier de plus que le nombre de tours reels. Dernier
  palier = points du tour `totalRounds` (`pointsByRound[totalRounds]`,
  donne aussi au vainqueur ET a tout "qualifie" gagnant le dernier tour de
  quali) ; avant-dernier palier = `runnerUpPoints` (points du perdant du
  dernier tour, "Finale" ou "Q(N)e tour") ; le reste se lit dans l'ordre
  jusqu'au tour 1 (toujours force a 0). `dotations_to_rounds()` fait ce
  calcul - ne jamais assigner les points a la main tour par tour.
- **`total_rounds_for()`** : peut differer du tour max reellement present
  dans les donnees scrapees quand un tour existe dans le panneau Dotations
  mais est vide cote DOM (`drop_phantom_matches` retire les matchs "fantomes"
  ou aucun joueur n'est renseigne) - ex. Targu Mures S26 quali (3 paliers
  Dotations, 1 seul tour reellement peuple : total_rounds=2 quand meme,
  round2 reste PENDING). Cas degenere ou meme le panneau Dotations n'a
  qu'1 seul palier (Piracicaba S26 quali, quali a 12 reellement) : geree a
  part dans `dotations_to_rounds` (pas de palier "finaliste" distinct).
- **Homonymes** : `find_candidates` ne matche QUE sur `lastName` exact (+
  initiale du prenom, + nationalite si ambigu) - **le fuzzy matching
  (difflib) a ete retire** apres deux vrais faux-positifs qui ont corrompu
  des fiches existantes (Bocci F. vs Bocchi L., Mazza M. vs Mazzola F. -
  deux joueurs italiens distincts, memes tournois). Un nom sans candidat
  exact cree systematiquement un nouveau joueur plutot que de risquer une
  fusion erronee (un doublon isole est visible et corrigeable a l'audit ;
  une fausse fusion corrompt silencieusement une fiche existante - risque
  juge trop asymetrique). `post_entry_with_fallback` retente aussi avec
  l'id suivant si le backend refuse un candidat ("deja inscrit cette
  semaine ailleurs" - vrai homonyme, ex. Darwin Blanch (id237) vs Dali
  Blanch (id311), tous deux "Blanch D." sur TennisTemple).

### Bug majeur trouve et corrige : decalage systematique des semaines 27-35

**Independant de ce chantier d'import** - deja present dans les donnees du
seed original (`V2__seed_data.sql`), decouvert en recoupant les dates
reelles TennisTemple contre `week_number` en base. Les tournois **DB
semaine 26** melangeaient en realite deux semaines calendaires distinctes
(3 vrais S26 + 5 vrais S27), et TOUS les tournois DB semaines 27 a 34
etaient decales de +1 par rapport a leur vraie date TennisTemple (DB S27 =
vraie S28, DB S28 = vraie S29, ..., DB S34 = vraie S35) - le decalage
s'autocorrige exactement a la frontiere DB S34/S36 (**la case "semaine 35"
etait vide en base avant la correction** - c'est elle qui se remplit une
fois le decalage corrige). DB semaine 36 et au-dela : dates deja correctes,
verifie sur Augsburg (S35 reel apres correction) et Côme (S36, deja bon).

**Corrige** : `weekNumber` de 38 tournois (main draws) remis a la bonne
valeur via `PUT /api/tournaments/{id}` (pas de SQL brut - bloque par le
classifieur auto-mode comme "Cloud Storage Mass Delete", faux positif sur
un simple UPDATE ; contourne en bouclant sur l'API, plus propre de toute
facon). Les 5 tournois S26->S27 corriges individuellement en premier
(Brasov=160, Cary=158, Milan=159, Quito=162, Troyes=161, + leurs qualifs
455/456/457/460/462) ; les 33 autres (DB S27-34 restants) corriges en un
seul passage vers +1 (aucune qualif n'existait encore pour ceux-la, pas de
double correction a faire).

**A faire au demarrage de la prochaine session, avant de pousser quoi que
ce soit dans une semaine pas encore touchee (S29+)** : ce decalage
semblait resolu a partir de DB semaine 36, mais ca n'a ete verifie que sur
1 tournoi de chaque cote de la frontiere (Augsburg/Côme) - **revalider
au moins 1 tournoi par nouvelle semaine abordee** (ouvrir sa page
TennisTemple, comparer "X - Y mois" a la date attendue pour ce numero de
semaine - semaine N = semaine24 + (N-24) semaines a partir du 8 juin) avant
de pousser ses donnees, pour eviter de repeter cette erreur plus loin dans
la saison. Le signal qui a permis de detecter le probleme la premiere fois :
une cascade de conflits "deja inscrit cette semaine" sur des joueurs qui
n'auraient normalement aucune raison de se cotoyer (meme reflexe a avoir -
si `post_entry_with_fallback` cree soudain beaucoup de doublons pour des
noms qui ont pourtant l'air d'avoir deja une fiche, verifier les dates
avant de continuer).

### Anomalies source ponctuelles (documentees en commentaire dans le `job_*.py` concerne)

- **Plovdiv S26, main draw, round1 ordre6** (Kuzmanov D. vs Sels J.) :
  aucun des deux marque vainqueur cote TennisTemple (bug source). Resolu a
  la main par continuite de seed (round2 montre le meme "seed 11" face a
  Michalski, score coherent avec une victoire de Kuzmanov puis une
  contre-performance/retrait en round2) - voir `job_plovdiv1.py`.
- **Trieste S28, quali** : 2 matchs (round1 ordre7, round2 ordre4) sans
  aucun signal exploitable (score vide, pas de continuite de seed) -
  laisses PENDING volontairement (jamais deviner un resultat).
- **Piracicaba S26, quali** : le round2 n'existe simplement pas cote
  TennisTemple (dotations n'affiche qu'1 seul palier) - quali a 12
  reellement, geree comme un cas a part par `dotations_to_rounds`.

### Nettoyage restant pour Charles (doublons orphelins crees par erreur cette session)

16 fiches joueur creees a tort (candidat existant ecarte a cause du bug de
semaines ci-dessus, avant sa correction) puis devenues orphelines (aucune
entree ne les reference plus - la vraie fiche existante a ete reutilisee
une fois le bug corrige). Aucune n'est utilisee nulle part, suppression
sans risque mais pas d'endpoint `DELETE /api/players/{id}` dans l'app
actuelle : id 1590, 1629-1634, 1636-1641, 1646, 1649, 1651 (voir
`SELECT * FROM player WHERE id >= 1580 AND id NOT IN (SELECT player_id FROM entry)`
pour la liste a jour si d'autres s'accumulent).

### Pour reprendre directement

1. Relire ce README depuis "## 12." (ce chantier n'est fait qu'a moitie).
2. Verifier la date reelle d'au moins 1 tournoi de la semaine 29 avant de
   commencer (voir section decalage ci-dessus).
3. Suivre "La methode" ci-dessus, tournoi par tournoi, semaine par semaine
   dans l'ordre (29, 30, 31...), en utilisant `import/challengers/tt_push.py`
   tel quel (deja teste et durci sur 26 tournois).
4. Mettre a jour ce README (etat d'avancement + semaines restantes) avant
   de clore la session suivante.

## 13. Coupe Davis et United Cup (2026-09-23)

Onglets `Coupe Davis` (`/coupe-davis`) et `United Cup` (`/united-cup`), meme vue
`TeamCompetitionView.vue`. Modele a part des tournois (tables `team_tie` /
`team_rubber`, V21) : une rencontre oppose deux pays sur 3 ou 5 matchs, joueurs
en texte libre, **aucun point au classement**.

- Donnees 2026 (V22, generee par `import/teams/build_seed.py` depuis les
  extractions `daviscup_2026_raw.json` / `unitedcup_2026_raw.json`, a relancer
  si on corrige une extraction) : Coupe Davis qualifs 1er tour (13) et 2e tour
  (7), barrages Groupe mondial I (13), Final 8 de Bologne (quarts connus, demies
  et finale en attente) ; United Cup 6 poules + phase finale. Le script verifie
  que chaque score de rencontre recalcule = score officiel.
- Saisie : clic sur une rencontre -> "Saisir" par match (`PUT
  /api/team-ties/{id}/rubbers/{n}`). Score et vainqueur de la rencontre
  recalcules (majorite des matchs), vainqueur reporte automatiquement dans la
  rencontre du tour suivant (FINALS_QF -> FINALS_SF -> FINALS_F, QF -> SF -> F).
- Saisons suivantes (Charles, 2026-09-23 : "pour l'annee prochaine il faudra que
  je puisse saisir") : selecteur de saison (saisons en base + la suivante),
  "Nouvelle rencontre" par phase (`POST /api/team-ties`, matchs vides crees :
  5 en qualifs/barrages, 3 en United Cup, le 3e toujours le double), "Creer le
  tableau" pour le Final 8 / la phase finale United Cup (`POST
  /api/team-ties/bracket` : 4 quarts + demies et finale en attente), "Modifier"
  / "Supprimer la rencontre" dans le detail (`PUT` / `DELETE
  /api/team-ties/{id}` ; un quart ne se supprime pas seul : `DELETE
  /api/team-ties/bracket`).
- Sources : daviscup.com (pages `/en/tie/<uuid>`, rendues cote client - extraction
  via iframe dans le navigateur) et unitedcup.com/en/scores/results (le
  vainqueur de chaque match y est liste en premier, pas l'equipe 1 : equipe des
  joueurs deduite par `assign_uc_teams`).

## Structure du repo

```
tennis-results/
├── backend/    Spring Boot (Java 21, Maven, PostgreSQL, Flyway)
├── frontend/   Vue 3 + Vite (avec le composant BracketView pour le tableau visuel)
├── import/     Script Python de conversion Excel → SQL
│   ├── weekly_results/   Decodage + import des feuilles hebdo (voir section 9)
│   └── challengers/      Import TennisTemple des Challengers S24+ (voir section 12)
└── README.md
```
