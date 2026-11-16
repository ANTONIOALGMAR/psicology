package com.psicology.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public String store(MultipartFile file, String subdir) throws IOException {
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String safeName = UUID.randomUUID() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = root.resolve(subdir).normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return root.relativize(target).toString().replace("\\", "/");
    }

    public Resource loadAsResource(String relativePath) throws MalformedURLException {
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Caminho inválido");
        return new UrlResource(file.toUri());
    }

    public void delete(String relativePath) throws IOException {
        if (relativePath == null) return;
        Path file = root.resolve(relativePath).normalize();
        if (file.startsWith(root)) Files.deleteIfExists(file);
    }
}
