package com.esprit.entities;

public class Lieu {

    private int idLieu;
    private String nom;
    private String description;
    private String ville;
    private double prix;
    private String image;
    private boolean statut;
    private int idCategorie;
    private int idAdresse;

    // Joined fields
    private String nomCategorie;

    public Lieu() {}

    public int getIdLieu() { return idLieu; }
    public void setIdLieu(int idLieu) { this.idLieu = idLieu; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public boolean isStatut() { return statut; }
    public void setStatut(boolean statut) { this.statut = statut; }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public int getIdAdresse() { return idAdresse; }
    public void setIdAdresse(int idAdresse) { this.idAdresse = idAdresse; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }
}
