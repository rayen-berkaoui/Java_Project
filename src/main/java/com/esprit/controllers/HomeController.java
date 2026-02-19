package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    private void goActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_view.fxml", event);   // ou /activite.fxml selon ton nom
    }

    @FXML
    private void goEtablissements(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }
}
