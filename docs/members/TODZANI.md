# Todzani's part — mining-accident-service + alert-service

You own the mining hazard slice AND the notifications that every other member's
approvals trigger. Your demo is the most visual one — notifications arriving live.

## mining-accident-service (port 8085, db dpdms_mining)

An exact twin of flood-service. Your fields: `mineName`, `accidentType`
(ROCKFALL/FLOODING/GAS_LEAK/EQUIPMENT_FAILURE/OTHER), `casualties`, `rescued`,
`mineOperationalStatus` (OPERATIONAL/SUSPENDED/CLOSED), `description`.

| File | One-sentence explanation |
|------|--------------------------|
| `model/MiningAccident.java` | Row in `mining_accidents`: shared metadata + GPS + your mining indicators + audit trail. |
| `model/AccidentType.java`, `model/MineStatus.java` | The two mining enums. |
| `service/MiningAccidentService.java` | Same rules as every hazard service: PENDING on create, 403 scoping before DB access, approved locked, reviews audited. |
| `service/AlertNotifier.java` | Approval → best-effort POST to alert-service. |
| `controller/MiningAccidentController.java` | 9 endpoints under `/api/minings`. |
| `MiningAccidentServiceTest.java` | 13 unit tests, one per rule. |

## alert-service (port 8087, db dpdms_alert)

| File | One-sentence explanation |
|------|--------------------------|
| `model/AlertLog.java` + enums | One row per delivery attempt: channel, recipient, SENT/FAILED/SKIPPED + reason in `detail`. |
| `channel/AlertChannel.java` | The strategy interface — the heart of your design. |
| `channel/EmailChannel.java` | SMTP via JavaMailSender; `alert.email.enabled=false` = MOCK mode (log rows still written). |
| `channel/WhatsAppChannel.java` | Meta Cloud API call, or mock; 5 s timeouts. |
| `channel/TelegramChannel.java` | Bot API sendMessage — the easiest LIVE demo (free bot token). |
| `service/AlertService.java` | Fan-out + audit; `scanApproved()` pulls the five approved feeds and alerts anything never alerted (dedupe). |
| `service/ApprovedIncidentFetcher.java` | RestClient to all five hazard services; a dead service = empty list, never an exception. |
| `controller/AlertController.java` | send / scan / list / per-hazard list / config. |
| Tests (10) | Fan-out, SKIPPED-with-reason, FAILED-doesn't-stop-others, scan dedupe, empty feeds, email mock/send/failure. |

## Your demo (5 minutes)

1. Create a Telegram bot with @BotFather, put the token + your chat id in `.env`
   (`ALERT_TELEGRAM_BOT_TOKEN`, `ALERT_RECIPIENTS_TELEGRAM`), restart alert-service.
2. Approve an incident (any member's service) → the message lands in your
   Telegram within seconds.
3. Show `GET :8888/api/alerts` — one row per channel with status and detail.
4. Show a FAILED row by stopping a channel (wrong token) — proof failures are
   logged, not hidden.

## If the teacher asks YOU

- **Why does an approval not block when Telegram is down?** The hazard-side
  notifier has a 3 s timeout + catch; your channel calls have 5 s timeouts and
  return FAILED log rows — nothing propagates exceptions upward.
- **How do you avoid alerting the same incident twice?** `scanApproved` checks
  `existsByHazardAndIncidentId` before sending; the direct notify path only
  fires once because it happens exactly on the approval event.
- **Where is the polymorphism?** `AlertChannel` — three implementations, one
  loop in `AlertService`; adding SMS = one class, zero changes elsewhere.
- **Why log SKIPPED rows?** So the demo can show WHY a channel sent nothing
  ("no recipients configured") instead of looking broken.
