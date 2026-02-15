package com.esprit.entities;

import java.time.LocalDateTime;

public class Reservation {

    private int idReservation;
    private int idPanier;
    private LocalDateTime datePaiement;
    private double montantTotal;
    private String modePaiement;
    private String statutPaiement;      // Payé, En cours de paiement, Remboursé
    private String codeConfirmation;

    // Joined fields (for display)
    private String typeService;
    private String nomEtablissement;
    private int nbPersonnes;

    public Reservation() {}

    public Reservation(int idPanier, LocalDateTime datePaiement, double montantTotal,
                       String modePaiement, String statutPaiement, String codeConfirmation) {
        this.idPanier = idPanier;
        this.datePaiement = datePaiement;
        this.montantTotal = montantTotal;
        this.modePaiement = modePaiement;
        this.statutPaiement = statutPaiement;
        this.codeConfirmation = codeConfirmation;
    }

    // Getters & Setters
    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public LocalDateTime getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDateTime datePaiement) { this.datePaiement = datePaiement; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public String getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(String statutPaiement) { this.statutPaiement = statutPaiement; }

    public String getCodeConfirmation() { return codeConfirmation; }
    public void setCodeConfirmation(String codeConfirmation) { this.codeConfirmation = codeConfirmation; }

    public String getTypeService() { return typeService; }
    public void setTypeService(String typeService) { this.typeService = typeService; }

    public String getNomEtablissement() { return nomEtablissement; }
    public void setNomEtablissement(String nomEtablissement) { this.nomEtablissement = nomEtablissement; }

    public int getNbPersonnes() { return nbPersonnes; }
    public void setNbPersonnes(int nbPersonnes) { this.nbPersonnes = nbPersonnes; }

    @Override
    public String toString() {
        return "Reservation{" +
                "idReservation=" + idReservation +
                ", montantTotal=" + montantTotal +
                ", modePaiement='" + modePaiement + '\'' +
                ", statutPaiement='" + statutPaiement + '\'' +
                ", codeConfirmation='" + codeConfirmation + '\'' +
                '}';
    }
}
