package com.resumeai.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FactualConsistencyGuardTest {

    private final FactualConsistencyGuard guard = new FactualConsistencyGuard();

    @Test
    @DisplayName("rejects a rewrite that introduces a metric not present in the original")
    void rejectsFabricatedMetric() {
        String original = "Built a URL shortener using Spring Boot";
        String rewritten = "Built a URL shortener serving 1M users using Spring Boot";

        FactualConsistencyGuard.GuardResult result = guard.check(original, rewritten);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("number or metric");
    }

    @Test
    @DisplayName("rejects a rewrite that introduces a technology not present in the original")
    void rejectsFabricatedTechnology() {
        String original = "Developed a REST API for the checkout flow";
        String rewritten = "Developed a REST API for the checkout flow using Kubernetes and Docker";

        FactualConsistencyGuard.GuardResult result = guard.check(original, rewritten);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("technology");
    }

    @Test
    @DisplayName("passes a rewrite that only rephrases without adding new facts")
    void passesLegitimateRephrase() {
        String original = "Worked on a URL shortener using Spring Boot with REST APIs";
        String rewritten = "Developed a URL shortener leveraging Spring Boot and REST APIs";

        FactualConsistencyGuard.GuardResult result = guard.check(original, rewritten);

        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).isNull();
    }

    @Test
    @DisplayName("passes when the original already contains the metric and it is preserved verbatim")
    void passesWhenMetricAlreadyPresent() {
        String original = "Reduced page load time by 20% using caching";
        String rewritten = "Cut page load time by 20% through improved caching strategy";

        FactualConsistencyGuard.GuardResult result = guard.check(original, rewritten);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("rejects a rewrite that is suspiciously much longer than the original")
    void rejectsSuspiciouslyLongRewrite() {
        String original = "Wrote tests";
        String rewritten = "Wrote comprehensive unit tests, integration tests, and end to end tests "
                + "covering every possible edge case across the entire application stack, "
                + "significantly improving overall code quality and reliability for the team";

        FactualConsistencyGuard.GuardResult result = guard.check(original, rewritten);

        assertThat(result.passed()).isFalse();
    }

    @Test
    @DisplayName("rejects when a dollar-amount metric is fabricated")
    void rejectsFabricatedDollarMetric() {
        String original = "Managed the payments integration project";
        String rewritten = "Managed the payments integration project, saving the company $50000 annually";

        FactualConsistencyGuard.GuardResult result = guard.check(original, rewritten);

        assertThat(result.passed()).isFalse();
    }
}

