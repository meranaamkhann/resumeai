package com.resumeai.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class DocumentProcessingLockRegistry {

    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock lockFor(UUID documentId) {
        return locks.computeIfAbsent(documentId, k -> new ReentrantLock());
    }

    public void release(UUID documentId, ReentrantLock lock) {
        locks.remove(documentId, lock);
    }
}

