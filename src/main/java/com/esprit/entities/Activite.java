package com.esprit.entities;

public class Activite {
    private int idActivite;
    private String nomActivite;
    private String description;
    private String categorie;
    private Integer duree;   // peut être null
    private String niveau;

    public Activite() {}

    public Activite(String nomActivite, String description, String categorie, Integer duree, String niveau) {
        this.nomActivite = nomActivite;
        this.description = description;
        this.categorie = categorie;
        this.duree = duree;
        this.niveau = niveau;
    }

    public Activite(int idActivite, String nomActivite, String description, String categorie, Integer duree, String niveau) {
        this.idActivite = idActivite;
        this.nomActivite = nomActivite;
        this.description = description;
        this.categorie = categorie;
        this.duree = duree;
        this.niveau = niveau;
    }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public String getNomActivite() { return nomActivite; }
    public void setNomActivite(String nomActivite) { this.nomActivite = nomActivite; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    @Override
    public String toString() {
        return nomActivite + (categorie != null && !categorie.isBlank() ? " (" + categorie + ")" : "");
    }
}
