# 3-day mastery plan (how to learn THIS project before the demo)

> Goal: every member can run the system, explain their own part file-by-file,
> demo the main flows, and answer the teacher's questions. Split the days if
> you must, but **day 1 is the most important one** — the main flows are what
> get tested first.

## Before anything: get it running (30 min)

```bash
cp .env.example .env
docker compose up -d --build      # first build ~10-15 min
docker compose ps
```

No Docker on your laptop? Install MySQL locally, then run the services in this
order and wait ~5 s between each:
discovery → auth → flood → drought → fire → zoonotic → mining → report → alert → dashboard → gateway.

---

## Day 1 — the main flows (what gets tested first)

Everyone learns these four flows TODAY, whatever their part is. They cover the
marking guide's core: capture → review → notify → report.

1. **Login** (Sean's auth-service):
   `POST /api/auth/login` with an admin account → copy the token from the response.
2. **Capture** (your own hazard service):
   `POST /api/fires` (or your hazard) with `X-User-Ward`/`X-User-Hazard` headers →
   see status come back as `PENDING`.
3. **Review** (the workflow):
   `PATCH /api/fires/1/approve` as the supervisor → status APPROVED, reviewedBy set,
   and an alert row appears in `GET /api/alerts`. Also try the failure case:
   approve with a different hazard → **403**.
4. **Consume** (Rejoice + Oliver):
   `GET /api/reports/fires?format=pdf` downloads a real PDF;
   `GET /api/dashboard/summary` shows the counts.

Read while doing it: `docs/03-API-REFERENCE.md` (copy-paste curl for every step)
and `docs/01-ARCHITECTURE.md` (the flow diagram at the top).

**Exit test for day 1:** you can run the four flows from memory and explain
what the gateway did with your token on the way.

## Day 2 — your own part, file by file

Open `docs/members/<you>.md`. For every file listed there:
1. Read the file (they are short and commented for exactly this).
2. Say out loud what it does — if you can't, re-read.
3. Change something small and watch the tests catch it (or not):
   e.g. remove `enforceWardScope` from create → which test fails? (It's
   `createRejectsRecorderFromAnotherWard` — that's how you learn the safety net.)
4. Run YOUR service's tests: `mvn -f <service>/pom.xml test`.

Also read your service's README (every service has one, with a "files" table
and an "if the teacher asks" section).

**Exit test for day 2:** you can explain every file of your service in one
sentence each, and you ran its tests green.

## Day 3 — the whole system + the awkward questions

1. Read the other members' one-page guides (`docs/members/`) — enough to know
   what their part does and where it plugs into yours.
2. Trace one request across 4 services (the approve flow in
   `docs/diagrams/sequence-approve-alert.png` is the map).
3. Study the diagram set in `docs/diagrams/`: class, object, statechart, ER,
   component, deployment, use-case + the two sequence diagrams. The statechart
   and ER are the two teachers ask about most.
4. Read `docs/02-DATABASE.md` — know your table's columns and why there are no
   cross-service foreign keys.
5. Practise the awkward questions below with a teammate playing the teacher.

**Exit test for day 3:** each member gives a 5-minute tour of their part
running system included — no reading from notes.

---

## The awkward questions (and the honest answers)

**"Why do the five hazard services look so similar?"**
Because the leader set the pattern with flood-service and every member built a
twin of it — same layers, same validation, same workflow. That was a deliberate
team decision: consistent, reviewable, and no shared library that couples us.

**"Where is the OOP?"**
- **Encapsulation**: entities keep validation annotations with their data;
  the state machine is enforced inside services, not left to callers.
- **Inheritance / consistent structure**: same layering everywhere; Spring Data
  repository interfaces extend `JpaRepository`.
- **Polymorphism**: `ReportGenerator` (CSV/PDF/XLSX/DOCX), `AlertChannel`
  (EMAIL/WHATSAPP/TELEGRAM) — add one class, no if-statements.
- **Abstraction**: controllers never touch repositories; every service exposes
  intent-named methods (`approve`, `requestCorrections`).

**"How do you know only Mudzi's recorder can write Mudzi's records?"**
Two 403 guards in every service method (`enforceHazardScope`, `enforceWardScope`)
plus unit tests proving the guard fires BEFORE any database lookup — and the
gateway injects those headers only from a verified JWT.

**"What happens if alert-service is down when I approve?"**
Nothing breaks: the notifier has a 3-second timeout and a catch-all; approval
succeeds; alert-service catches up later via `/api/alerts/scan`.

**"Why does report-service have no database?"**
It reports live data — fetching at report time means a report can never be
stale, and there is nothing to keep in sync.

**"Show me the tests."**
`mvn -f <service>/pom.xml test` — ~100 tests across the system covering the
workflow rules, the scoping rules, JWT issue/verify, the alert fan-out, the
report files, and the dashboard aggregation. They run on in-memory H2 — no
MySQL needed.

## If you only have ONE evening

1. Run the docker compose section above.
2. Do Day 1's four flows with `docs/03-API-REFERENCE.md` open.
3. Read your member guide's "if the teacher asks" section.
