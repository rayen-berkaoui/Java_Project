package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ForumAdminLoginController {

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private ForumAppNavigator navigator;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String user = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String pass = passwordField.getText() != null ? passwordField.getText() : "";

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir identifiant et mot de passe.");
            return;
        }

        if (ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass)) {
            if (navigator != null) navigator.showAdminDashboard();
        } else {
            showAlert("Accès refusé", "Identifiant ou mot de passe incorrect.");
        }
    }

    private void showAlert(String title, String message) {
        com.esprit.utils.ForumStyledDialog.show(title, message);
    }

    public void setNavigator(ForumAppNavigator navigator) {
        this.navigator = navigator;
    }
}


