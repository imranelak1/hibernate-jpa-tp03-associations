package com.example.service;

import com.example.model.*;
import com.example.persistence.JpaContext;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class ReservationService {
    private final JpaContext context;
    public ReservationService(JpaContext context) { this.context = context; }
    public void ajouterUtilisateur(Utilisateur user) {
        context.transaction(em -> { em.persist(user); return null; });
    }
    public Salle enregistrerSalle(Salle salle) { return context.transaction(em -> em.merge(salle)); }
    public void modifierNomUtilisateur(Long id, String nom) {
        context.transaction(em -> { require(em, Utilisateur.class, id).setNom(nom); return null; });
    }
    public Reservation reserver(Long utilisateurId, Long salleId, LocalDateTime debut, LocalDateTime fin) {
        Reservation.verifierDates(debut, fin);
        return context.transaction(em -> {
            Salle salle = em.find(Salle.class, salleId, LockModeType.PESSIMISTIC_WRITE);
            if (salle == null) throw new NoSuchElementException("Salle introuvable : " + salleId);
            if (!disponible(em, salleId, debut, fin)) throw new IllegalStateException("Salle deja reservee sur cette periode");
            Utilisateur user = require(em, Utilisateur.class, utilisateurId);
            Reservation reservation = new Reservation(debut, fin);
            reservation.setSalle(salle); user.addReservation(reservation); em.persist(reservation);
            return reservation;
        });
    }
    public boolean salleDisponible(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        Reservation.verifierDates(debut, fin);
        return context.read(em -> { require(em, Salle.class, salleId); return disponible(em, salleId, debut, fin); });
    }
    private boolean disponible(EntityManager em, Long salleId, LocalDateTime debut, LocalDateTime fin) {
        return em.createQuery("SELECT COUNT(r) FROM Reservation r WHERE r.salle.id = :salle AND r.dateDebut < :fin AND r.dateFin > :debut", Long.class)
                .setParameter("salle", salleId).setParameter("debut", debut).setParameter("fin", fin).getSingleResult() == 0;
    }
    public List<Reservation> reservationsUtilisateur(Long utilisateurId) {
        return context.read(em -> em.createQuery("SELECT r FROM Reservation r JOIN FETCH r.salle WHERE r.utilisateur.id = :user ORDER BY r.dateDebut", Reservation.class)
                .setParameter("user", utilisateurId).getResultList());
    }
    public List<Salle> sallesAvecEquipement(String nom) {
        return context.read(em -> em.createQuery("SELECT DISTINCT s FROM Salle s JOIN s.equipements e WHERE e.nom = :nom ORDER BY s.nom", Salle.class)
                .setParameter("nom", nom).getResultList());
    }
    public void supprimerReservation(Long id) {
        context.transaction(em -> {
            Reservation reservation = require(em, Reservation.class, id);
            reservation.getUtilisateur().removeReservation(reservation);
            reservation.getSalle().removeReservation(reservation);
            return null;
        });
    }
    public void supprimerUtilisateur(Long id) {
        context.transaction(em -> { em.remove(require(em, Utilisateur.class, id)); return null; });
    }
    public void supprimerSalle(Long id) {
        context.transaction(em -> {
            Salle salle = require(em, Salle.class, id);
            if (!salle.getReservations().isEmpty()) throw new IllegalStateException("Une salle reservee ne peut pas etre supprimee");
            new LinkedHashSet<>(salle.getEquipements()).forEach(salle::removeEquipement);
            em.remove(salle); return null;
        });
    }
    public Map<String, Long> comptages() {
        return context.read(em -> {
            Map<String, Long> result = new LinkedHashMap<>();
            for (String entity : List.of("Utilisateur", "Salle", "Reservation", "Equipement"))
                result.put(entity, em.createQuery("SELECT COUNT(e) FROM " + entity + " e", Long.class).getSingleResult());
            result.put("salle_equipement", ((Number) em.createNativeQuery("SELECT COUNT(*) FROM salle_equipement").getSingleResult()).longValue());
            return result;
        });
    }
    private <T> T require(EntityManager em, Class<T> type, Long id) {
        T result = em.find(type, id);
        if (result == null) throw new NoSuchElementException(type.getSimpleName() + " introuvable : " + id);
        return result;
    }
}
