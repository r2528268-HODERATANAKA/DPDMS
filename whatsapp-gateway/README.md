# WhatsApp gateway (self-hosted) — how to set it up

DPDMS sends WhatsApp alerts. The official routes (Meta Cloud API, Twilio) need a
**WhatsApp Business Account** (business verification / paid tier), which a school project
can't get. This folder runs an **open-source** WhatsApp gateway locally instead, so any
teammate can send real WhatsApp messages from their own number. No business, no cost.

Under the hood: **[Evolution API](https://github.com/EvolutionAPI/evolution-api)** (v2),
which emulates WhatsApp Web (Baileys) and exposes a simple REST API.

> ⚠️ This uses the unofficial WhatsApp Web protocol. It's fine for a demo with your own
> number — just don't send high volumes or you risk a temporary WhatsApp ban.

---

## 1. Run the gateway

```bash
cd whatsapp-gateway
docker compose up -d
docker compose ps          # wait until all three are "Up"
```

- API:     http://localhost:18090
- Manager: http://localhost:18090/manager   (API key: `dpdms-evolution-key`)

If the API restarts before Postgres is ready, just `docker compose restart evolution-api`.

## 2. Link your WhatsApp (once)

1. Open the **Manager** at http://localhost:18090/manager
2. Log in with the API key `dpdms-evolution-key`
3. Create/select an instance named **`dpdms`** (or create it via the API, see below)
4. Scan the QR with **WhatsApp → Settings → Linked Devices → Link a Device**

Use a **disposable number** if you can. Check the state is `open`:

```bash
curl -s http://localhost:18090/instance/connectionState/dpdms -H "apikey: dpdms-evolution-key"
# {"instance":{"instanceName":"dpdms","state":"open"}}
```

Create the instance from the CLI instead of the Manager (optional):

```bash
KEY=dpdms-evolution-key
curl -s -X POST http://localhost:18090/instance/create -H "apikey: $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"instanceName":"dpdms","qrcode":true,"integration":"WHATSAPP-BAILEYS"}'
# then GET /instance/connect/dpdms returns {"base64":"data:image/png;base64,..."} -> scan that
```

## 3. Let alert-service reach the gateway

The gateway runs in its own compose project; the DPDMS stack runs in `dpdms-team_default`.
Connect them (or use `host.docker.internal`):

```bash
docker network connect dpdms-team_default dpdms-evolution-api
```

## 4. Configure DPDMS (`school/DPDMS/.env`)

```env
ALERT_WHATSAPP_PROVIDER=local_gateway
ALERT_GATEWAY_URL=http://dpdms-evolution-api:8080/message/sendText/dpdms
ALERT_GATEWAY_API_KEY=dpdms-evolution-key
ALERT_RECIPIENTS_WHATSAPP=whatsapp:+263XXXXXXXXX     # the number you linked
```

Restart alert-service:

```bash
docker compose -p dpdms-team up -d alert-service
```

## 5. Test

```bash
# login, then send a test alert through the gateway
TOKEN=$(curl -s -X POST http://localhost:3000/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
curl -s -X POST http://localhost:3000/api/alerts/send -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"hazard":"fire","incidentId":1,"ward":"Ward 4","district":"Mudzi","severity":"HIGH","message":"Hello from DPDMS"}'
# the WHATSAPP row should be status SENT
```

Approving an incident in the UI (Approvals page) also triggers this automatically.

---

## Provider options (`ALERT_WHATSAPP_PROVIDER`)

| Value | What it uses | Needs |
|-------|--------------|-------|
| `mock` (default) | logs only | nothing |
| `local_gateway` | this Evolution API gateway | Docker + one QR scan |
| `callmebot` | CallMeBot free API | apikey from their WhatsApp opt-in |
| `meta` | Meta WhatsApp Cloud API | WhatsApp Business Account (not for school) |
| `twilio` | Twilio sandbox | Twilio + Content template / paid |

Email is separate and already works via Gmail SMTP (`ALERT_EMAIL_PROVIDER=smtp`).
