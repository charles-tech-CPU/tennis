# Tennis Results

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

- Ecran de modification d'un tournoi deja cree (nom, categorie, bareme de points).
- Tirage automatique des tetes de serie (positions "protegees" officielles).
- Tableau de qualification a part (actuellement seulement 2 points informatifs).
- Import assiste (semi-automatique, avec relecture obligatoire) des tournois
  passes depuis les feuilles de semaine, si tu veux un jour retenter le sujet -
  possible en decoupant le travail tournoi par tournoi plutot qu'en un seul script
  generique.
- Historique du classement (courbe semaine par semaine) plutot qu'une seule vue
  "instantanee".

## Structure du repo

```
tennis-results/
├── backend/    Spring Boot (Java 21, Maven, PostgreSQL, Flyway)
├── frontend/   Vue 3 + Vite (avec le composant BracketView pour le tableau visuel)
├── import/     Script Python de conversion Excel → SQL
└── README.md
```
