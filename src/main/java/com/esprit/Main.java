package com.esprit;

import com.esprit.entities.utilisateur;
import com.esprit.entities.role;
import com.esprit.services.utilisateurServices;
import com.esprit.services.roleServices;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static Scanner scanner = new Scanner(System.in);
    private static utilisateurServices us = new utilisateurServices();
    private static roleServices rs = new roleServices();

    public static void main(String[] args) {

        boolean running = true;

        while (running) {
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1. Gérer les utilisateurs");
            System.out.println("2. Gérer les rôles");
            System.out.println("3. Quitter");

            int choix = getIntInput("Votre choix : ");

            switch (choix) {
                case 1 -> menuUtilisateur();
                case 2 -> menuRole();
                case 3 -> running = false;
                default -> System.out.println("Choix invalide !");
            }
        }
    }

    // ===============================
    // GESTION UTILISATEUR
    // ===============================
    private static void menuUtilisateur() {
        try {
            System.out.println("\n1. Ajouter");
            System.out.println("2. Modifier");
            System.out.println("3. Supprimer");
            System.out.println("4. Afficher");

            int choix = getIntInput("Votre choix : ");

            switch (choix) {
                case 1 -> ajouterUtilisateur();
                case 2 -> modifierUtilisateur();
                case 3 -> supprimerUtilisateur();
                case 4 -> afficherUtilisateurs();
                default -> System.out.println("Choix invalide !");
            }

        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private static void ajouterUtilisateur() throws SQLException {

        System.out.print("Nom : ");
        String nom = scanner.nextLine();

        System.out.print("Prénom : ");
        String prenom = scanner.nextLine();

        System.out.print("Email : ");
        String email = scanner.nextLine();

        System.out.print("Mot de passe : ");
        String mdp = scanner.nextLine();

        System.out.print("NFC ID (optionnel) : ");
        String nfc = scanner.nextLine();
        if (nfc.isEmpty()) nfc = null;

        System.out.print("Role ID : ");
        int roleId = scanner.nextInt();
        scanner.nextLine();

        utilisateur u = new utilisateur(
                nom,
                prenom,
                email,
                mdp,
                "ACTIF",
                LocalDate.now(),
                nfc,
                roleId
        );

        us.ajouter(u);
        System.out.println("✓ Utilisateur ajouté !");
    }

    private static void modifierUtilisateur() throws SQLException {

        System.out.print("ID utilisateur : ");
        int id = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Nom : ");
        String nom = scanner.nextLine();

        System.out.print("Prénom : ");
        String prenom = scanner.nextLine();

        System.out.print("Email : ");
        String email = scanner.nextLine();

        System.out.print("Mot de passe : ");
        String mdp = scanner.nextLine();

        System.out.print("Statut (ACTIF/BLOQUE) : ");
        String statut = scanner.nextLine();

        System.out.print("NFC ID : ");
        String nfc = scanner.nextLine();

        System.out.print("Role ID : ");
        int roleId = scanner.nextInt();
        scanner.nextLine();

        utilisateur u = new utilisateur(
                id,
                nom,
                prenom,
                email,
                mdp,
                statut,
                LocalDate.now(),
                nfc,
                roleId
        );

        us.modifier(u);
        System.out.println("✓ Utilisateur modifié !");
    }

    private static void supprimerUtilisateur() throws SQLException {
        int id = getIntInput("ID utilisateur à supprimer : ");
        us.supprimer(id);
        System.out.println("✓ Utilisateur supprimé !");
    }

    private static void afficherUtilisateurs() throws SQLException {

        List<utilisateur> list = us.afficher();

        System.out.println("\nID | Nom | Prénom | Email | Statut | Role | NFC");
        System.out.println("------------------------------------------------");

        for (utilisateur u : list) {
            System.out.println(
                    u.getId() + " | " +
                            u.getNom() + " | " +
                            u.getPrenom() + " | " +
                            u.getEmail() + " | " +
                            u.getStatut() + " | " +
                            u.getRoleId() + " | " +
                            u.getNfcId()
            );
        }
    }

    // ===============================
    // GESTION ROLE
    // ===============================
    private static void menuRole() {
        try {
            System.out.println("\n1. Ajouter");
            System.out.println("2. Modifier");
            System.out.println("3. Supprimer");
            System.out.println("4. Afficher");

            int choix = getIntInput("Votre choix : ");

            switch (choix) {
                case 1 -> ajouterRole();
                case 2 -> modifierRole();
                case 3 -> supprimerRole();
                case 4 -> afficherRoles();
                default -> System.out.println("Choix invalide !");
            }

        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private static void ajouterRole() throws SQLException {

        System.out.print("Nom : ");
        String nom = scanner.nextLine();

        System.out.print("Description : ");
        String desc = scanner.nextLine();

        role r = new role(nom, desc);

        rs.ajouter(r);
        System.out.println("✓ Rôle ajouté !");
    }

    private static void modifierRole() throws SQLException {

        System.out.print("ID : ");
        int id = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Nom : ");
        String nom = scanner.nextLine();

        System.out.print("Description : ");
        String desc = scanner.nextLine();

        role r = new role(id, nom, desc);

        rs.modifier(r);
        System.out.println("✓ Rôle modifié !");
    }

    private static void supprimerRole() throws SQLException {
        int id = getIntInput("ID rôle à supprimer : ");
        rs.supprimer(id);
        System.out.println("✓ Rôle supprimé !");
    }

    private static void afficherRoles() throws SQLException {

        List<role> list = rs.afficher();

        System.out.println("\nID | Nom | Description");
        System.out.println("----------------------");

        for (role r : list) {
            System.out.println(
                    r.getId() + " | " +
                            r.getNom() + " | " +
                            r.getDescription()
            );
        }
    }

    // ===============================
    private static int getIntInput(String msg) {
        while (true) {
            try {
                System.out.print(msg);
                return scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Veuillez entrer un nombre valide !");
                scanner.nextLine();
            }
        }
    }
}