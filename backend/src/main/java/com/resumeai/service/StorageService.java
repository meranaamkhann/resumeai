package com.resumeai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class StorageService {

    private final Path storageRoot;

    public StorageService(@Value("${app.upload.storage-dir}") String storageDir) {
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory: " + storageRoot, e);
        }
    }

    public String store(UUID userId, UUID documentId, byte[] bytes) {
        String key = userId + "/" + documentId + ".bin";
        Path target = resolveWithinRoot(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }
        return key;
    }

    public byte[] read(String storageKey) {
        Path target = resolveWithinRoot(storageKey);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored file", e);
        }
    }

    public void delete(String storageKey) {
        Path target = resolveWithinRoot(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete stored file", e);
        }
    }

    private Path resolveWithinRoot(String key) {
        Path resolved = storageRoot.resolve(key).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new SecurityException("Path traversal attempt detected for storage key: " + key);
        }
        return resolved;
    }
}

