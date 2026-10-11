# TP 3 — Associations JPA, cascades et réservations

Projet autonome du cours **Hibernate & JPA**, laboratoire 146. Les entités Salle, Reservation, Utilisateur et Equipement illustrent les relations OneToMany, ManyToOne et ManyToMany. Une démonstration exécutable et des tests sur une vraie base H2 vérifient la persistance en cascade, les orphelins et la conservation des équipements partagés.

## Exécuter

Prérequis : **JDK 17** et **Maven 3.9+**. Importer `pom.xml` comme projet Maven dans un IDE.

```sh
mvn clean test
mvn compile exec:java
```

La classe principale est `com.example.App`. La commande explicite du TP fonctionne également :

```sh
mvn clean compile exec:java -Dexec.mainClass=com.example.App
```

La démonstration se termine automatiquement et n'exige ni serveur JEE, ni EJB, ni installation de base externe.

## Couverture de toutes les étapes

| Étape | Réalisation |
| --- | --- |
| Projet Maven | `com.example:gestion-reservations:1.0-SNAPSHOT`, structure standard et compilation Java 17 |
| Dépendances du TP | JPA `javax.persistence-api:2.2`, Hibernate Core `5.4.30.Final`, H2 `1.4.200` en runtime |
| Configuration | Unité `gestion-reservations-pu`, fournisseur Hibernate, transactions `RESOURCE_LOCAL`, classes explicitement déclarées |
| H2 et SQL | `jdbc:h2:mem:test;DB_CLOSE_DELAY=-1`, schéma `update`, dialecte H2 et SQL formaté affiché |
| Entités | `com.example.model.Utilisateur`, `Salle`, `Reservation`, `Equipement` avec ID auto-générés et champs non nuls |
| OneToMany/ManyToOne | Utilisateur ↔ Reservation et Salle ↔ Reservation, propriétaires et clés étrangères explicites |
| ManyToMany | Salle ↔ Equipement, table `salle_equipement`, colonnes `salle_id` et `equipement_id` |
| Cascades | `ALL` de l'utilisateur vers ses réservations ; `PERSIST/MERGE` de la réservation vers sa salle, puis de la salle vers ses équipements |
| Orphelins | `orphanRemoval=true` sur `Utilisateur.reservations` et suppression via méthode d'association |
| Logique d'association | Méthodes `add/removeReservation`, `add/removeEquipement`, `add/removeSalle` synchronisant les deux côtés |
| Classe principale | Création d'Alice Dupont, Salle A et Projecteur, puis `persist(utilisateur)` persistant le graphe complet |
| Observation SQL | Insertions des quatre entités et de la table de jointure, mises à jour et suppressions |
| Modification/suppression | Modification du nom, suppression orpheline, suppression d'un utilisateur et d'une salle |
| Exercices supplémentaires | Validation des dates, disponibilité, deux recherches JPQL et gestion des exceptions avec rollback |

## Relations et suppression

`Reservation.utilisateur` est le côté propriétaire de la relation avec Utilisateur ; `Utilisateur.reservations` porte `mappedBy="utilisateur"`. Il s'agit d'une relation **bidirectionnelle**. Les méthodes d'association maintiennent la collection et la référence de la réservation ensemble.

`Salle.equipements` est propriétaire de la relation ManyToMany, tandis que `Equipement.salles` porte `mappedBy="equipements"`. Les collections exposées sont non modifiables : les changements passent par les méthodes qui synchronisent les côtés. Elles utilisent l'identité objet, sans hash dépendant d'un identifiant généré pendant la persistance.

La cascade `PERSIST/MERGE` de `Reservation.salle` complète explicitement le chemin décrit dans la démonstration du TP : `persist(utilisateur)` suffit à créer la réservation, la salle et ses équipements. **Aucune cascade REMOVE ne vise la salle ni les équipements partagés.** Supprimer un utilisateur supprime ses réservations, mais conserve salles et équipements. Supprimer une salle libre supprime ses liens ManyToMany et conserve l'équipement pour les autres salles.

`ReservationService.enregistrerSalle` utilise `merge` pour pouvoir créer une seconde salle avec un équipement déjà persistant. Une salle ayant encore des réservations ne peut pas être supprimée.

## Exercices supplémentaires réalisés

1. **Validation des dates** : `Reservation.verifierDates` exige deux dates non nulles et un début strictement antérieur à la fin. Le constructeur et le service vérifient cette règle ; `@PrePersist` et `@PreUpdate` la revérifient au moment de la persistance, même si des setters ont été utilisés séparément.
2. **Disponibilité des salles** : JPQL compte les réservations vérifiant `dateDebut < finDemandee AND dateFin > debutDemande`. Les périodes sont semi-ouvertes `[début, fin)` : deux réservations peuvent se toucher à la borne. `reserver` verrouille la salle avec `PESSIMISTIC_WRITE` avant de vérifier puis insérer, dans la même transaction.
3. **Requêtes JPQL** : `reservationsUtilisateur` retourne les réservations d'un utilisateur triées par date, et `sallesAvecEquipement` recherche les salles contenant un équipement donné. Les valeurs sont liées via paramètres nommés.
4. **Exceptions** : `JpaContext` annule les transactions actives sur exception, propage l'erreur et ferme toujours l'EntityManager. Les identifiants absents, chevauchements, dates invalides et conflits d'unicité sont refusés sans laisser des insertions partielles.

Les dates de démonstration sont fixes (`2026-10-12`) pour rendre les résultats reproductibles ; elles représentent des horaires locaux sans fuseau (`LocalDateTime`). La vérification de disponibilité seule est informative : la création transactionnelle doit passer par `reserver` pour conserver le verrou pendant l'insertion.

## Résultats observés

Le premier `persist(utilisateur)` crée **1 utilisateur, 1 salle, 1 réservation, 1 équipement et 1 lien de jointure**. Salle B réutilise ensuite le Projecteur : les recherches trouvent deux salles, sans dupliquer l'équipement.

Le créneau 09:30–10:30 est indisponible ; 10:00–11:00 est disponible. Un chevauchement et une période inversée sont refusés. Après création d'un créneau adjacent, suppression de la première réservation comme orpheline, suppression d'Alice en cascade puis suppression de Salle A, l'état final est :

| Table | Lignes |
| --- | ---: |
| Utilisateur | 0 |
| Reservation | 0 |
| Salle | 1 — Salle B |
| Equipement | 1 — Projecteur |
| salle_equipement | 1 |

## Tests et preuves

**10 tests JUnit 4 réussis, 0 échec, 0 erreur, 0 test ignoré.** Une base H2 différente et un schéma `create-drop` isolent chaque méthode. Les tests vérifient :

- le graphe persisté et le lien de jointure ;
- la synchronisation bidirectionnelle et les collections protégées ;
- la suppression orpheline et la cascade de suppression de l'utilisateur ;
- la conservation d'un équipement partagé après suppression d'une salle ;
- les dates invalides et l'annulation de leurs insertions ;
- différents chevauchements et les créneaux adjacents ;
- les deux filtres JPQL ;
- l'unicité, les identifiants absents et la reprise après erreur ;
- le rollback d'une erreur au milieu de la persistance du graphe.

Les traces sont issues d'exécutions réelles : [tests](evidence/test-output.txt), [démonstration](evidence/demo-output.txt), [rapports JUnit XML](evidence/junit/), [rapport consultable](evidence/report.html). Les erreurs SQL de contraintes dans les tests correspondent aux cas négatifs attendus, avec assertions sur la base après rollback.

Les captures ci-dessous sont des **rapports de navigateur présentant les journaux et résultats JUnit conservés**, et non des captures directes du terminal.

![Résultats de la démonstration](screenshots/results.png)
![Résultats des dix tests](screenshots/tests.png)

## Références

- [Guide officiel Hibernate ORM 5.4](https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html)
- [Tutoriel officiel Oracle : relations entre entités](https://docs.oracle.com/javaee/7/tutorial/persistence-intro.htm)
- [Documentation JUnit 4](https://junit.org/junit4/)


## Régénérer le rapport et les captures

Les images sont des captures navigateur de résultats réellement exécutés, conservés dans `evidence/`. Les tests proviennent des XML Surefire, également archivés dans `evidence/junit/`. Les empreintes SHA-256 des sources figurent dans `evidence/checksums.json`.

Avec Python 3 et Node.js disponibles, depuis la racine du dépôt :

```sh
mvn clean verify > evidence/test-output.txt 2>&1
mvn exec:java > evidence/demo-output.txt 2>&1
python scripts/generate_report.py
npm install --no-save playwright
npx playwright install chromium
node scripts/capture-results.cjs
```

Le rapport se consulte aussi directement dans `evidence/report.html`. Les captures générées sont `screenshots/results.png` et `screenshots/tests.png`.
