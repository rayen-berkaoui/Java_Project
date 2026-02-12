package com.esprit.entities;

import java.time.LocalDate;

public class utilisateur {

    // Attributs
    private int id;                 // id utilisateur
    private String nom;             // nom
    private String prenom;          // prénom
    private String email;           // email
    private String motDePasse;      // mot de passe
    private String statut;          // ACTIF / BLOQUE
    private LocalDate dateCreation; // date de création
    private String nfcId;           // identifiant NFC
    private int roleId;             // clé étrangère vers role

    // Constructeur vide
    public utilisateur() {}

    // Constructeur sans id (ajout)
    public utilisateur(String nom, String prenom, String email,
                       String motDePasse, String statut,
                       LocalDate dateCreation, String nfcId, int roleId) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.nfcId = nfcId;
        this.roleId = roleId;
    }

    // Constructeur avec id (modification / lecture DB)
    public utilisateur(int id, String nom, String prenom, String email,
                       String motDePasse, String statut,
                       LocalDate dateCreation, String nfcId, int roleId) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.nfcId = nfcId;
        this.roleId = roleId;
    }

    // Getters & Setters
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
                '}';
    }
}