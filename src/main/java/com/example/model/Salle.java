package com.example.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class Salle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    @Column(nullable = false)
    private int capacite;
    @OneToMany(mappedBy = "salle")
    private Set<Reservation> reservations = new LinkedHashSet<>();
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "salle_equipement", joinColumns = @JoinColumn(name = "salle_id"), inverseJoinColumns = @JoinColumn(name = "equipement_id"))
    private Set<Equipement> equipements = new LinkedHashSet<>();

    public Salle() { }
    public Salle(String nom, int capacite) { setNom(nom); setCapacite(capacite); }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Nom de salle obligatoire");
        this.nom = nom;
    }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) {
        if (capacite <= 0) throw new IllegalArgumentException("Capacite strictement positive requise");
        this.capacite = capacite;
    }
    public Set<Reservation> getReservations() { return Collections.unmodifiableSet(reservations); }
    public Set<Equipement> getEquipements() { return Collections.unmodifiableSet(equipements); }
    public void addEquipement(Equipement equipement) {
        if (equipement == null) throw new IllegalArgumentException("Equipement obligatoire");
        equipements.add(equipement); equipement.linkSalle(this);
    }
    public void removeEquipement(Equipement equipement) {
        equipements.remove(equipement); equipement.unlinkSalle(this);
    }
    public void addReservation(Reservation reservation) { reservation.setSalle(this); }
    public void removeReservation(Reservation reservation) {
        if (reservations.contains(reservation)) reservation.setSalle(null);
    }
    void linkReservation(Reservation reservation) { reservations.add(reservation); }
    void unlinkReservation(Reservation reservation) { reservations.remove(reservation); }
    @Override public String toString() { return "Salle{id=" + id + ", nom='" + nom + "', capacite=" + capacite + "}"; }
}
