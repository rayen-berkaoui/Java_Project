package com.esprit.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.esprit.entities.ForumComment;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ForumCommentCell extends ListCell<ForumComment> {

    private final ForumDetailsController detailsController;
    private final String currentUser;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT).withLocale(Locale.FRENCH);

    public ForumCommentCell(ForumDetailsController detailsController, String currentUser) {
        this.detailsController = detailsController;
        this.currentUser = currentUser;
    }

    @Override
    protected void updateItem(ForumComment comment, boolean empty) {
        super.updateItem(comment, empty);
        if (empty || comment == null) {
            setGraphic(null);
        } else {
            VBox card = new VBox(10);
            card.getStyleClass().add("comment-card");
            card.setMaxWidth(Double.MAX_VALUE);

            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            Label userLabel = new Label("👤 " + (comment.getUserKey() != null ? comment.getUserKey() : "Anonyme"));
            userLabel.getStyleClass().add("comment-user");

            String dateStr = comment.getCreatedAt() != null ? comment.getCreatedAt().format(dateFormat) : "";
            Label dateLabel = new Label(dateStr);
            dateLabel.getStyleClass().add("comment-date");

            header.getChildren().addAll(userLabel, dateLabel);
            card.getChildren().add(header);

            Label contentLabel = new Label(comment.getContent() != null ? comment.getContent() : "");
            contentLabel.getStyleClass().add("comment-content");
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(780);
            card.getChildren().add(contentLabel);

            HBox actions = new HBox(12);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setPadding(new Insets(4, 0, 0, 0));

            Integer commentId = comment.getId();
            boolean hasValidId = commentId != null;

            Button likeBtn = new Button("👍 " + comment.getLikesCount());
            likeBtn.getStyleClass().add("comment-action-btn");
            likeBtn.setDisable(!hasValidId);
            if (hasValidId) likeBtn.setOnAction(e -> detailsController.handleCommentLike(commentId));

            Button dislikeBtn = new Button("👎 " + comment.getDislikesCount());
            dislikeBtn.getStyleClass().add("comment-action-btn");
            dislikeBtn.setDisable(!hasValidId);
            if (hasValidId) dislikeBtn.setOnAction(e -> detailsController.handleCommentDislike(commentId));

            Button editBtn = new Button("Modifier");
            editBtn.getStyleClass().add("comment-action-btn");
            editBtn.setVisible(hasValidId && currentUser.equals(comment.getUserKey()));
            if (hasValidId) editBtn.setOnAction(e -> detailsController.handleEditComment(comment));

            Button deleteBtn = new Button("Supprimer");
            deleteBtn.getStyleClass().addAll("comment-action-btn", "comment-action-delete");
            deleteBtn.setVisible(hasValidId && currentUser.equals(comment.getUserKey()));
            if (hasValidId) deleteBtn.setOnAction(e -> detailsController.handleDeleteComment(commentId));

            actions.getChildren().addAll(likeBtn, dislikeBtn, editBtn, deleteBtn);
            card.getChildren().add(actions);

            card.setPadding(new Insets(12, 16, 12, 16));
            setGraphic(card);
        }
    }
}


