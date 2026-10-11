package com.example.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class Equipement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    @ManyToMany(mappedBy = "equipements")
    private Set<Salle> salles = new LinkedHashSet<>();

    public Equipement() { }
    public Equipement(String nom) { setNom(nom); }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Nom d'equipement obligatoire");
        this.nom = nom;
    }
    public Set<Salle> getSalles() { return Collections.unmodifiableSet(salles); }
    public void addSalle(Salle salle) { salle.addEquipement(this); }
    public void removeSalle(Salle salle) { salle.removeEquipement(this); }
    void linkSalle(Salle salle) { salles.add(salle); }
    void unlinkSalle(Salle salle) { salles.remove(salle); }
    @Override public String toString() { return "Equipement{id=" + id + ", nom='" + nom + "'}"; }
}
