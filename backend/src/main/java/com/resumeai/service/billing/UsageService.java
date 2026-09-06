package com.resumeai.service.billing;

import com.resumeai.entity.UsageRecord;
import com.resumeai.entity.User;
import com.resumeai.exception.ApiException;
import com.resumeai.repository.UsageRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

@Service
public class UsageService {

    private final UsageRecordRepository usageRecordRepository;
    private final int freeAnalysesPerMonth;
    private final int freeJdMatchesPerMonth;
    private final int freeOptimizationsPerMonth;

    public enum UsageType { ANALYSIS, JD_MATCH, OPTIMIZATION }

    public UsageService(UsageRecordRepository usageRecordRepository,
                         @Value("${app.usage.free-analyses-per-month}") int freeAnalysesPerMonth,
                         @Value("${app.usage.free-jd-matches-per-month}") int freeJdMatchesPerMonth,
                         @Value("${app.usage.free-optimizations-per-month}") int freeOptimizationsPerMonth) {
        this.usageRecordRepository = usageRecordRepository;
        this.freeAnalysesPerMonth = freeAnalysesPerMonth;
        this.freeJdMatchesPerMonth = freeJdMatchesPerMonth;
        this.freeOptimizationsPerMonth = freeOptimizationsPerMonth;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndIncrement(UUID userId, User.Plan plan, UsageType type) {
        String period = YearMonth.now().toString();

        UsageRecord record = usageRecordRepository.findForUpdate(userId, period)
                .orElseGet(() -> usageRecordRepository.save(
                        UsageRecord.builder().userId(userId).period(period).build()));

        if (plan == User.Plan.FREE) {
            int current = switch (type) {
                case ANALYSIS -> record.getAnalysisCount();
                case JD_MATCH -> record.getJdMatchCount();
                case OPTIMIZATION -> record.getOptimizationCount();
            };
            int limit = switch (type) {
                case ANALYSIS -> freeAnalysesPerMonth;
                case JD_MATCH -> freeJdMatchesPerMonth;
                case OPTIMIZATION -> freeOptimizationsPerMonth;
            };
            if (current >= limit) {
                throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "QUOTA_EXCEEDED",
                        "You've reached your free plan's monthly limit for this feature (" + limit + "/month). Upgrade to Pro for higher limits.");
            }
        }

        switch (type) {
            case ANALYSIS -> record.setAnalysisCount(record.getAnalysisCount() + 1);
            case JD_MATCH -> record.setJdMatchCount(record.getJdMatchCount() + 1);
            case OPTIMIZATION -> record.setOptimizationCount(record.getOptimizationCount() + 1);
        }
        usageRecordRepository.save(record);
    }
}

