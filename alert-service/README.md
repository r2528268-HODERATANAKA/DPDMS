# alert-service

Sends DPDMS notifications through **EMAIL**, **WHATSAPP** and **TELEGRAM**,
and keeps an audit log of every attempt.

**Owner:** Todzani   |   **Port:** 8087   |   **Database:** `dpdms_alert` (auto-created)

## Files in this service (what each one does)

| File | What it does |
|------|--------------|
| `pom.xml` | Boot 4.1.1 starters + `spring-boot-starter-mail` (SMTP) + MySQL + H2 for tests. |
| `AlertServiceApplication.java` | Entry point. |
| `model/AlertLog.java` | One row per delivery attempt: who was alerted, on which channel, SENT/FAILED/SKIPPED + why. |
| `model/AlertChannelType.java` | Enum: EMAIL, WHATSAPP, TELEGRAM. |
| `model/AlertStatus.java` | Enum: SENT, FAILED, SKIPPED (with the reason in `detail`). |
| `repository/AlertLogRepository.java` | Newest-first queries + `existsByHazardAndIncidentId` for scan dedupe. |
| `channel/AlertChannel.java` | **Strategy interface** — the service loops over all implementations. This is the polymorphism of my part. |
| `channel/EmailChannel.java` | SMTP via `JavaMailSender`; `alert.email.enabled=false` (default) = MOCK mode: same log rows, message printed to console, nothing sent. |
| `channel/WhatsAppChannel.java` | `provider=mock` (default) or Meta Cloud API call (`POST .../messages` with Bearer token). |
| `channel/TelegramChannel.java` | Bot API `POST /bot<token>/sendMessage`; SKIPPED until you paste a bot token. |
| `channel/AlertRequest.java` | The payload that travels through channels (hazard, incident, ward, severity, message). |
| `service/AlertService.java` | Fan-out: builds the recipient list per channel, calls each channel, saves every log row. `scanApproved()` pulls approved incidents from all five hazard feeds and alerts any that were never alerted. |
| `service/ApprovedIncidentFetcher.java` | RestClient calls to the five hazard services' approved feeds; a dead service degrades to an empty list. |
| `controller/AlertController.java` | `POST /api/alerts/send`, `POST /api/alerts/scan`, `GET /api/alerts`, `GET /api/alerts/hazard/{h}`, `GET /api/alerts/config`. |
| `AlertServiceTest.java` (6 tests) | fan-out saves one row per recipient, missing recipients are logged as SKIPPED (never silent), a FAILED channel never stops the others, scan alerts new incidents once, scan skips already-alerted ones, empty feeds do nothing. |
| `EmailChannelTest.java` (3 tests) | mock mode → SKIPPED + no send, real send → SENT, SMTP error → FAILED row (never an exception out). |

## Who calls it?

1. **The five hazard services** — on every APPROVE they best-effort POST
   `/api/alerts/send` (3 s timeout, try/catch — never blocks an approval).
2. **You** — `POST /api/alerts/scan` pulls approved incidents from the five
   feeds and alerts anything new (dedupe via `alert_logs`). Run it manually or
   from cron.
3. **The demo** — `POST /api/alerts/send` with any body to see the fan-out live.

## Turning channels on (all default to mock/safe)

| Channel | Property keys | How to get credentials |
|---------|---------------|------------------------|
| EMAIL | `alert.email.enabled`, `spring.mail.username`, `spring.mail.password`, `alert.recipients.email` | Gmail App Password (DEPLOYMENT.md §notifications) |
| WHATSAPP | `alert.whatsapp.provider=meta`, `alert.whatsapp.phone-number-id`, `alert.whatsapp.token`, `alert.recipients.whatsapp` | Meta Cloud API test app (free) |
| TELEGRAM | `alert.telegram.bot-token`, `alert.recipients.telegram` | @BotFather `/newbot` + @userinfobot (5 minutes, easiest live demo) |

## If the teacher asks

- **Why SKIPPED and not just silent?** The log must show WHY nothing went out
  on a channel ("no recipients configured", "MOCK mode") — otherwise the demo
  looks broken when it is only unconfigured.
- **What if one channel is down?** Each call is wrapped: the result is a FAILED
  row with the provider error, and the remaining channels still send. One test
  proves exactly that.
- **Design pattern?** Strategy — `AlertChannel` interface; adding SMS later =
  one new class, zero changes to `AlertService`.
