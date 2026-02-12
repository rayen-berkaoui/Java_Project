package com.esprit.entities;

public class role {

    // Attributs
    private int id;             // id du rôle
    private String nom;         // nom du rôle (ADMIN, TOURISTE, PARTENAIRE)
    private String description; // description du rôle

    // Constructeur vide
    public role() {}

    // Constructeur sans id (ajout)
    public role(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    // Constructeur avec id (lecture / modification)
    public role(int id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "role{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}