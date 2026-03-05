package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class ForumChatPlaceholderController implements Initializable {

    private ForumAppNavigator navigator;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void setNavigator(ForumAppNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void goBack() {
        if (navigator != null) {
            navigator.showTabaaniConnect();
        }
    }
}


