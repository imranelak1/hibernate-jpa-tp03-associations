package com.example.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class Utilisateur {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String nom;
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservations = new LinkedHashSet<>();

    public Utilisateur() { }
    public Utilisateur(String nom, String email) { setNom(nom); setEmail(email); }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Nom obligatoire");
        this.nom = nom;
    }
    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obligatoire");
        this.email = email;
    }
    public Set<Reservation> getReservations() { return Collections.unmodifiableSet(reservations); }
    public void addReservation(Reservation reservation) { reservation.setUtilisateur(this); }
    public void removeReservation(Reservation reservation) {
        if (reservations.contains(reservation)) reservation.setUtilisateur(null);
    }
    void linkReservation(Reservation reservation) { reservations.add(reservation); }
    void unlinkReservation(Reservation reservation) { reservations.remove(reservation); }
    @Override public String toString() { return "Utilisateur{id=" + id + ", nom='" + nom + "', email='" + email + "'}"; }
}
