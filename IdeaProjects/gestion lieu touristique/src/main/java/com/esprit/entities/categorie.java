package com.esprit.entities;

import java.time.LocalDate;

public class categorie {

    private int idcategorie;
    private String nomcategorie;
    private String description;
    private LocalDate dateCreation;

    // Constructeur vide
    public categorie() {}

    // Constructeur sans id
    public categorie(String nomcategorie, String description, LocalDate dateCreation) {
        this.nomcategorie = nomcategorie;
        this.description = description;
        this.dateCreation = dateCreation;
    }

    // Constructeur avec id
    public categorie(int idcategorie, String nomcategorie, String description, LocalDate dateCreation) {
        this.idcategorie = idcategorie;
        this.nomcategorie = nomcategorie;
        this.description = description;
        this.dateCreation = dateCreation;
    }

    // Getters & Setters
    public int getIdcategorie() { return idcategorie; }
    public void setIdcategorie(int idcategorie) { this.idcategorie = idcategorie; }
    public String getNomcategorie() { return nomcategorie; }
    public void setNomcategorie(String nomcategorie) { this.nomcategorie = nomcategorie; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return "Categorie{" +
                "idcategorie=" + idcategorie +
                ", nomcategorie='" + nomcategorie + '\'' +
                ", description='" + description + '\'' +
                ", dateCreation=" + dateCreation +
                '}';
    }

    public void setIdCategorie(int idCategorie) { this.idcategorie = idCategorie; }
}
