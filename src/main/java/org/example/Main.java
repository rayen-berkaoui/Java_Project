package org.example;

import org.example.entities.Post;
import org.example.services.PostService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        PostService postService = new PostService();

        try {
            Post post = new Post();
            post.setContent("Post de test avec image et vidéo");
            post.setImagePath("/images/photo.jpg");
            post.setVideoPath("/videos/video.mp4");
            post.setCreatedAt(LocalDateTime.now());

            postService.ajouter(post);
            System.out.println("✅ Post ajouté avec ID: " + post.getId());
            System.out.println("    Contenu: " + post.getContent());
            System.out.println("    Image: " + post.getImagePath());
            System.out.println("    Vidéo: " + post.getVideoPath());

        } catch (SQLException e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
