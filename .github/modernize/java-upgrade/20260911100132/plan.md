# Upgrade Plan: resumeai (20260911100132)

- **Generated**: 2026-09-11
- **HEAD Branch**: current working branch (pre-existing changes preserved)
- **HEAD Commit ID**: repository status inspected; working tree contains pre-existing changes

## Available Tools

**JDKs**
- JDK 17.0.10: C:\Users\Asad Khan\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.10.7-hotspot\bin (current project JDK, baseline)
- JDK 25.0.2: C:\Users\Asad Khan\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.2\bin (target runtime)

**Build Tools**
- Maven 3.9.16: C:\Users\Asad Khan\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin

## Guidelines

- Upgrade the Java runtime to the latest LTS version available for this request: Java 25.
- Run in auto-execution mode.
- Preserve unrelated pre-existing working-tree changes.

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: appmod/java-upgrade-20260911100132
- Run tests before and after the upgrade: true

## Upgrade Goals

- Java runtime and compilation target: 25

## Technology Stack

| Technology/Dependency | Current | Min Compatible Version | Why Incompatible |
| --------------------- | ------- | ---------------------- | ---------------- |
| Java | 17 | 25 | User-requested runtime upgrade |
| Spring Boot | 3.3.2 | 3.3.2 | No framework upgrade required for Java 25 target; validate with compile/tests |
| Maven | 3.9.16 | 3.9+ | Compatible and already installed |
| Spring Boot Maven Plugin | 3.3.2 managed | 3.3.2 managed | Validate plugin execution on Java 25 |
| Kotlin | Not used | N/A | No Kotlin sources or Kotlin build property detected |
| Docker build JDK | Eclipse Temurin 17 | Eclipse Temurin 25 | Container build/runtime must match the target Java runtime |

## Derived Upgrades

- Update Maven's `java.version` property to `25` so the compiler and Spring Boot plugin target Java 25.
- Update both Docker stages from Eclipse Temurin 17 to Eclipse Temurin 25 so container build and runtime use the target LTS.
- Update README Java prerequisites from Java 17+ to Java 25+ to prevent local setup drift.
- Maven 3.9.16 already satisfies the recommended Maven baseline for Java 25; no build-tool upgrade is required.

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|------------|---------|--------|--------|--------|
| backend/pom.xml | `java.version` | 17 | upgrade | 25 | Sets Maven compiler/release target through Spring Boot parent |

### Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|-----------------|--------|
| None | N/A | No Java-17-specific source incompatibilities identified during targeted scan | No source rewrite planned | Java 25 is backward compatible with the current source APIs; build/test verification is required |

### Configuration Changes

| File | Property/Setting | Current | Required Change | Reason |
|------|------------------|---------|-----------------|--------|
| None | N/A | No application runtime properties reference Java 17 | No change | Java target is controlled by Maven and container configuration |

### CI/CD Changes

| File | Location | Current | Required Change |
|------|----------|---------|-----------------|
| backend/Dockerfile | build image line 1 | `maven:3.9-eclipse-temurin-17` | Change to `maven:3.9-eclipse-temurin-25` |
| backend/Dockerfile | runtime image line 8 | `eclipse-temurin:17-jre-jammy` | Change to `eclipse-temurin:25-jre-jammy` |

### Documentation Changes

| File | Location | Current | Required Change |
|------|----------|---------|-----------------|
| README.md | Java prerequisites | Java 17+ | Change to Java 25+ |
| README.md | backend stack description | Java 17 | Change to Java 25 |

### Risks & Warnings

- **Java 25 toolchain compatibility**: Spring Boot 3.3.2 and its managed plugins are older than the target JDK. **Mitigation**: run clean test compilation and the full test suite under JDK 25; only upgrade framework dependencies if an actual compatibility failure occurs.
- **Container tag availability**: The Dockerfile depends on Temurin 25 image tags. **Mitigation**: validate the Docker build when Docker is available; Maven validation remains the local acceptance check if Docker is unavailable.
- **Pre-existing working-tree changes**: The repository contains unrelated source, documentation, and generated-artifact changes. **Mitigation**: preserve them and review only the requested runtime edits before validation; do not reset or discard the worktree.
- **Dependency CVEs**: Direct dependencies include explicit versions and BOM-managed Spring dependencies. **Mitigation**: scan resolved direct dependencies after the upgrade and patch any reported vulnerabilities without removing security pins blindly.

## Upgrade Steps

- Step 1: Setup Environment
  - **Rationale**: Confirm the target JDK and Maven are available before changing project files.
  - **Changes to Make**: Use installed JDK 25.0.2 and Maven 3.9.16.
  - **Verification**: Verify JDK 25 and Maven 3.9.16 availability; expected both available.

- Step 2: Setup Baseline
  - **Rationale**: Establish the pre-upgrade compile and test status under Java 17.
  - **Changes to Make**: None.
  - **Verification**: `mvn clean compile test-compile -q && mvn clean test -q` using JDK 17; record baseline results.

- Step 3: Upgrade Java Runtime Configuration
  - **Rationale**: Apply the requested Java 25 target across Maven, Docker, and documentation.
  - **Changes to Make**: Apply all Dependency, CI/CD, and Documentation Changes above.
  - **Verification**: `mvn clean test-compile -q` using JDK 25; expected main and test compilation succeeds.

- Step 4: CVE Validation and Fix
  - **Rationale**: Confirm the resolved direct dependency set has no known vulnerabilities after the runtime upgrade.
  - **Changes to Make**: Scan direct dependencies; upgrade only vulnerable dependencies where a patched version is available; preserve justified security pins.
  - **Verification**: Re-run `mvn clean test-compile -q` and the dependency CVE scan; expected no unresolved actionable CVEs.

- Step 5: Final Validation
  - **Rationale**: Verify the requested target, clean compilation, and complete regression suite.
  - **Changes to Make**: Fix any Java 25 compatibility failures or test regressions discovered during validation.
  - **Verification**: `mvn clean test-compile -q` and `mvn clean test -q` using JDK 25; expected 100% test pass rate. Validate Dockerfile tag consistency if Docker is available.