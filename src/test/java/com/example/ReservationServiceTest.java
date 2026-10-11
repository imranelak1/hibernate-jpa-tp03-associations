package com.example;

import com.example.model.*;
import com.example.persistence.JpaContext;
import com.example.service.ReservationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import static org.junit.Assert.*;

public class ReservationServiceTest {
    private JpaContext context;
    private ReservationService service;
    private final LocalDateTime debut = LocalDateTime.of(2026, 10, 12, 9, 0);
    @Before public void setUp() {
        context = new JpaContext(Map.of("javax.persistence.jdbc.url", "jdbc:h2:mem:tp3_" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        service = new ReservationService(context);
    }
    @After public void tearDown() { context.close(); }
    private Utilisateur graphe() {
        Utilisateur user = new Utilisateur("Alice", "alice@example.com");
        Salle salle = new Salle("Salle A", 20); salle.addEquipement(new Equipement("Projecteur"));
        Reservation reservation = new Reservation(debut, debut.plusHours(1)); reservation.setSalle(salle); user.addReservation(reservation);
        service.ajouterUtilisateur(user); return user;
    }
    @Test public void cascadePersistEnregistreLeGrapheEtLaJointure() {
        Utilisateur user = graphe();
        assertNotNull(user.getId());
        assertEquals(Map.of("Utilisateur",1L,"Salle",1L,"Reservation",1L,"Equipement",1L,"salle_equipement",1L), service.comptages());
        Reservation saved = service.reservationsUtilisateur(user.getId()).get(0);
        assertEquals("Salle A", saved.getSalle().getNom()); assertEquals(debut, saved.getDateDebut());
    }
    @Test public void helpersSynchronisentLesDeuxCotes() {
        Utilisateur a = new Utilisateur("A", "a@example.com"), b = new Utilisateur("B", "b@example.com");
        Salle salle = new Salle("A", 10); Equipement e = new Equipement("Projecteur");
        Reservation r = new Reservation(debut, debut.plusHours(1)); a.addReservation(r); salle.addReservation(r);
        assertTrue(a.getReservations().contains(r)); assertSame(a, r.getUtilisateur()); assertTrue(salle.getReservations().contains(r));
        r.setUtilisateur(b); assertFalse(a.getReservations().contains(r)); assertTrue(b.getReservations().contains(r));
        b.removeReservation(r); assertNull(r.getUtilisateur()); assertTrue(b.getReservations().isEmpty());
        salle.addEquipement(e); assertTrue(e.getSalles().contains(salle)); e.removeSalle(salle); assertTrue(salle.getEquipements().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> a.getReservations().add(r));
    }
    @Test public void orphanRemovalSupprimeLaReservationMaisPasLaSalle() {
        Utilisateur user = graphe(); Long id = user.getReservations().iterator().next().getId();
        service.supprimerReservation(id);
        assertEquals(Long.valueOf(0), service.comptages().get("Reservation"));
        assertEquals(Long.valueOf(1), service.comptages().get("Salle")); assertEquals(Long.valueOf(1), service.comptages().get("Equipement"));
    }
    @Test public void suppressionUtilisateurPropageSeulementAuxReservations() {
        Utilisateur user = graphe(); service.supprimerUtilisateur(user.getId());
        assertEquals(Long.valueOf(0), service.comptages().get("Utilisateur")); assertEquals(Long.valueOf(0), service.comptages().get("Reservation"));
        assertEquals(Long.valueOf(1), service.comptages().get("Salle")); assertEquals(Long.valueOf(1), service.comptages().get("Equipement"));
    }
    @Test public void suppressionSalleConserveEquipementPartage() {
        Utilisateur user = graphe(); Salle a = user.getReservations().iterator().next().getSalle();
        Equipement projecteur = a.getEquipements().iterator().next(); Salle b = new Salle("Salle B",30); b.addEquipement(projecteur);
        service.enregistrerSalle(b); assertEquals(2, service.sallesAvecEquipement("Projecteur").size());
        service.supprimerUtilisateur(user.getId()); service.supprimerSalle(a.getId());
        assertEquals(1, service.sallesAvecEquipement("Projecteur").size());
        assertEquals("Salle B", service.sallesAvecEquipement("Projecteur").get(0).getNom());
        assertEquals(Long.valueOf(1), service.comptages().get("Equipement")); assertEquals(Long.valueOf(1), service.comptages().get("salle_equipement"));
    }
    @Test public void datesInvalidesSontRefuseesAvantEtPendantPersistance() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation(debut, debut));
        Utilisateur user = new Utilisateur("A", "a@example.com"); Salle salle = new Salle("A",10);
        Reservation r = new Reservation(); r.setDateDebut(debut); r.setDateFin(debut.minusHours(1)); r.setSalle(salle); user.addReservation(r);
        assertThrows(RuntimeException.class, () -> service.ajouterUtilisateur(user));
        assertEquals(Long.valueOf(0), service.comptages().get("Utilisateur")); assertEquals(Long.valueOf(0), service.comptages().get("Reservation"));
    }
    @Test public void disponibiliteDetecteChevauchementEtPermetAdjacence() {
        Utilisateur user = graphe(); Long salleId = user.getReservations().iterator().next().getSalle().getId();
        assertFalse(service.salleDisponible(salleId, debut.plusMinutes(30), debut.plusHours(2)));
        assertFalse(service.salleDisponible(salleId, debut.minusHours(1), debut.plusMinutes(30)));
        assertFalse(service.salleDisponible(salleId, debut.minusHours(1), debut.plusHours(2)));
        assertTrue(service.salleDisponible(salleId, debut.plusHours(1), debut.plusHours(2)));
        assertTrue(service.salleDisponible(salleId, debut.minusHours(1), debut));
        assertThrows(IllegalStateException.class, () -> service.reserver(user.getId(), salleId, debut, debut.plusHours(1)));
        service.reserver(user.getId(), salleId, debut.plusHours(1), debut.plusHours(2));
        assertEquals(2, service.reservationsUtilisateur(user.getId()).size());
    }
    @Test public void jpqlFiltreParUtilisateurEtEquipement() {
        Utilisateur alice = graphe(); Utilisateur bob = new Utilisateur("Bob","bob@example.com"); service.ajouterUtilisateur(bob);
        assertEquals(1, service.reservationsUtilisateur(alice.getId()).size()); assertTrue(service.reservationsUtilisateur(bob.getId()).isEmpty());
        assertEquals(1, service.sallesAvecEquipement("Projecteur").size()); assertTrue(service.sallesAvecEquipement("Inconnu").isEmpty());
    }
    @Test public void uniciteEtErreurMetierNeCorrompentPasLaBase() {
        Utilisateur user = graphe();
        assertThrows(RuntimeException.class, () -> service.ajouterUtilisateur(new Utilisateur("Autre", "alice@example.com")));
        Long salleId = user.getReservations().iterator().next().getSalle().getId();
        assertThrows(IllegalStateException.class, () -> service.supprimerSalle(salleId));
        assertThrows(NoSuchElementException.class, () -> service.reserver(999L, salleId, debut.plusHours(2), debut.plusHours(3)));
        assertEquals(Long.valueOf(1), service.comptages().get("Utilisateur")); assertEquals(Long.valueOf(1), service.comptages().get("Reservation"));
        service.modifierNomUtilisateur(user.getId(), "Alice Martin");
        assertEquals("Alice Martin", context.read(em -> em.find(Utilisateur.class, user.getId()).getNom()));
    }
    @Test public void erreurAuMilieuDuGrapheAnnuleToutesLesInsertions() {
        assertThrows(IllegalStateException.class, () -> context.transaction(em -> {
            Salle salle = new Salle("Annulee", 20); salle.addEquipement(new Equipement("Annule")); em.persist(salle);
            throw new IllegalStateException("Erreur simulee");
        }));
        assertEquals(Long.valueOf(0), service.comptages().get("Salle")); assertEquals(Long.valueOf(0), service.comptages().get("Equipement"));
        graphe(); assertEquals(Long.valueOf(1), service.comptages().get("Reservation"));
    }
}
