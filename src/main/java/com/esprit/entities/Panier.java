package com.esprit.entities;

import java.time.LocalDateTime;

public class Panier {

    private int idPanier;
    private int idClient;
    private int idEtablissement;
    private String typeService;       // Voyage, Restaurant, Café
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int nbPersonnes;
    private double prixEstime;
    private String statutItem;        // en_attente, annulé
    private int nbAdultes;            // number of adults
    private int nbEnfants;            // number of children

    // Joined fields (for display)
    private String nomClient;
    private String nomEtablissement;

    public Panier() {}

    public Panier(int idClient, int idEtablissement, String typeService,
                  LocalDateTime dateDebut, LocalDateTime dateFin,
                  int nbPersonnes, double prixEstime, String statutItem) {
        this.idClient = idClient;
        this.idEtablissement = idEtablissement;
        this.typeService = typeService;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbPersonnes = nbPersonnes;
        this.prixEstime = prixEstime;
        this.statutItem = statutItem;
    }

    // Getters & Setters
    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdEtablissement() { return idEtablissement; }
    public void setIdEtablissement(int idEtablissement) { this.idEtablissement = idEtablissement; }

    public String getTypeService() { return typeService; }
    public void setTypeService(String typeService) { this.typeService = typeService; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public int getNbPersonnes() { return nbPersonnes; }
    public void setNbPersonnes(int nbPersonnes) { this.nbPersonnes = nbPersonnes; }

    public double getPrixEstime() { return prixEstime; }
    public void setPrixEstime(double prixEstime) { this.prixEstime = prixEstime; }

    public String getStatutItem() { return statutItem; }
    public void setStatutItem(String statutItem) { this.statutItem = statutItem; }

    public String getNomClient() { return nomClient; }
    public void setNomClient(String nomClient) { this.nomClient = nomClient; }

    public String getNomEtablissement() { return nomEtablissement; }
    public void setNomEtablissement(String nomEtablissement) { this.nomEtablissement = nomEtablissement; }

    public int getNbAdultes() { return nbAdultes; }
    public void setNbAdultes(int nbAdultes) { this.nbAdultes = nbAdultes; }

    public int getNbEnfants() { return nbEnfants; }
    public void setNbEnfants(int nbEnfants) { this.nbEnfants = nbEnfants; }

    @Override
    public String toString() {
        return "Panier{" +
                "idPanier=" + idPanier +
                ", typeService='" + typeService + '\'' +
                ", nbPersonnes=" + nbPersonnes +
                ", prixEstime=" + prixEstime +
                ", statutItem='" + statutItem + '\'' +
                '}';
    }
}
