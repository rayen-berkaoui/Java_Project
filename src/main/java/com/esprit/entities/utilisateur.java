package com.esprit.entities;

import java.time.LocalDate;

public class utilisateur {

    // =========================
    // Attributs
    // =========================
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String statut;
    private LocalDate dateCreation;
    private String nfcId;
    private int roleId;
    private int numTel;
    private String profilePicture;   // base64 encoded image
    private String faceEncoding;      // stored face encoding for face recognition
    private double faceConfidence;    // best match confidence score (0.0 - 1.0)
    private int faceSamplesCount;     // number of face samples stored
    private LocalDate lastFaceLogin;  // last successful face login date
    private String totpSecret;        // TOTP secret key for Google Authenticator 2FA
    private boolean totpEnabled;      // whether 2FA is enabled for this user

    // =========================
    // Constructeur vide
    // =========================
    public utilisateur() {}

    // =========================
    // Constructeur sans id (AJOUT)
    // =========================
    public utilisateur(String nom, String prenom, String email,
                       String motDePasse, String statut,
                       LocalDate dateCreation, String nfcId,
                       int roleId, int numTel) {

        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.nfcId = nfcId;
        this.roleId = roleId;
        this.numTel = numTel;
    }

    // =========================
    // Constructeur avec id
    // =========================
    public utilisateur(int id, String nom, String prenom, String email,
                       String motDePasse, String statut,
                       LocalDate dateCreation, String nfcId,
                       int roleId, int numTel) {

        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.nfcId = nfcId;
        this.roleId = roleId;
        this.numTel = numTel;
    }

    // =========================
    // Getters & Setters
    // =========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getNfcId() {
        return nfcId;
    }

    public void setNfcId(String nfcId) {
        this.nfcId = nfcId;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public int getNumTel() {
        return numTel;
    }

    public void setNumTel(int numTel) {
        this.numTel = numTel;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getFaceEncoding() {
        return faceEncoding;
    }

    public void setFaceEncoding(String faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    public double getFaceConfidence() {
        return faceConfidence;
    }

    public void setFaceConfidence(double faceConfidence) {
        this.faceConfidence = faceConfidence;
    }

    public int getFaceSamplesCount() {
        return faceSamplesCount;
    }

    public void setFaceSamplesCount(int faceSamplesCount) {
        this.faceSamplesCount = faceSamplesCount;
    }

    public LocalDate getLastFaceLogin() {
        return lastFaceLogin;
    }

    public void setLastFaceLogin(LocalDate lastFaceLogin) {
        this.lastFaceLogin = lastFaceLogin;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    // =========================
    // toString()
    // =========================
    @Override
    public String toString() {
        return "utilisateur{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", statut='" + statut + '\'' +
                ", dateCreation=" + dateCreation +
                ", nfcId='" + nfcId + '\'' +
                ", roleId=" + roleId +
                ", numTel=" + numTel +
                ", profilePicture=" + (profilePicture != null ? "[base64]" : "null") +
                '}';
    }
}