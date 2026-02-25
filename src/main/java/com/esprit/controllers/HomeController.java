package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class HomeController {

    @FXML private StackPane rootStack;

    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(() -> ChatbotPanel.install(rootStack));
    }

    @FXML
    private void goActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }

    @FXML
    private void goEtablissements(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }

    @FXML
    private void goTableau(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_tableau.fxml", event);
    }

    @FXML
    private void goTableauActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_tableau.fxml", event);
    }
}
