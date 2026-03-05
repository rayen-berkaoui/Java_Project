package com.esprit.utils;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Optional;

public class ForumStyledDialog {

    private static final String BG_DARK = "#0a0a0a";
    private static final String BG_CARD = "#111111";
    private static final String BORDER_GOLD = "rgba(255, 215, 0, 0.15)";
    private static final String GOLD = "#FFD700";
    private static final String GOLD_DIM = "#FFAA00";
    private static final String TEXT_PRIMARY = "#f0f0f0";
    private static final String TEXT_SECONDARY = "#999999";
    private static final String RED = "#FF4444";
    private static final String GREEN = "#4CAF50";
    private static final String ORANGE = "#FFB347";

    private static final String DIALOG_PANE_CSS = """
            -fx-background-color: %s;
            -fx-border-color: %s;
            -fx-border-width: 1;
            -fx-border-radius: 14;
            -fx-background-radius: 14;
            -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.08), 30, 0, 0, 6);
            """.formatted(BG_DARK, BORDER_GOLD);

    private static final String HEADER_LABEL_CSS = """
            -fx-text-fill: %s;
            -fx-font-size: 14;
            -fx-font-weight: bold;
            -fx-padding: 0;
            """.formatted(GOLD);

    private static final String CONTENT_LABEL_CSS = """
            -fx-text-fill: %s;
            -fx-font-size: 12.5;
            -fx-wrap-text: true;
            -fx-padding: 4 0 4 0;
            """.formatted(TEXT_PRIMARY);

    private static final String BUTTON_BASE_CSS = """
            -fx-font-size: 12;
            -fx-font-weight: bold;
            -fx-padding: 8 22;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-border-radius: 8;
            """;

    private static final String BUTTON_PRIMARY_CSS = BUTTON_BASE_CSS + """
            -fx-background-color: linear-gradient(to right, #FFD700, #FFAA00);
            -fx-text-fill: #000000;
            -fx-border-color: transparent;
            """;

    private static final String BUTTON_SECONDARY_CSS = BUTTON_BASE_CSS + """
            -fx-background-color: rgba(255,255,255,0.06);
            -fx-text-fill: #cccccc;
            -fx-border-color: rgba(255,255,255,0.1);
            -fx-border-width: 1;
            """;

    private static final String BUTTON_DANGER_CSS = BUTTON_BASE_CSS + """
            -fx-background-color: linear-gradient(to right, #FF4444, #CC0000);
            -fx-text-fill: white;
            -fx-border-color: transparent;
            """;

    private static final String TEXT_FIELD_CSS = """
            -fx-background-color: #1a1a1a;
            -fx-text-fill: white;
            -fx-prompt-text-fill: #555;
            -fx-background-radius: 8;
            -fx-border-color: rgba(255,215,0,0.12);
            -fx-border-radius: 8;
            -fx-border-width: 1;
            -fx-padding: 8 12;
            -fx-font-size: 12;
            """;

    public static void showInfo(String title, String message) {
        showStyled("ℹ️", title, message, GOLD, Alert.AlertType.INFORMATION);
    }

    public static void showSuccess(String title, String message) {
        showStyled("✅", title, message, GREEN, Alert.AlertType.INFORMATION);
    }

    public static void showWarning(String title, String message) {
        showStyled("⚠️", title, message, ORANGE, Alert.AlertType.WARNING);
    }

    public static void showError(String title, String message) {
        showStyled("❌", title, message, RED, Alert.AlertType.ERROR);
    }

    public static void show(String title, String message) {
        if (title == null) title = "";
        String t = title.toLowerCase();
        if (t.contains("erreur") || t.contains("error") || t.contains("impossible")) {
            showError(title, message);
        } else if (t.contains("succès") || t.contains("succes") || t.contains("success") || t.contains("modifié") || t.contains("supprimé")) {
            showSuccess(title, message);
        } else if (t.contains("refus") || t.contains("accès") || t.contains("interdit") || t.contains("banni")
                || t.contains("expulsé") || t.contains("bloqué") || t.contains("non autorisé") || t.contains("warning")) {
            showWarning(title, message);
        } else {
            showInfo(title, message);
        }
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.TRANSPARENT);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText(null);

        ButtonType btnOui = new ButtonType("✓ Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("✗ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnOui, btnNon);

        DialogPane pane = alert.getDialogPane();
        styleDialogPane(pane);

        VBox content = buildContent("⚡", title, message, GOLD);
        pane.setContent(content);

        styleButton(pane, btnOui, BUTTON_PRIMARY_CSS);
        styleButton(pane, btnNon, BUTTON_SECONDARY_CSS);

        animateIn(pane);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnOui;
    }

    public static boolean confirm(String message) {
        return confirm("Confirmer", message);
    }

    public static Optional<String> textInput(String title, String header, String prompt, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(null);
        dialog.setHeaderText(null);
        dialog.setContentText(null);

        DialogPane pane = dialog.getDialogPane();
        styleDialogPane(pane);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 20, 8, 20));

        Label icon = new Label("✏️");
        icon.setStyle("-fx-font-size: 28;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle(HEADER_LABEL_CSS);

        Label headerLabel = new Label(header);
        headerLabel.setStyle(CONTENT_LABEL_CSS);

        Label promptLabel = new Label(prompt);
        promptLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 11; -fx-padding: 4 0 2 0;");

        TextField textField = dialog.getEditor();
        textField.setStyle(TEXT_FIELD_CSS);
        textField.setPrefWidth(300);

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setMaxWidth(Double.MAX_VALUE);
        separator.setStyle("-fx-background-color: " + BORDER_GOLD + ";");

        content.getChildren().addAll(icon, titleLabel, separator, headerLabel, promptLabel, textField);
        content.setAlignment(Pos.CENTER_LEFT);
        pane.setContent(content);

        ButtonType okType = dialog.getDialogPane().getButtonTypes().stream()
                .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().orElse(ButtonType.OK);
        ButtonType cancelType = dialog.getDialogPane().getButtonTypes().stream()
                .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE).findFirst().orElse(ButtonType.CANCEL);

        styleButton(pane, okType, BUTTON_PRIMARY_CSS);
        styleButton(pane, cancelType, BUTTON_SECONDARY_CSS);

        animateIn(pane);
        return dialog.showAndWait();
    }

    private static void showStyled(String emoji, String title, String message, String accentColor, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.initStyle(StageStyle.TRANSPARENT);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText(null);

        ButtonType btnOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnOk);

        DialogPane pane = alert.getDialogPane();
        styleDialogPane(pane);

        VBox content = buildContent(emoji, title, message, accentColor);
        pane.setContent(content);

        styleButton(pane, btnOk, BUTTON_PRIMARY_CSS);

        animateIn(pane);
        alert.showAndWait();
    }

    private static VBox buildContent(String emoji, String title, String message, String accentColor) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(16, 20, 8, 20));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(340);
        box.setMaxWidth(480);

        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 32;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("""
                -fx-text-fill: %s;
                -fx-font-size: 15;
                -fx-font-weight: bold;
                -fx-padding: 2 0 0 0;
                """.formatted(accentColor));

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setMaxWidth(Double.MAX_VALUE);
        separator.setStyle("-fx-background-color: " + BORDER_GOLD + ";");
        VBox.setMargin(separator, new Insets(2, 0, 2, 0));

        Label msgLabel = new Label(message);
        msgLabel.setStyle(CONTENT_LABEL_CSS);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(420);

        box.getChildren().addAll(iconLabel, titleLabel, separator, msgLabel);
        return box;
    }

    private static void styleDialogPane(DialogPane pane) {
        pane.setStyle(DIALOG_PANE_CSS);
        pane.setPrefWidth(420);

        pane.getScene().setFill(Color.TRANSPARENT);
        Stage stage = (Stage) pane.getScene().getWindow();
        if (stage != null) {
            stage.initStyle(StageStyle.TRANSPARENT);
        }

        pane.lookup(".content.label") ;
        pane.setGraphic(null);
        pane.setHeaderText(null);

        pane.applyCss();
    }

    private static void styleButton(DialogPane pane, ButtonType buttonType, String css) {
        Button btn = (Button) pane.lookupButton(buttonType);
        if (btn != null) {
            btn.setStyle(css);

            String hoverCss;
            if (css.contains("#FFD700")) {
                hoverCss = css.replace("linear-gradient(to right, #FFD700, #FFAA00)",
                        "linear-gradient(to right, #FFAA00, #FF8C00)");
            } else if (css.contains("#FF4444")) {
                hoverCss = css.replace("linear-gradient(to right, #FF4444, #CC0000)",
                        "linear-gradient(to right, #FF6666, #EE2222)");
            } else {
                hoverCss = css.replace("rgba(255,255,255,0.06)", "rgba(255,255,255,0.12)");
            }
            btn.setOnMouseEntered(e -> btn.setStyle(hoverCss));
            btn.setOnMouseExited(e -> btn.setStyle(css));
        }
    }

    private static void animateIn(DialogPane pane) {
        pane.setOpacity(0);
        pane.setScaleX(0.92);
        pane.setScaleY(0.92);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(pane.opacityProperty(), 0),
                        new KeyValue(pane.scaleXProperty(), 0.92),
                        new KeyValue(pane.scaleYProperty(), 0.92)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(pane.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(pane.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(pane.scaleYProperty(), 1, Interpolator.EASE_OUT)
                )
        );
        timeline.play();
    }
}


