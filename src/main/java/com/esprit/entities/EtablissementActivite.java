package com.esprit.entities;

public class EtablissementActivite {
    // PK composée (idEtablissement, idActivite)
    private int idEtablissement;
    private int idActivite;

    private double prix;
    private boolean disponible;
    private int capacite;

    public EtablissementActivite() {}

    public EtablissementActivite(int idEtablissement, int idActivite, double prix, boolean disponible, int capacite) {
        this.idEtablissement = idEtablissement;
        this.idActivite = idActivite;
        this.prix = prix;
        this.disponible = disponible;
        this.capacite = capacite;
    }

    public int getIdEtablissement() { return idEtablissement; }
    public void setIdEtablissement(int idEtablissement) { this.idEtablissement = idEtablissement; }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    @Override
    public String toString() {
        return "EtablissementActivite{" +
                "idEtablissement=" + idEtablissement +
                ", idActivite=" + idActivite +
                ", prix=" + prix +
                ", disponible=" + disponible +
                ", capacite=" + capacite +
                '}';
    }
}
