# DPDMS architecture

## The one rule that shaped everything

Tanaka started the repo with **flood-service as a standalone Spring Boot app**
(own pom, own port, own database). Every other service is an exact twin of that
pattern. There is NO parent pom and NO shared library — repetition is deliberate:
any member can build, run, test and explain their service alone, and no change
in one service can break another.

```
            HTTPS + JWT
Browser ───────────────► api-gateway :8888   (Tanaka)
                            │  1. verifies the JWT (same secret as auth-service)
                            │  2. strips client-sent X-User-* headers (anti-faking)
                            │  3. injects X-User-Name / Role / Ward / Hazard from claims
                            │  4. asks discovery-server which live instance owns the path
                            ▼
        ┌───────────────────────────────────────────────────┐
        │ lb://<service-name> via Eureka (discovery :8761)  │
        └───────────────────────────────────────────────────┘
   ┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
   ▼          ▼          ▼          ▼          ▼          ▼          ▼
 auth      flood      drought     fire     zoonotic    mining    report/alert/dashboard
 :8080     :8081      :8082      :8083      :8084      :8085      :8086/:8087/:8088
  Sean     Tanaka     Oliver      Sean     Rejoice    Todzani    Rejoice/Todzani/Oliver
   │          │          │          │          │          │
   └──────────┴──── MySQL (7 schemas) ┴──────────┴──────────┘
```

## Identity: how the caller is known everywhere (the interim-auth seam)

The team's marking guide says services start with **headers**, and real JWTs
arrive in integration week. The leader's flood-service NOTE comment planned
exactly this swap, so we kept BOTH halves compatible:

- **Now (direct calls / Postman):** you add `X-User-Ward`, `X-User-Hazard`,
  `X-User-Name`, `X-User-Role` headers yourself.
- **Through the gateway (production shape):** you send `Authorization: Bearer <jwt>`
  from auth-service; the gateway verifies it and injects the very same headers.
- The hazard services did not need to change — their controllers read headers,
  and their service methods take `callerWard/callerHazard` as plain parameters.

Token claims (issued by auth-service, HS256, issuer `dpdms-auth-service`):

| Claim | Meaning |
|-------|---------|
| `sub` | username |
| `role` | WARD_RECORDER / PROVINCIAL_SUPERVISOR / PROVINCIAL_ADMIN |
| `name` | full name → becomes `reviewedBy` on approvals |
| `ward` | recorder's ward (absent for supervisors/admins) |
| `hazard` | recorder's or supervisor's hazard (absent for admins) |
| `iss`, `iat`, `exp` | issuer + 8-hour validity |

## The approval workflow (identical in all 5 hazard services)

```
POST (recorder) ──► PENDING ──approve──► APPROVED (locked)
                      │  ▲
                 reject│  │ PUT (recorder edit)
             request- │  │ resubmits as PENDING
             corrections ▼
                 REJECTED / CORRECTIONS_REQUESTED
```

- Creating always forces `PENDING` (no client can smuggle an APPROVED record in).
- Only the matching supervisor can approve/reject/return; a wrong hazard gets 403
  **before any database lookup**.
- Every review stamps `reviewedBy` / `reviewedAt` / notes — the audit trail.

## Scoping rules (FR-SCOPE-01)

| Role | May do |
|------|--------|
| WARD_RECORDER | create/edit/delete records of their OWN ward for their ONE hazard |
| PROVINCIAL_SUPERVISOR | approve/reject/request-corrections any ward of their ONE hazard |
| PROVINCIAL_ADMIN | manage accounts; needs a concrete ward+hazard for recorders at creation time |
| (violations) | ward mismatch → 403 · hazard mismatch → 403 · wildcards ("ALL", "*") → 400 at account creation |

## Notifications chain

1. A supervisor approves an incident.
2. The hazard service saves it, then **best-effort** POSTs to
   `alert-service /api/alerts/send` (3 s timeout, try/catch — a dead alert-service
   can never block an approval). This fulfils the leader's `TODO (integration
   week)` comment inside flood-service's approve method.
3. alert-service fans the message out to EMAIL (SMTP or mock), WHATSAPP
   (Meta Cloud API or mock) and TELEGRAM (Bot API), writing one `alert_logs`
   row per channel attempt (SENT / FAILED / SKIPPED + reason).
4. `POST /api/alerts/scan` pulls approved incidents from all five feeds and
   alerts any that were never alerted (dedupe by hazard+incidentId) — useful
   when services run before alert-service, or for a scheduled cron.

Why HTTP and not RabbitMQ? Simpler for a team that started Java this month:
one less broker to install/deploy/defend, and the leader's own TODO in
flood-service already specified the HTTP call.

## Reports and dashboard (no-database services)

- **report-service** fetches the approved feed of one hazard (forwarding the
  caller's X-User-* headers so ward scoping still applies) and renders it with
  one of four `ReportGenerator` strategies: CSV (pure Java), PDF (OpenPDF),
  XLSX/DOCX (Apache POI). Reporting live data means reports can never be stale.
- **dashboard-service** fetches all five approved feeds and returns counts per
  hazard, counts per severity, the 5 latest approved incidents, plus a health
  view (UP/DOWN per service). A dead hazard service degrades to zero instead of
  breaking the whole dashboard.

## Service discovery (why Eureka)

The gateway routes with `lb://flood-service` URIs. Eureka tells it where
`flood-service` currently lives. Add a second instance of any service and the
gateway load-balances automatically; kill one and it drops out of the registry.

## Port map

| 8761 | 8080 | 8081 | 8082 | 8083 | 8084 | 8085 | 8086 | 8087 | 8088 | 8888 |
|------|------|------|------|------|------|------|------|------|------|------|
| discovery | auth | flood | drought | fire | zoonotic | mining | report | alert | dashboard | gateway |

## Design decisions worth defending in the demo

| Decision | Why |
|----------|-----|
| Standalone services, no shared parent pom | every member owns one service end-to-end; leader's pattern |
| Same structure in all 5 hazard services | one review, one mental model, reviewer's job is easy |
| Headers as the identity seam now | the guide's interim-auth plan; JWT drop-in needs no service changes |
| One MySQL schema per service | independent deploys, no cross-service joins to defend |
| report/dashboard keep no DB | live data always; nothing to sync |
| HTTP notify instead of RabbitMQ | beginner-simple, matches the leader's TODO |
| Mock-first notifications (EMAIL/WHATSAPP/TELEGRAM) | the demo works anywhere; flipping to real is config-only |
