package com.resumeai.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdvisoryLockService {

    private final JdbcTemplate jdbcTemplate;

    public AdvisoryLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryAcquireForTransaction(UUID resourceId) {
        long key = resourceId.getMostSignificantBits() ^ resourceId.getLeastSignificantBits();
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)", Boolean.class, key);
        return Boolean.TRUE.equals(acquired);
    }
}

