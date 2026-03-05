package com.esprit.entities;

public class EtablissementImage {
    private int idImage;
    private int idEtablissement;
    private String imagePath;     // ex: "blue1.jpg"
    private int ordreAffichage;   // ex: 1,2,3

    public EtablissementImage() {}

    public EtablissementImage(int idEtablissement, String imagePath, int ordreAffichage) {
        this.idEtablissement = idEtablissement;
        this.imagePath = imagePath;
        this.ordreAffichage = ordreAffichage;
    }

    public int getIdImage() { return idImage; }
    public void setIdImage(int idImage) { this.idImage = idImage; }

    public int getIdEtablissement() { return idEtablissement; }
    public void setIdEtablissement(int idEtablissement) { this.idEtablissement = idEtablissement; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }
}