package com.esprit.entities;

/**
 * Represents a single image associated with a LieuTouristique.
 * Stored in the lieu_image table.
 */
public class LieuImage {

    private int id;
    private int idLieu;
    private String imagePath;

    public LieuImage() {}

    public LieuImage(int idLieu, String imagePath) {
        this.idLieu = idLieu;
        this.imagePath = imagePath;
    }

    public LieuImage(int id, int idLieu, String imagePath) {
        this.id = id;
        this.idLieu = idLieu;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdLieu() { return idLieu; }
    public void setIdLieu(int idLieu) { this.idLieu = idLieu; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return "LieuImage{id=" + id + ", idLieu=" + idLieu + ", imagePath='" + imagePath + "'}";
    }
}
