package com.resumeai.repository;

import com.resumeai.entity.UsageRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsageRecord u where u.userId = :userId and u.period = :period")
    Optional<UsageRecord> findForUpdate(UUID userId, String period);
}

