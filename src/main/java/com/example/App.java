package com.example;

import com.example.model.*;
import com.example.persistence.JpaContext;
import com.example.service.ReservationService;
import java.time.LocalDateTime;

public final class App {
    private App() { }
    public static void main(String[] args) {
        try (JpaContext context = new JpaContext()) {
            ReservationService service = new ReservationService(context);
            Utilisateur alice = new Utilisateur("Alice Dupont", "alice@example.com");
            Salle salleA = new Salle("Salle A", 20);
            Equipement projecteur = new Equipement("Projecteur"); salleA.addEquipement(projecteur);
            LocalDateTime debut = LocalDateTime.of(2026, 10, 12, 9, 0), fin = debut.plusHours(1);
            Reservation initial = new Reservation(debut, fin); initial.setSalle(salleA); alice.addReservation(initial);
            service.ajouterUtilisateur(alice);
            System.out.println("\nTP3 - Graphe persiste par em.persist(utilisateur) : " + service.comptages());

            Salle salleB = new Salle("Salle B", 30); salleB.addEquipement(projecteur); salleB = service.enregistrerSalle(salleB);
            System.out.println("Projecteur partage : " + service.sallesAvecEquipement("Projecteur"));
            System.out.println("Reservations Alice : " + service.reservationsUtilisateur(alice.getId()));
            System.out.println("Salle A disponible 09:30-10:30 : " + service.salleDisponible(salleA.getId(), debut.plusMinutes(30), fin.plusMinutes(30)));
            System.out.println("Salle A disponible 10:00-11:00 : " + service.salleDisponible(salleA.getId(), fin, fin.plusHours(1)));
            try { service.reserver(alice.getId(), salleA.getId(), debut.plusMinutes(15), fin); }
            catch (IllegalStateException expected) { System.out.println("Chevauchement refuse : " + expected.getMessage()); }
            try { service.reserver(alice.getId(), salleA.getId(), fin, debut); }
            catch (IllegalArgumentException expected) { System.out.println("Dates invalides refusees : " + expected.getMessage()); }

            Reservation seconde = service.reserver(alice.getId(), salleA.getId(), fin, fin.plusHours(1));
            service.modifierNomUtilisateur(alice.getId(), "Alice Martin");
            System.out.println("Apres modification et nouvelle reservation : " + service.reservationsUtilisateur(alice.getId()));
            service.supprimerReservation(initial.getId());
            System.out.println("Apres orphanRemoval : " + service.comptages());
            service.supprimerUtilisateur(alice.getId());
            System.out.println("Apres suppression utilisateur (reservation " + seconde.getId() + " en cascade) : " + service.comptages());
            service.supprimerSalle(salleA.getId());
            System.out.println("Apres suppression Salle A : " + service.comptages());
            System.out.println("Equipement conserve pour Salle B : " + service.sallesAvecEquipement("Projecteur"));
            System.out.println("TP3 RESULTAT : associations, cascades, orphelins, dates, disponibilite, JPQL et rollback verifies.");
        }
    }
}
