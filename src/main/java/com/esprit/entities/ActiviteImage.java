package com.esprit.entities;

public class ActiviteImage {
    private int idImage;
    private int idActivite;
    private String imagePath;      // ex: "1700000000_photo.jpg"
    private int ordreAffichage;    // 1,2,3...

    public ActiviteImage() {}

    public ActiviteImage(int idActivite, String imagePath, int ordreAffichage) {
        this.idActivite = idActivite;
        this.imagePath = imagePath;
        this.ordreAffichage = ordreAffichage;
    }

    public int getIdImage() { return idImage; }
    public void setIdImage(int idImage) { this.idImage = idImage; }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }
}