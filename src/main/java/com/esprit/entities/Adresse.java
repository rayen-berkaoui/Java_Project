package com.esprit.entities;

public class Adresse {

    private int id_adresse;
    private String rue;
    private String ville;
    private double latitude;
    private double longitude;
    private double altitude; // <-- ici on utilise le même nom que la table

    // Constructeur vide
    public Adresse() {}

    // Constructeur sans id (pour ajouter)
    public Adresse(String rue, String ville, double latitude, double longitude, double altitude) {
        this.rue = rue;
        this.ville = ville;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    // Constructeur avec id (pour modifier)
    public Adresse(int id_adresse, String rue, String ville, double latitude, double longitude, double altitude) {
        this.id_adresse = id_adresse;
        this.rue = rue;
        this.ville = ville;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    // Getters & Setters
    public int getId_adresse() { return id_adresse; }
    public void setId_adresse(int id_adresse) { this.id_adresse = id_adresse; }

    public String getRue() { return rue; }
    public void setRue(String rue) { this.rue = rue; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    @Override
    public String toString() {
        return "Adresse{" +
                "id_adresse=" + id_adresse +
                ", rue='" + rue + '\'' +
                ", ville='" + ville + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                '}';
    }
}
