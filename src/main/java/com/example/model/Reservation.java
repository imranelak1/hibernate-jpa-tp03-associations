package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime dateDebut;
    @Column(nullable = false)
    private LocalDateTime dateFin;
    @ManyToOne @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    // PERSIST et MERGE permettent le graphe initial ; aucune cascade REMOVE vers la salle partagee.
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}) @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    public Reservation() { }
    public Reservation(LocalDateTime debut, LocalDateTime fin) { setDates(debut, fin); }
    public Long getId() { return id; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public void setDates(LocalDateTime debut, LocalDateTime fin) { verifierDates(debut, fin); dateDebut = debut; dateFin = fin; }
    public static void verifierDates(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null || !debut.isBefore(fin)) throw new IllegalArgumentException("La date de debut doit preceder la date de fin");
    }
    @PrePersist @PreUpdate private void valider() { verifierDates(dateDebut, dateFin); }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur next) {
        if (utilisateur == next) return;
        if (utilisateur != null) utilisateur.unlinkReservation(this);
        utilisateur = next;
        if (next != null) next.linkReservation(this);
    }
    public Salle getSalle() { return salle; }
    public void setSalle(Salle next) {
        if (salle == next) return;
        if (salle != null) salle.unlinkReservation(this);
        salle = next;
        if (next != null) next.linkReservation(this);
    }
    @Override public String toString() {
        return "Reservation{id=" + id + ", salle='" + (salle == null ? "aucune" : salle.getNom()) + "', debut=" + dateDebut + ", fin=" + dateFin + "}";
    }
}
