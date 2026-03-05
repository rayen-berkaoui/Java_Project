package com.esprit.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Activite {

    private int idActivite;
    private String nomActivite;
    private String description;
    private String categorie;
    private Integer duree; // nullable
    private String niveau;

    private BigDecimal prix;      // nullable
    private String devise;        // default 'TND' in DB
    private Timestamp dateDebut;  // nullable
    private Timestamp dateFin;    // nullable

    private Integer nbPlaces;     // nullable
    private Integer placesDispo;  // nullable

    private String adresseDepart;         // nullable
    private Integer ageMin;              // nullable
    private String equipementInclus;     // nullable
    private String conditionsAnnulation; // nullable

    private String statut; // 'disponible','complete','annulee'
    private Integer idEtablissement;

    public Activite() {}

    // Constructeur sans id (pour INSERT)
    public Activite(String nomActivite, String description, String categorie, Integer duree, String niveau,
                    BigDecimal prix, String devise, Timestamp dateDebut, Timestamp dateFin,
                    Integer nbPlaces, Integer placesDispo, String adresseDepart, Integer ageMin,
                    String equipementInclus, String conditionsAnnulation, String statut) {

        this.nomActivite = nomActivite;
        this.description = description;
        this.categorie = categorie;
        this.duree = duree;
        this.niveau = niveau;
        this.prix = prix;
        this.devise = devise;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbPlaces = nbPlaces;
        this.placesDispo = placesDispo;
        this.adresseDepart = adresseDepart;
        this.ageMin = ageMin;
        this.equipementInclus = equipementInclus;
        this.conditionsAnnulation = conditionsAnnulation;
        this.statut = statut;
    }

    // Getters / Setters
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

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public Timestamp getDateDebut() { return dateDebut; }
    public void setDateDebut(Timestamp dateDebut) { this.dateDebut = dateDebut; }

    public Timestamp getDateFin() { return dateFin; }
    public void setDateFin(Timestamp dateFin) { this.dateFin = dateFin; }

    public Integer getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(Integer nbPlaces) { this.nbPlaces = nbPlaces; }

    public Integer getPlacesDispo() { return placesDispo; }
    public void setPlacesDispo(Integer placesDispo) { this.placesDispo = placesDispo; }

    public String getAdresseDepart() { return adresseDepart; }
    public void setAdresseDepart(String adresseDepart) { this.adresseDepart = adresseDepart; }

    public Integer getAgeMin() { return ageMin; }
    public void setAgeMin(Integer ageMin) { this.ageMin = ageMin; }

    public String getEquipementInclus() { return equipementInclus; }
    public void setEquipementInclus(String equipementInclus) { this.equipementInclus = equipementInclus; }

    public String getConditionsAnnulation() { return conditionsAnnulation; }
    public void setConditionsAnnulation(String conditionsAnnulation) { this.conditionsAnnulation = conditionsAnnulation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Integer getIdEtablissement() { return idEtablissement; }
    public void setIdEtablissement(Integer idEtablissement) { this.idEtablissement = idEtablissement; }

    @Override
    public String toString() {
        return "Activite{" +
                "idActivite=" + idActivite +
                ", nomActivite='" + nomActivite + '\'' +
                ", categorie='" + categorie + '\'' +
                ", niveau='" + niveau + '\'' +
                ", prix=" + prix +
                ", devise='" + devise + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}