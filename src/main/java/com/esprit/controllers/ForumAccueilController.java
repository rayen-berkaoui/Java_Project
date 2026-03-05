package com.esprit.controllers;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import com.esprit.entities.ForumPost;
import com.esprit.services.*;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ForumAccueilController implements Initializable {
    private static final int[] PAGE_SIZES = { 10, 20, 50, 100 };
    private static final int DEFAULT_PAGE_SIZE = 10;

    private ForumAppNavigator navigator;

    @FXML private ListView<ForumPost> postsListView;
    @FXML private TextField searchField;
    @FXML private TextField hashtagField;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private Label paginationInfoLabel;
    @FXML private Label pageIndicatorLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final ForumPostService postService = new ForumPostService();
    private final ForumActivityService activityService = new ForumActivityService();
    private final ForumCommentService commentService = new ForumCommentService();

    private List<ForumPost> allFilteredPosts = new ArrayList<>();
    private int currentPage = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        postsListView.setCellFactory(lv -> new ForumPostCell(activityService, commentService, this::filterByHashtag));
        pageSizeCombo.setItems(FXCollections.observableArrayList(
            PAGE_SIZES[0], PAGE_SIZES[1], PAGE_SIZES[2], PAGE_SIZES[3]
        ));
        pageSizeCombo.getSelectionModel().select(Integer.valueOf(DEFAULT_PAGE_SIZE));
        loadPosts();
        postsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ForumPost selected = postsListView.getSelectionModel().getSelectedItem();
                if (selected != null) openDetails(selected);
            }
        });
    }

    private void openDetails(ForumPost post) {
        if (post != null && post.getId() != null && navigator != null) {
            navigator.showDetails(post.getId());
        }
    }

    public void setNavigator(ForumAppNavigator navigator) {
        this.navigator = navigator;
    }

    public void filterByHashtag(String tag) {
        if (hashtagField != null) {
            hashtagField.setText(tag);
            applyFilters();
        }
    }

    private void loadPosts() {
        applyFilters();
    }

    @FXML
    private void applyFilters() {
        try {
            String search = searchField != null && searchField.getText() != null ? searchField.getText().trim() : "";
            String hashtagsInput = hashtagField != null && hashtagField.getText() != null ? hashtagField.getText().trim() : "";
            LocalDateTime from = dateFrom != null && dateFrom.getValue() != null
                    ? dateFrom.getValue().atStartOfDay() : null;
            LocalDateTime to = dateTo != null && dateTo.getValue() != null
                    ? dateTo.getValue().atTime(LocalTime.MAX) : null;

            if (search.isEmpty() && hashtagsInput.isEmpty() && from == null && to == null) {
                allFilteredPosts = postService.afficher();
            } else {
                allFilteredPosts = postService.filterPosts(search.isEmpty() ? null : search,
                        hashtagsInput.isEmpty() ? null : hashtagsInput, from, to);
            }
            currentPage = 1;
            updatePagination();

            if (allFilteredPosts.isEmpty() && !postService.isConnected()) {
                showAlert("Information", "Base de données non disponible — mode hors-ligne.");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Chargement: " + e.getMessage());
        }
    }

    private void updatePagination() {
        int total = allFilteredPosts.size();
        int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / pageSize);
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ForumPost> pageItems = total == 0 ? new ArrayList<>() : allFilteredPosts.subList(fromIndex, toIndex);

        postsListView.getItems().setAll(pageItems);

        if (paginationInfoLabel != null) {
            if (total == 0) {
                paginationInfoLabel.setText("Aucun résultat");
            } else {
                paginationInfoLabel.setText("Affichage " + (fromIndex + 1) + " - " + toIndex + " sur " + total);
            }
        }
        if (pageIndicatorLabel != null) {
            pageIndicatorLabel.setText("Page " + currentPage + " / " + totalPages);
        }
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= totalPages || total == 0);
    }

    @FXML
    private void goToPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    private void goToNextPage() {
        int totalPages = allFilteredPosts.isEmpty() ? 1 : (int) Math.ceil((double) allFilteredPosts.size() / pageSize);
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    @FXML
    private void onPageSizeChanged() {
        Integer selected = pageSizeCombo != null ? pageSizeCombo.getSelectionModel().getSelectedItem() : null;
        if (selected != null && selected.intValue() != pageSize) {
            pageSize = selected.intValue();
            currentPage = 1;
            updatePagination();
        }
    }

    @FXML
    private void clearFilters() {
        if (searchField != null) searchField.clear();
        if (hashtagField != null) hashtagField.clear();
        if (dateFrom != null) dateFrom.setValue(null);
        if (dateTo != null) dateTo.setValue(null);
        applyFilters();
    }

    @FXML
    private void handleAdmin() {
        if (navigator != null) navigator.showAdminLogin();
    }

    @FXML
    private void handleAddPost() {
        if (navigator != null) navigator.showAddPost(null);
    }

    private void showAlert(String title, String message) {
        com.esprit.utils.ForumStyledDialog.show(title, message);
    }
}


