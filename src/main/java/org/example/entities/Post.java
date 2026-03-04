package org.example.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Post {
    private static final String PATH_DELIMITER = "|||";
    public static final String TAG_DELIM = "\n<!--TAGS-->";

    private Integer id;
    private String content;
    private String hashtags;
    private String imagePath;
    private String videoPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {}

    public Post(String content, String imagePath, String videoPath, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.content = content;
        this.imagePath = imagePath;
        this.videoPath = videoPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;}

    public Integer getId() {return id;}

    public void setId(Integer id) {this.id = id;}

    public String getContent() {return content;}

    public void setContent(String content) {this.content = content;}

    public String getHashtags() { return hashtags; }
    public void setHashtags(String hashtags) { this.hashtags = hashtags; }

    public List<String> getHashtagList() {
        if (hashtags == null || hashtags.isBlank()) return new ArrayList<>();
        return Arrays.stream(hashtags.split("[,\\s#]+"))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    public static String encodeForDb(String content, String hashtags) {
        String c = content != null ? content : "";
        String h = (hashtags != null && !hashtags.isBlank()) ? hashtags.trim() : null;
        return h != null ? c + TAG_DELIM + h : c;
    }

    public static String[] decodeFromDb(String raw) {
        if (raw == null) return new String[]{ "", null };
        int i = raw.indexOf(TAG_DELIM);
        if (i < 0) return new String[]{ raw, null };
        return new String[]{ raw.substring(0, i), raw.substring(i + TAG_DELIM.length()).trim() };
    }

    public String getImagePath() {return imagePath;}

    public void setImagePath(String imagePath) {this.imagePath = imagePath;}

    public String getVideoPath() {return videoPath;}

    public void setVideoPath(String videoPath) {this.videoPath = videoPath;}

    public List<String> getImagePaths() {
        if (imagePath == null || imagePath.isBlank()) return new ArrayList<>();
        return Arrays.stream(imagePath.split("\\|\\|\\|")).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    public void setImagePaths(List<String> paths) {
        this.imagePath = paths == null || paths.isEmpty() ? null : String.join(PATH_DELIMITER, paths);
    }

    public List<String> getVideoPaths() {
        if (videoPath == null || videoPath.isBlank()) return new ArrayList<>();
        return Arrays.stream(videoPath.split("\\|\\|\\|")).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    public void setVideoPaths(List<String> paths) {
        this.videoPath = paths == null || paths.isEmpty() ? null : String.join(PATH_DELIMITER, paths);
    }

    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

    public LocalDateTime getUpdatedAt() {return updatedAt;}

    public void setUpdatedAt(LocalDateTime updatedAt) {this.updatedAt = updatedAt;}

    @Override
    public String toString() {
        return "Post{" + "id=" + id + ", content='" + content + '\'' + ", imagePath='" + imagePath + '\'' + ", videoPath='" + videoPath + '\'' + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}
