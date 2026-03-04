package Controllers;

import org.example.entities.Post;

public interface AppNavigator {
    void goBack();
    void showTabaaniConnect();
    void showHome();
    void showAddPost(Post toEdit);
    void showDetails(int postId);
    void showAdminLogin();
    void showAdminDashboard();
    void showChat();
}
