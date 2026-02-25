package com.esprit.entities;

public class Etablissement {
    private int idEtablissement;
    private String nom;
    private String description;
    private String adresse;
    private String ville;
    private String telephone;
    private String email;
    private String horaires;
    private String gammePrix;

    private String type;

    public Etablissement() {}

    public Etablissement(String nom, String description, String adresse, String ville,
                         String telephone, String email, String horaires, String gammePrix,
                         String type) {
        this.nom = nom;
        this.description = description;
        this.adresse = adresse;
        this.ville = ville;
        this.telephone = telephone;
        this.email = email;
        this.horaires = horaires;
        this.gammePrix = gammePrix;
        this.type = type;
    }

    public int getIdEtablissement() { return idEtablissement; }
    public void setIdEtablissement(int idEtablissement) { this.idEtablissement = idEtablissement; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHoraires() { return horaires; }
    public void setHoraires(String horaires) { this.horaires = horaires; }

    public String getGammePrix() { return gammePrix; }
    public void setGammePrix(String gammePrix) { this.gammePrix = gammePrix; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}