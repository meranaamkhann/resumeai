# Resume Intelligence Platform

An AI-powered resume intelligence platform covering all 6 phases of the original spec, hardened against the real gaps found during actual local setup and a code review pass.

## What changed in this hardening pass

Going in, 10 gaps were identified. Here's the honest status of each:

| # | Gap | Status |
|---|---|---|
| 1 | No automated tests | **Fixed** — real JUnit 5 tests for the file-security validator (including a zip-bomb rejection test), the factual-consistency guard, the scoring engine, and the job-match engine. Run with `mvn test`. |
| 2 | Never confirmed a full end-to-end run | **Fixed through this exact conversation** — register → upload → analyze was walked through live and every real bug hit along the way (missing Bouncy Castle dependency, CORS preflight rejection, port collisions) is now fixed in the code itself, not just worked around locally. |
| 3 | AI features disabled by default | **Not something to "fix" in code** — `AiAnalysisService`/Gemini implementation is real and already wired (see Phase 3 notes below). It only activates once you provide a real `AI_API_KEY`. Nobody should fake this. |
| 4 | Payments never connected | **Same as above** — the Stripe webhook verification and idempotent processing are real; activating it needs a real Stripe account and webhook secret, which only you can provide. |
| 5 | OCR was a documented no-op | **Real implementation added** (`TesseractOcrService`, using tess4j) — activates with `OCR_PROVIDER=tesseract` once Tesseract is installed on the host and `OCR_TESSDATA_PATH` is set. Cannot be tested from this environment since installing a system binary isn't possible here — you'll need to verify it yourself with `docker exec` or a local install. |
| 6 | Locks/rate-limiting were single-instance only | **Locks fixed for real** — analysis and deletion now use Postgres transaction-scoped advisory locks (`pg_try_advisory_xact_lock`) instead of an in-JVM `ReentrantLock` map. This works correctly across multiple backend instances with zero new infrastructure. Rate limiting is still per-instance in-memory — that's a defensible trade-off (soft protection, not correctness-critical) but would need Redis to be truly distributed; noted honestly rather than faked. |
| 7 | No CI/CD | **Fixed** — `.github/workflows/ci.yml` runs the backend test suite and the frontend build on every push/PR. |
| 8 | Docker build never verified end-to-end | **Config bugs fixed, but still not executable from this environment** (no Docker daemon available here). Fixed: `docker-compose.yml` now has a real Postgres healthcheck with `depends_on: condition: service_healthy` (previously Flyway could race Postgres startup), and all ports are now configurable via `.env` with collision-resistant defaults — directly informed by the exact port conflicts hit during manual setup in this conversation (5432, 8080, and 5173 were all already taken by other local projects). You should still run `docker compose up --build` yourself once to confirm it actually builds clean end to end. |
| 9 | No email verification / password reset | **Fully implemented** — real token-based flows (hashed opaque tokens, 24h/1h expiry, single-use), a pluggable `EmailSender` (console-log by default, real SMTP via `EMAIL_PROVIDER=smtp`), and matching frontend pages (`/verify-email`, `/forgot-password`, `/reset-password`). |
| 10 | Frontend polish / accessibility | **Partially addressed** — added a skip-to-content link, `role="alert"`/`role="status"` on error and success messages so screen readers announce them, and completed the previously-missing password-reset UI. A full WCAG audit was not performed — that's a bigger, more manual effort than a code pass can respond to honestly. |

## Architecture

```
frontend/   React + TypeScript (Vite)
backend/    Spring Boot 3 (Java 17), PostgreSQL, Flyway migrations
```

## Setup

### Prerequisites
- Java 17+, Maven
- Node 20+
- PostgreSQL 16 (or `docker compose up db`)

### Backend
```
cd backend
cp .env.example .env   # fill in JWT_SECRET at minimum: openssl rand -base64 64
export $(cat .env | xargs)   # PowerShell users: set each $env:VAR manually, see below
mvn clean spring-boot:run
```

**Windows PowerShell users:** `.env` files aren't auto-loaded. Set each variable explicitly in the same terminal session before running Maven:
```powershell
$env:JWT_SECRET="..."
$env:DB_URL="jdbc:postgresql://localhost:5434/resumeai"
$env:DB_USERNAME="resumeai"
$env:DB_PASSWORD="changeme"
mvn clean spring-boot:run
```
If port 5432/8080/5173 are already taken by another local project (common if you have multiple Spring Boot projects checked out), pick different host ports — `docker-compose.yml` now defaults to 5434/8081/5175 specifically to avoid this.

### Frontend
```
cd frontend
npm install
npm run dev
```
Update `frontend/vite.config.ts`'s proxy target if your backend isn't on the default port.

### Running tests
```
cd backend
mvn test
```

### Docker
```
cp .env.example .env   # project root — fill in JWT_SECRET
docker compose up --build
```

## Security posture

- Argon2id password hashing (requires the Bouncy Castle provider — now declared explicitly in `pom.xml`, since Spring Security doesn't bundle it).
- JWT access tokens (15 min) + rotating refresh tokens (7 days, hashed at rest, revocable).
- CORS: explicit allowlist, and `OPTIONS` preflight requests are explicitly permitted (a JSON `POST` from a browser always sends a preflight first — without this, every browser-based call would 403 before ever reaching a controller).
- Every resource query is scoped to the authenticated user; non-owned or soft-deleted resources return an identical 404.
- Upload pipeline: magic-byte validation (Tika), a real zip-bomb guard for DOCX (verified by an actual test constructing a malicious archive), and a hard parse timeout.
- **Analysis and deletion concurrency is now correct across multiple backend instances** via Postgres advisory locks, not just a single process's memory.
- Usage quotas enforced with a `SELECT ... FOR UPDATE` row lock — already safe across instances.
- Password reset and email verification tokens are opaque, hashed at rest (never the raw token), single-use, and time-limited. Password reset intentionally never reveals whether an email exists in the system.
- Stripe webhooks: real HMAC-SHA256 verification, idempotent via a unique `(provider, event_id)` constraint.

## Known, honestly-stated limitations

- Rate limiting is per-instance in-memory (bounded/expiring, so no memory-exhaustion risk, but not shared across horizontally-scaled instances). Fixing this properly needs Redis.
- OCR, AI rewriting, and payments are real implementations gated behind environment variables that default to "off." Turning them on requires your own Tesseract install, Gemini API key, and Stripe account respectively — none of which can be provisioned or tested from this environment.
- No full WCAG accessibility audit has been performed.
- URL-based JD import is still deliberately not implemented (SSRF risk, per the original spec's own instruction).

## Environment variables

See `backend/.env.example` (app config) and `.env.example` at the project root (Docker Compose config) for the full list.
