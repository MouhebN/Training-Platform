# Fonctionnalites Metier Avancees

Ce document explique les fonctionnalites metier avancees du projet, pour pouvoir les presenter facilement a un client ou a un encadrant.

## 1. Authentification JWT et gestion des roles

### Objectif metier
La plateforme doit separer les espaces selon le profil utilisateur :

- `ADMIN` : gere les utilisateurs, formations, categories, sessions et inscriptions.
- `TRAINER` : consulte ses sessions, son profil et ses disponibilites.
- `LEARNER` : consulte le catalogue, s'inscrit aux sessions et gere son profil.

### Comment ca fonctionne
Quand un utilisateur se connecte, le backend verifie son email et son mot de passe. Si les informations sont correctes, il genere un token JWT.

Le frontend stocke ce token dans `localStorage`, puis l'ajoute automatiquement dans chaque requete HTTP avec l'en-tete :

```http
Authorization: Bearer TOKEN
```

Spring Security lit ce token, identifie l'utilisateur et applique les regles d'acces.

### Exemple client
Un apprenant ne peut pas acceder au dashboard admin. Un formateur ne peut pas creer une formation. Chaque role voit uniquement les fonctionnalites qui le concernent.

## 2. Securite des comptes

### Objectif metier
Proteger les comptes contre les erreurs, oublis de mot de passe et tentatives de connexion abusives.

### Fonctionnalites

- Changement de mot de passe par l'utilisateur connecte.
- Mot de passe oublie avec envoi par SMTP.
- Blocage du compte apres plusieurs tentatives de connexion incorrectes.
- Deverrouillage du compte par l'administrateur.
- Activation/desactivation d'un compte par l'administrateur.

### Comment ca fonctionne
Pour chaque tentative de connexion echouee, le backend incremente `failedLoginAttempts`.

Apres 5 echecs :

- `accountLocked = true`
- `lockTime = now`

Si l'utilisateur essaye de se connecter apres le blocage, le backend refuse la connexion avec un message clair :

```text
Account is locked. Contact an administrator to unlock it.
```

### Exemple client
Si quelqu'un essaye de deviner le mot de passe d'un compte, le systeme bloque le compte automatiquement apres plusieurs echecs.

## 3. Catalogue des formations

### Objectif metier
Permettre a l'administrateur de structurer les formations par categories, niveaux et chapitres.

### Donnees gerees

- Categories : IT, Management, Languages, Cybersecurity...
- Formations : titre, description, prix, niveau, duree, categorie.
- Chapitres : contenu detaille d'une formation.
- Competences requises : skills necessaires avant de suivre une formation.

### Comment ca fonctionne
Une formation appartient a une categorie et peut avoir plusieurs competences requises.

Exemple :

```text
Formation: Advanced Spring Security
Required skills: Java, Spring Boot, Spring Security, REST APIs
```

Ces competences sont utilisees ensuite pour l'analyse d'ecart de competences.

### Exemple client
L'admin peut creer une formation "Spring Boot Fundamentals", ajouter les chapitres, definir son niveau et selectionner les competences requises.

## 4. Profils apprenants

### Objectif metier
Connaitre le niveau, les competences et les objectifs d'un apprenant pour personnaliser son experience.

### Donnees gerees

- Telephone
- Bio
- Niveau actuel : `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
- Competences possedees
- Objectifs d'apprentissage

### Comment ca fonctionne
Quand un utilisateur s'inscrit publiquement, il est cree avec le role `LEARNER`.

Le backend cree automatiquement un profil apprenant vide. Ensuite, l'apprenant complete son profil depuis l'interface.

### Exemple client
Un apprenant peut dire qu'il connait deja `Java` et `Angular`, puis ajouter comme objectif : `full stack java angular`.

Le systeme utilise ces donnees pour recommander les formations les plus pertinentes.

## 5. Profils formateurs et disponibilites

### Objectif metier
Permettre a l'administrateur de gerer les formateurs et de planifier les sessions selon leurs expertises.

### Donnees gerees

- Informations du formateur
- CV
- Annees d'experience
- Expertises
- Note moyenne
- Disponibilites hebdomadaires

### Comment ca fonctionne
Un compte formateur ne peut pas etre cree depuis l'inscription publique. Il est cree par l'administrateur.

Lors de la creation du formateur, le backend cree :

1. Un utilisateur avec role `TRAINER`
2. Un `TrainerProfile`
3. Les expertises associees

Le formateur peut ensuite gerer ses disponibilites.

### Exemple client
Un formateur Java peut etre disponible lundi matin et mercredi apres-midi. L'admin peut ensuite l'assigner a une session Spring Boot.

## 6. Sessions de formation

### Objectif metier
Planifier des sessions concretes pour une formation avec un formateur, des dates, une capacite et un mode online/onsite.

### Donnees gerees

- Formation associee
- Formateur assigne
- Date de debut et de fin
- Capacite
- Lieu ou lien de reunion
- Statut : `PLANNED`, `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

### Comment ca fonctionne
L'administrateur cree une session pour une formation existante et assigne un formateur.

Le systeme verifie :

- La date de debut est avant la date de fin.
- La capacite est superieure a 0.
- Si la session est online, elle peut avoir un lien de reunion.
- Si la session est onsite, elle doit avoir un lieu.

### Exemple client
La formation "Angular Essentials" peut avoir une session onsite avec 15 places dans "Training Center Room A".

## 7. Inscriptions aux sessions

### Objectif metier
Permettre aux apprenants de s'inscrire aux sessions disponibles et permettre a l'admin de suivre les inscriptions.

### Regles metier

- Un apprenant ne peut pas s'inscrire deux fois a la meme session.
- Il ne peut pas s'inscrire a une session annulee ou terminee.
- Il peut s'inscrire seulement si la session est `OPEN` ou `PLANNED`.
- Si la capacite est atteinte, l'inscription est refusee.
- Un formateur peut consulter les inscriptions de ses propres sessions.

### Comment ca fonctionne
Quand un apprenant clique sur "Enroll", le backend cree une inscription avec le statut `CONFIRMED`.

Le backend calcule aussi :

- Nombre d'inscrits
- Places disponibles
- Statut de la session

### Exemple client
Si une session a une capacite de 15 et deja 15 inscrits confirmes, le systeme refuse toute nouvelle inscription.

## 8. Score de completion du profil apprenant

### Objectif metier
Encourager l'apprenant a completer son profil pour obtenir de meilleures recommandations.

### Endpoint

```http
GET /api/learners/me/profile-score
```

### Calcul du score

Le score est calcule sur 100 points :

| Champ | Points |
| --- | ---: |
| Nom et prenom | 15 |
| Telephone | 15 |
| Bio | 15 |
| Niveau actuel | 15 |
| Au moins une competence | 20 |
| Objectifs d'apprentissage | 20 |

### Messages affiches

- Score < 50 : profil incomplet.
- Score entre 50 et 79 : bon debut, mais profil a completer.
- Score >= 80 : profil bien complete.

### Exemple client
Si un apprenant n'a pas ajoute ses competences ni ses objectifs, le systeme l'indique dans le dashboard.

## 9. Analyse d'ecart de competences

### Objectif metier
Comparer les competences d'un apprenant avec les competences requises pour une formation.

### Endpoint

```http
GET /api/learners/me/skill-gap/{formationId}
```

### Comment ca fonctionne
Le backend compare :

- Les skills de l'apprenant
- Les required skills de la formation

Puis il calcule :

```text
matchPercentage = matchingSkills / requiredSkills * 100
```

Si le pourcentage est au moins 60%, l'apprenant est considere pret.

### Exemple

Apprenant :

```text
Skills: Java, REST APIs
```

Formation :

```text
Required skills: Java, Spring Boot, REST APIs
```

Resultat :

```text
Matching: Java, REST APIs
Missing: Spring Boot
Match: 67%
Ready: true
```

### Valeur client
Le client voit que la plateforme ne propose pas seulement une liste de formations. Elle aide l'apprenant a comprendre pourquoi une formation est adaptee ou non.

## 10. Plan d'amelioration personnel

### Objectif metier
Recommander les meilleures formations a un apprenant selon son profil, ses objectifs et ses competences.

### Endpoint

```http
GET /api/learners/me/improvement-plan
```

### Donnees analysees

- Niveau actuel de l'apprenant
- Competences de l'apprenant
- Objectifs d'apprentissage
- Formations actives
- Competences requises des formations
- Sessions ouvertes ou planifiees

### Regles de priorite

Le systeme attribue une priorite :

- `HIGH` : la formation correspond aux objectifs de l'apprenant et le match de competences est suffisant.
- `MEDIUM` : la formation correspond au niveau actuel de l'apprenant.
- `LOW` : formation active mais moins prioritaire.

Le systeme donne aussi des raisons, par exemple :

- Correspond a vos objectifs.
- Correspond a votre niveau.
- Une session ouverte ou planifiee est disponible.
- Ameliore les competences manquantes.

### Gestion des fautes simples
Le systeme normalise certains textes. Par exemple :

- `frensh`
- `french`
- `francais`

peuvent aider a retrouver les formations liees au francais.

### Exemple client
Si l'apprenant met comme objectif `french`, le systeme doit recommander en premier les formations de francais ou de langues, pas une formation full stack uniquement parce que le niveau correspond.

## 11. Generateur intelligent de parcours d'apprentissage

> Note MLA (sujet 4): les **suggestions de formations** basées sur l'analyse de profil
> passent par le service Python `ml-service` (voir `docs/mla-pipeline.md`).
> Le learning path reste un parcours structuré côté Spring; l'Improvement Plan utilise le modèle MLA.

### Objectif metier
Construire automatiquement un parcours personnalise pour chaque apprenant, au lieu de lui afficher seulement une liste de formations.

Le parcours repond a une question simple :

```text
Quelle formation dois-je suivre maintenant, et dans quel ordre progresser ?
```

### Endpoint

```http
GET /api/learning-path/me
```

### Donnees analysees

Le systeme utilise :

- Le profil apprenant
- Son niveau actuel
- Ses competences
- Ses objectifs d'apprentissage
- Les formations actives
- Les competences requises par chaque formation
- Les inscriptions deja terminees ou en cours
- Les sessions ouvertes ou planifiees

### Comment ca fonctionne

Pour chaque formation active, le backend calcule :

- Le pourcentage de matching entre les skills de l'apprenant et les skills requis.
- Si la formation est deja terminee.
- Si la formation est en cours.
- Si la formation correspond aux objectifs de l'apprenant.
- Si une session est disponible.

Ensuite, le systeme trie les formations pour former un parcours :

1. Formations deja terminees.
2. Formations liees aux objectifs.
3. Niveau logique : `BEGINNER`, puis `INTERMEDIATE`, puis `ADVANCED`.
4. Meilleur match de competences.
5. Formations avec sessions disponibles.

### Statuts possibles

Chaque etape du parcours a un statut :

| Statut | Signification |
| --- | --- |
| `COMPLETED` | L'apprenant a deja termine cette formation |
| `IN_PROGRESS` | L'apprenant est actuellement inscrit |
| `RECOMMENDED_NEXT` | Meilleure prochaine formation recommandee |
| `AVAILABLE` | Formation accessible selon le profil |
| `LOCKED` | Formation a eviter pour l'instant car il manque trop de pre-requis |

### Exemple client

Si un apprenant a :

```text
Skills: Java
Goal: spring backend
Level: BEGINNER
```

Le systeme peut proposer :

1. Java Basics : `COMPLETED`
2. Spring Boot Fundamentals : `RECOMMENDED_NEXT`
3. Advanced Spring Security : `LOCKED`

### Valeur client

Cette fonctionnalite donne un effet "plateforme intelligente" :

- Elle guide l'apprenant.
- Elle explique pourquoi une formation est recommandee.
- Elle evite de pousser une formation trop avancee.
- Elle montre une progression claire avec un pourcentage global.

## 12. Communication de session et support live

### Objectif metier
Permettre une communication directe entre le formateur assigne a une session et les apprenants inscrits.

Cette fonctionnalite transforme la session en espace interactif :

- L'apprenant peut poser des questions.
- Le formateur peut repondre en temps reel.
- Les messages restent sauvegardes.
- Les utilisateurs voient les messages non lus.
- Une notification sonore est jouee a la reception d'un nouveau message.

### Technologies utilisees

- Spring WebSocket
- STOMP
- JWT pour identifier l'utilisateur connecte
- Base de donnees pour stocker les messages
- Angular WebSocket cote frontend
- Web Audio API pour le son de notification

### Regles d'acces

Un utilisateur peut acceder au chat d'une session seulement si :

- il est `ADMIN`, ou
- il est le formateur assigne a la session, ou
- il est apprenant inscrit a la session avec statut `CONFIRMED`, `WAITLISTED` ou `COMPLETED`.

### Donnees sauvegardees

Chaque message contient :

- Session
- Expediteur
- Nom complet de l'expediteur
- Role de l'expediteur
- Contenu
- Type de message : `TEXT` ou `SYSTEM`
- Date d'envoi

Le systeme sauvegarde aussi les lectures avec `MessageReadReceipt`.

### Fonctionnement temps reel

Quand un utilisateur envoie un message :

1. Le frontend envoie le message via WebSocket.
2. Le backend verifie que l'utilisateur a acces au chat.
3. Le backend sauvegarde le message.
4. Le backend diffuse le message a tous les utilisateurs connectes au chat de cette session.
5. Les autres utilisateurs voient le message instantanement.
6. Un son court est joue pour signaler la reception.

### Endpoints REST

Historique des messages :

```http
GET /api/sessions/{sessionId}/messages
```

Marquer les messages comme lus :

```http
POST /api/sessions/{sessionId}/messages/read
```

Nombre de messages non lus :

```http
GET /api/sessions/{sessionId}/messages/unread-count
```

### Destinations WebSocket

Envoi de message :

```text
/app/sessions/{sessionId}/chat.send
```

Indicateur de saisie :

```text
/app/sessions/{sessionId}/chat.typing
```

Lecture des messages :

```text
/app/sessions/{sessionId}/chat.read
```

Reception :

```text
/topic/sessions/{sessionId}/chat
/topic/sessions/{sessionId}/typing
/topic/sessions/{sessionId}/read
```

### Interface utilisateur

L'interface est inspiree de WhatsApp :

- Messages de l'utilisateur a droite.
- Messages des autres utilisateurs a gauche.
- Nom de l'expediteur affiche.
- Heure du message affichee.
- Zone de messages scrollable.
- Barre d'envoi fixe en bas.
- Badge de messages non lus.
- Indicateur "is typing".
- Son de notification quand un nouveau message est recu.

### Valeur client

Cette fonctionnalite donne une dimension collaborative et professionnelle a la plateforme.

Elle montre que la session n'est pas seulement une inscription administrative, mais un vrai espace de formation avec accompagnement, support et interaction en direct.

## 13. Admin Intelligence Center

### Objectif metier
Donner a l'administrateur une vision intelligente de la plateforme, pas seulement des compteurs simples.

Le centre d'intelligence aide l'admin a detecter rapidement :

- les formations avec forte demande,
- les formations sans session ouverte,
- les sessions pleines ou presque pleines,
- les formateurs surcharges,
- les apprenants proches de la certification,
- les profils apprenants incomplets,
- les competences les plus manquantes.

### Endpoint

```http
GET /api/admin/intelligence
```

Acces : `ADMIN` uniquement.

### Donnees analysees

Le backend analyse automatiquement :

- les formations actives,
- les competences requises par les formations,
- les sessions ouvertes, planifiees ou en cours,
- les inscriptions confirmees et en liste d'attente,
- les profils apprenants,
- les objectifs d'apprentissage,
- la charge de travail des formateurs.

### Score global de sante

Le systeme calcule un score de sante global entre 0 et 100.

Le score commence a 100, puis le backend retire des points :

| Probleme detecte | Penalite |
| --- | ---: |
| Formateur surcharge | -5 |
| Session pleine ou a haut risque | -5 |
| Formation demandee sans session ouverte | -3 |
| Profil apprenant incomplet | -2 |
| Formation sans competences requises | -2 |

Interpretation cote interface :

- `>= 80` : plateforme saine.
- `60-79` : attention necessaire.
- `< 60` : situation critique a traiter.

### Formations a forte demande

Une formation est consideree comme demandee si :

- elle n'a pas de session `OPEN` ou `PLANNED`,
- ses sessions sont pleines,
- elle a des apprenants en liste d'attente,
- les objectifs des apprenants correspondent a son titre, sa categorie ou ses competences,
- beaucoup d'apprenants n'ont pas encore ses competences requises.

Le backend calcule un `demandScore` et propose une action :

- `Create a new session`
- `Assign available trainer`
- `Review formation capacity`

### Risques sur les sessions

Le systeme calcule le taux d'occupation :

```text
capacityUsagePercentage = confirmedEnrollments / capacity * 100
```

Puis il classe la session :

| Taux d'occupation | Risque |
| --- | --- |
| >= 100% | `FULL` |
| 80% - 99% | `HIGH` |
| 50% - 79% | `MEDIUM` |
| < 50% | `LOW` |

Exemples d'actions :

- session pleine : ouvrir une autre session ou augmenter la capacite,
- session presque pleine : surveiller les inscriptions,
- risque moyen : continuer le suivi.

### Charge de travail des formateurs

Le centre analyse les heures planifiees sur les 30 prochains jours :

| Heures | Niveau |
| ---: | --- |
| 0 - 5 | `LOW` |
| 6 - 20 | `NORMAL` |
| 21 - 35 | `HIGH` |
| > 35 | `OVERLOADED` |

Si un formateur est `HIGH` ou `OVERLOADED`, l'admin voit une alerte et une recommandation :

```text
Avoid assigning too many additional sessions this month
Reassign future sessions or reduce workload
```

### Competences manquantes

Le systeme compare les competences requises des formations actives avec les competences des apprenants.

Pour chaque competence requise, il compte combien d'apprenants ne la possedent pas encore.

Exemple :

```text
Skill: Spring Boot
Missing count: 8 learners
Related formations: 3
Suggested action: Create beginner content for this skill
```

### Alertes et actions recommandees

Le dashboard admin affiche :

- des alertes avec severite `INFO`, `WARNING`, `CRITICAL`,
- des actions recommandees avec priorite `LOW`, `MEDIUM`, `HIGH`,
- un bouton d'action quand une route Angular existe.

Exemples :

```text
HIGH - Plan capacity for Spring Boot Fundamentals
MEDIUM - Close skill gap: Spring Boot
HIGH - Balance workload for Trainer One
```

### Valeur client

Cette fonctionnalite transforme le dashboard admin en outil d'aide a la decision.

Au lieu de seulement voir des chiffres, l'administrateur comprend :

- ce qui bloque la croissance,
- quelles formations ouvrir en priorite,
- quels formateurs ne pas surcharger,
- quelles competences developper,
- quels apprenants relancer pour completer leur profil.

## 14. Interface Angular par role

### Objectif metier
Chaque utilisateur a une interface adaptee a son role.

### Admin

- Dashboard
- Categories
- Formations
- Chapitres
- Formateurs
- Sessions
- Inscriptions
- Utilisateurs

### Learner

- Dashboard
- Catalogue
- Details formation
- Skill gap
- Mes inscriptions
- Profil
- Improvement plan

### Trainer

- Dashboard
- Mes sessions
- Profil
- Disponibilites

### Comment ca fonctionne
Angular utilise :

- `AuthGuard` pour verifier si l'utilisateur est connecte.
- `RoleGuard` pour verifier son role.
- `HttpInterceptor` pour ajouter le JWT automatiquement.

## 15. Donnees de demonstration

### Objectif metier
Avoir une base de donnees prete pour les tests et les demos client.

### Fichiers

```text
scripts/seed-demo-data.sql
scripts/seed-demo-data.sh
```

### Ce que le script ajoute

- Utilisateurs demo
- Formateurs
- Apprenants
- Competences
- Categories
- Formations
- Chapitres
- Sessions
- Inscriptions

### Comptes utiles

```text
admin@training.com / admin123
trainer.java@training.com / password
trainer.angular@training.com / password
trainer.management@training.com / password
learner.amine@training.com / password
learner.sara@training.com / password
learner.nour@training.com / password
learner.mehdi@training.com / password
```

## 16. Resume pour presentation client

La plateforme ne se limite pas a gerer un catalogue de formations. Elle couvre tout le cycle metier :

1. Gestion des comptes et securite.
2. Gestion des formations, categories et chapitres.
3. Gestion des formateurs, expertises et disponibilites.
4. Planification des sessions.
5. Inscriptions des apprenants.
6. Analyse intelligente du profil apprenant.
7. Recommandations personnalisees basees sur des regles metier.
8. Parcours d'apprentissage personnalise.
9. Communication temps reel entre formateurs et apprenants.
10. Intelligence admin pour detecter les risques et recommander des actions.

Le point fort du projet est la partie intelligence metier :

- Score de completion du profil.
- Analyse d'ecart entre competences actuelles et competences requises.
- Plan d'amelioration personnel.
- Parcours d'apprentissage intelligent avec prochaine formation recommandee.
- Chat de session en temps reel avec messages persistants, non lus, typing indicator et son de notification.
- Admin Intelligence Center avec score global, alertes, formations a forte demande, sessions a risque, formateurs surcharges et competences manquantes.

Ces fonctionnalites rendent la plateforme plus professionnelle, car elle accompagne l'apprenant dans son parcours au lieu de simplement afficher une liste de formations.
