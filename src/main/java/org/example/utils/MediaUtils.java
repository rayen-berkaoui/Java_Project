package org.example.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class MediaUtils {

    private static final String UPLOADS_DIR = "uploads" + FileSystems.getDefault().getSeparator() + "posts";

    private MediaUtils() {}

    public static Path getUploadsPath() {
        String base = System.getProperty("user.dir");
        Path dir = Paths.get(base, UPLOADS_DIR.split("[\\\\/]"));
        return dir;
    }

    public static List<String> copyFilesToUploads(List<File> files) throws IOException {
        Path uploadDir = getUploadsPath();
        Files.createDirectories(uploadDir);
        List<String> savedPaths = new ArrayList<>();
        for (File f : files) {
            if (f == null || !f.exists() || !f.isFile()) continue;
            String ext = "";
            int dot = f.getName().lastIndexOf('.');
            if (dot > 0) ext = f.getName().substring(dot);
            String baseName = f.getName();
            if (dot > 0) baseName = baseName.substring(0, dot);
            baseName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = baseName + "_" + System.currentTimeMillis() + ext;
            Path dest = uploadDir.resolve(fileName);
            Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            savedPaths.add(dest.toAbsolutePath().toString());
        }
        return savedPaths;
    }
}
