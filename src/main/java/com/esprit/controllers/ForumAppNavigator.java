package com.esprit.controllers;

import com.esprit.entities.ForumPost;

public interface ForumAppNavigator {
    void goBack();
    void showTabaaniConnect();
    void showHome();
    void showAddPost(ForumPost toEdit);
    void showDetails(int postId);
    void showAdminLogin();
    void showAdminDashboard();
    void showChat();
}


