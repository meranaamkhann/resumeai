# Resume Intelligence Platform

An AI-powered resume intelligence platform covering all 6 phases of the original spec: resume analysis, job-description matching, AI-assisted bullet rewriting, versioning/export, an applications tracker with usage quotas and billing, and analytics. Every score, recommendation, and match is generated from the actual uploaded resume — nothing is hardcoded or fabricated.

## Architecture

```
frontend/   React + TypeScript (Vite)
backend/    Spring Boot 3 (Java 17), PostgreSQL, Flyway migrations
```

## What's real vs. what's an honest stub

Almost everything below is fully implemented and runnable today. Three things are intentionally NOT — because they need either credentials this build can't have, a system binary that can't be installed here, or a decision the spec itself said to defer:

| Feature | Status |
|---|---|
| Everything in Phases 1–4, and the applications tracker / usage quotas in Phase 5 | **Fully implemented, real deterministic logic, runs out of the box** |
| AI bullet rewriting (Gemini) | **Real implementation** — activates the moment you set `AI_PROVIDER=gemini` + `AI_API_KEY`. Ships disabled (`AI_PROVIDER=none`) with a clear error if called |
| Payments (Stripe) | **Real webhook signature verification + idempotent processing**, activates with `PAYMENT_PROVIDER=stripe` + a real webhook secret. Ships disabled — nobody should trust a payment integration nobody has tested end-to-end with real Stripe keys |
| OCR for image-only PDFs | **Honest no-op.** Real OCR needs a Tesseract binary (or a hosted OCR API), which isn't something to fake. The `OcrService` interface and the call site are fully wired — implementing it is a self-contained task that doesn't touch anything else |
| JD import via URL | **Deliberately not implemented.** The spec's own instruction (section 56): don't implement URL fetching unless SSRF protection is done properly. Paste/upload JD text works today |

If you plug in real AI/Stripe credentials, nothing else needs to change — that's the point of the provider-abstraction pattern used throughout (`AiAnalysisService`, `PaymentProvider`, `OcrService`).

## Phase-by-phase feature map

**Phase 1 — Core**: auth (Argon2id + JWT with refresh-token rotation), secure upload (magic-byte validation, zip-bomb guard, parse timeout), deterministic ATS scoring engine with explainable category breakdowns, dashboard.

**Phase 2 — Job matching**: paste a JD, get required/preferred skill extraction (deterministic, dictionary + heading-based), a job match score, missing-skill list, and a rough experience-years match — all explained, never keyword-stuffing advice.

**Phase 3 — AI-assisted features**: bullet rewriting in 5 modes (ATS-optimized, recruiter-friendly, concise, technical, achievement-focused) behind a real `AiAnalysisService` interface, gated by a **factual-consistency guard** that rejects any rewrite introducing a number, metric, or technology not present in the original — this is enforced in code, not just prompted for. Recruiter-view simulation is a deterministic heuristic (explicitly labeled as such, per spec) so it works even with no AI provider configured.

**Phase 4 — Versioning & export**: label a resume+document combo as a version targeting a role/company/JD, compare before/after scores category-by-category, export to an ATS-safe plain-template PDF (PDFBox) or DOCX (POI).

**Phase 5 — Applications & billing**: full CRUD applications tracker (company/role/status/notes), monthly usage quotas enforced with a **pessimistic DB row lock** (this is the piece that actually prevents the classic "two simultaneous requests both consume the last credit" race the spec calls out), and a Stripe-shaped payment abstraction with real HMAC webhook verification and idempotent event processing.

**Phase 6 — OCR & analytics**: OCR interface wired into the parsing pipeline (currently a documented no-op — see table above), lightweight non-sensitive analytics events (upload/analyze/match counts, no resume content ever stored), and a deliberate non-implementation of URL-based JD import for SSRF-safety reasons.

## Setup

### Prerequisites
- Java 17+, Maven (or use the Docker build)
- Node 20+
- PostgreSQL 16 (or use `docker-compose up db`)

### Backend
```
cd backend
cp .env.example .env   # fill in DB_PASSWORD and JWT_SECRET at minimum
export $(cat .env | xargs)
mvn spring-boot:run
```
Flyway runs all 9 migrations automatically. The app **fails to start** if `JWT_SECRET` is unset or too short — intentional.

To enable AI bullet rewriting: set `AI_PROVIDER=gemini` and `AI_API_KEY=<your Gemini API key>`.
To enable billing: set `PAYMENT_PROVIDER=stripe` and `PAYMENT_WEBHOOK_SECRET=<your Stripe webhook signing secret>`, then point a Stripe webhook at `POST /api/billing/webhook`.

### Frontend
```
cd frontend
npm install
npm run dev
```

### Docker
```
JWT_SECRET=$(openssl rand -base64 64) docker compose up --build
```

## Security posture

- Passwords hashed with Argon2id, never logged, never returned in API responses.
- JWT bearer auth with real refresh-token rotation (15-min access / 7-day rotating refresh, hashed at rest, revocable via `/auth/logout`).
- Explicit CORS allowlist, no wildcard. Client IP for rate limiting only trusts `X-Forwarded-For` when `TRUST_PROXY_HEADERS=true` is explicitly set — otherwise it's spoofable.
- Every query is scoped by the authenticated user's ID — never a client-supplied ID. Non-owned or soft-deleted resources return an identical 404.
- Upload pipeline: extension + magic-byte validation (Tika), a real zip-bomb guard for DOCX (entry count, size, compression-ratio checks), and a hard 15-second parse timeout on a bounded thread pool.
- Long resumes are handled transparently (page/length caps surfaced as explicit issues, never silently degrading the score).
- Global exception handler: no stack traces or internal errors ever reach the client.
- Per-user rate limiting on a bounded, expiring cache (not an unbounded map).
- A shared per-document lock prevents duplicate concurrent analysis AND prevents deleting a resume mid-analysis.
- Usage quotas are enforced with a `SELECT ... FOR UPDATE` row lock — two simultaneous requests cannot both consume the last free-tier credit.
- Stripe webhooks: real HMAC-SHA256 signature verification, idempotent processing via a unique `(provider, event_id)` constraint, and the webhook is the only source of truth for plan changes — the frontend can never set its own plan.
- The AI rewrite pipeline treats resume/bullet text as **data, never instructions** — the prompt structure explicitly separates the two, and no user-supplied content can alter the instruction.

### Known scaling limits

The per-document lock registry and the rate-limiter's bucket cache are in-process, in-memory state — correct for a single backend instance, but they won't coordinate across multiple instances behind a load balancer. Scaling out needs a distributed lock (Postgres advisory lock or Redis) and a shared rate-limit store before it's safe.

## What's genuinely not done

- Email verification / password reset flow — registration issues tokens immediately.
- No automated test suite yet — the scoring engine, file-security validator, zip-bomb guard, and factual-consistency guard are the highest-value targets.
- OCR, real payment credentials, and URL-based JD import — see the table above.
- Resume optimizer (spec section 23 — full auto-optimization against a JD) is not built as a single-click feature; the underlying pieces (bullet rewrite, JD matching, factual consistency guard) all exist and could compose into it.

## Environment variables

See `backend/.env.example` for the full list.
