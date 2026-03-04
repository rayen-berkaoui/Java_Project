package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class TabaaniConnectController implements Initializable {

    private AppNavigator navigator;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void setNavigator(AppNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void openEspacePostes() {
        if (navigator != null) {
            navigator.showHome();
        }
    }

    @FXML
    private void openEspaceChat() {
        if (navigator != null) {
            navigator.showChat();
        }
    }
}
