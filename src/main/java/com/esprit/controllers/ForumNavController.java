package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import com.esprit.entities.ForumPost;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ResourceBundle;

public class ForumNavController implements Initializable, ForumAppNavigator {

    @FXML private StackPane contentPane;
    @FXML private Button backBtn;
    @FXML private Button addPostBtn;
    @FXML private Button adminBtn;

    private final Deque<ViewState> viewStack = new ArrayDeque<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showTabaaniConnect();
    }

    @Override
    public void goBack() {
        if (viewStack.size() <= 1) return;
        viewStack.pop();
        ViewState prev = viewStack.peek();
        if (prev != null) loadView(prev);
        updateBackButton();
    }

    @Override
    public void showTabaaniConnect() {
        viewStack.clear();
        viewStack.push(ViewState.TABAANI_CONNECT);
        loadView(ViewState.TABAANI_CONNECT);
        updateBackButton();
    }

    @Override
    public void showHome() {
        viewStack.clear();
        viewStack.push(ViewState.TABAANI_CONNECT);
        viewStack.push(ViewState.HOME);
        loadView(ViewState.HOME);
        updateBackButton();
    }

    @Override
    public void showChat() {
        viewStack.push(ViewState.CHAT);
        loadView(ViewState.CHAT);
        updateBackButton();
    }

    @Override
    public void showAddPost(ForumPost toEdit) {
        viewStack.push(new ViewState(ViewState.Type.ADD_POST, toEdit, -1));
        loadView(viewStack.peek());
        updateBackButton();
    }

    @Override
    public void showDetails(int postId) {
        viewStack.push(new ViewState(ViewState.Type.DETAILS, null, postId));
        loadView(viewStack.peek());
        updateBackButton();
    }

    @Override
    public void showAdminLogin() {
        viewStack.push(ViewState.ADMIN_LOGIN);
        loadView(ViewState.ADMIN_LOGIN);
        updateBackButton();
    }

    @Override
    public void showAdminDashboard() {
        viewStack.pop();
        viewStack.push(ViewState.ADMIN_DASHBOARD);
        loadView(ViewState.ADMIN_DASHBOARD);
        updateBackButton();
    }

    private void updateBackButton() {
        boolean show = viewStack.size() > 1;
        backBtn.setVisible(show);
        backBtn.setManaged(show);
    }

    private void updateAddPostButton() {
        if (addPostBtn == null) return;
        ViewState current = viewStack.peek();
        boolean inPostSpace = current != null &&
                (current.type == ViewState.Type.HOME ||
                 current.type == ViewState.Type.DETAILS ||
                 current.type == ViewState.Type.ADD_POST);
        addPostBtn.setVisible(inPostSpace);
        addPostBtn.setManaged(inPostSpace);

        if (adminBtn != null) {
            boolean onMain = current != null && current.type == ViewState.Type.TABAANI_CONNECT;
            adminBtn.setVisible(onMain);
            adminBtn.setManaged(onMain);
        }
    }

    private void loadView(ViewState state) {
        try {
            Parent root = loadViewContent(state);
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
            updateAddPostButton();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Parent loadViewContent(ViewState state) throws IOException {
        switch (state.type) {
            case TABAANI_CONNECT:
                FXMLLoader connectLoader = new FXMLLoader(getClass().getResource("/forum/tabaani-connect.fxml"));
                Parent connectRoot = connectLoader.load();
                ForumTabaaniConnectController connectCtrl = connectLoader.getController();
                connectCtrl.setNavigator(this);
                return connectRoot;

            case CHAT:
                FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/forum/chat.fxml"));
                Parent chatRoot = chatLoader.load();
                ForumChatController chatCtrl = chatLoader.getController();
                chatCtrl.setNavigator(this);
                return chatRoot;

            case HOME:
                FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/forum/ForumAccueilController.fxml"));
                Parent homeRoot = homeLoader.load();
                ForumAccueilController homeCtrl = homeLoader.getController();
                homeCtrl.setNavigator(this);
                return homeRoot;

            case ADD_POST:
                FXMLLoader addLoader = new FXMLLoader(getClass().getResource("/forum/ForumAddPostController.fxml"));
                Parent addRoot = addLoader.load();
                ForumAddPostController addCtrl = addLoader.getController();
                addCtrl.setNavigator(this);
                addCtrl.setPostToEdit(state.postToEdit);
                return addRoot;

            case DETAILS:
                FXMLLoader detailsLoader = new FXMLLoader(getClass().getResource("/forum/PostDetails.fxml"));
                Parent detailsRoot = detailsLoader.load();
                ForumDetailsController detailsCtrl = detailsLoader.getController();
                detailsCtrl.setNavigator(this);
                detailsCtrl.setPostId(state.postId);
                detailsCtrl.setRefreshCallback(this::refreshHomeIfVisible);
                return detailsRoot;

            case ADMIN_LOGIN:
                FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/forum/admin-login.fxml"));
                Parent loginRoot = loginLoader.load();
                ForumAdminLoginController loginCtrl = loginLoader.getController();
                loginCtrl.setNavigator(this);
                return loginRoot;

            case ADMIN_DASHBOARD:
                FXMLLoader dashLoader = new FXMLLoader(getClass().getResource("/forum/admin-dashboard.fxml"));
                Parent dashRoot = dashLoader.load();
                ForumAdminDashboardController dashCtrl = dashLoader.getController();
                dashCtrl.setNavigator(this);
                return dashRoot;

            default:
                return loadViewContent(ViewState.HOME);
        }
    }

    private void refreshHomeIfVisible() {
        if (!viewStack.isEmpty() && viewStack.peek().type == ViewState.Type.HOME) {
            loadView(ViewState.HOME);
        }
    }

    @FXML
    private void handleBack() {
        goBack();
    }

    @FXML
    private void handleAdmin() {
        showAdminLogin();
    }

    @FXML
    private void handleAddPost() {
        showAddPost(null);
    }

    private static class ViewState {
        enum Type { TABAANI_CONNECT, CHAT, HOME, ADD_POST, DETAILS, ADMIN_LOGIN, ADMIN_DASHBOARD }
        final Type type;
        final ForumPost postToEdit;
        final int postId;

        static final ViewState TABAANI_CONNECT = new ViewState(Type.TABAANI_CONNECT, null, -1);
        static final ViewState CHAT = new ViewState(Type.CHAT, null, -1);
        static final ViewState HOME = new ViewState(Type.HOME, null, -1);
        static final ViewState ADMIN_LOGIN = new ViewState(Type.ADMIN_LOGIN, null, -1);
        static final ViewState ADMIN_DASHBOARD = new ViewState(Type.ADMIN_DASHBOARD, null, -1);

        ViewState(Type type, ForumPost postToEdit, int postId) {
            this.type = type;
            this.postToEdit = postToEdit;
            this.postId = postId;
        }
    }
}


