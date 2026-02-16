package com.esprit.entities;

public class LieuTouristique {

    private int id_lieu;
    private String nom;
    private String description;
    private String ville;
    private int id_adresse;
    private double prix;
    private String image;
    private int statut; // 1 = disponible, 0 = indisponible
    private int id_categorie;

    // Constructeur vide
    public LieuTouristique() {}

    // Sans id (ajout)
    public LieuTouristique(String nom, String description, String ville, int id_adresse,
                           double prix, String image, int statut, int id_categorie) {
        this.nom = nom;
        this.description = description;
        this.ville = ville;
        this.id_adresse = id_adresse;
        this.prix = prix;
        this.image = image;
        this.statut = statut;
        this.id_categorie = id_categorie;
    }

    // Avec id (modification)
    public LieuTouristique(int id_lieu, String nom, String description, String ville, int id_adresse,
                           double prix, String image, int statut, int id_categorie) {
        this.id_lieu = id_lieu;
        this.nom = nom;
        this.description = description;
        this.ville = ville;
        this.id_adresse = id_adresse;
        this.prix = prix;
        this.image = image;
        this.statut = statut;
        this.id_categorie = id_categorie;
    }

    // Getters & Setters
    public int getId_lieu() { return id_lieu; }
    public void setId_lieu(int id_lieu) { this.id_lieu = id_lieu; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public int getId_adresse() { return id_adresse; }
    public void setId_adresse(int id_adresse) { this.id_adresse = id_adresse; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getStatut() { return statut; }
    public void setStatut(int statut) { this.statut = statut; }
    public int getId_categorie() { return id_categorie; }
    public void setId_categorie(int id_categorie) { this.id_categorie = id_categorie; }

    @Override
    public String toString() {
        return "LieuTouristique{" +
                "id_lieu=" + id_lieu +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", ville='" + ville + '\'' +
                ", id_adresse=" + id_adresse +
                ", prix=" + prix +
                ", image='" + image + '\'' +
                ", statut=" + statut +
                ", id_categorie=" + id_categorie +
                '}';
    }
}
