# Deploying DPDMS on a VPS (step by step)

Two ways to deploy. **Option A (Docker)** is the recommended one — it installs
MySQL + Rabbit-free queueing-free infra and all 11 services with 3 commands.
**Option B (manual Java)** is the fallback if Docker is not available.

---

## Option A — Docker (recommended)

### 0. What you need
- An Ubuntu 22.04/24.04 VPS with at least **4 GB RAM** (8 GB comfortable).
  Tip: if you have a 2 GB VPS, add swap (step 2.3) and start only the services
  you demo first (`docker compose up -d mysql discovery auth-service api-gateway`).
- A user with sudo rights.
- Ports open in the VPS firewall/security group: 22, 8761, 8888
  (open 8080-8088 only if you want direct service access for debugging).

### 1. Install Docker (once)

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker $USER   # log out and back in after this
```

### 2. Get the code onto the VPS

```bash
# 2.1 push the group repo to GitHub first (from your laptop):
git remote add origin https://github.com/r2528268-HODERATANAKA/DPDMS.git
git push -u origin main --follow-tags

# 2.2 on the VPS:
git clone https://github.com/r2528268-HODERATANAKA/DPDMS.git
cd DPDMS

# 2.3 (only for 2 GB RAM servers) add 4 GB swap:
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 3. Configure and launch

```bash
cp .env.example .env
nano .env        # at minimum set DPDMS_JWT_SECRET to your own 32+ char value
docker compose up -d --build
docker compose ps           # wait until every service is "Up"
docker compose logs -f api-gateway   # Ctrl+C to stop following
```

First build takes ~10-15 min (Maven downloads inside the build). Subsequent
rebuilds are fast.

### 4. Verify the deployment (30 seconds)

```bash
curl http://localhost:8761                    # Eureka dashboard (all services listed)
curl http://localhost:8888/api/floods         # approved feed (empty list is OK)
curl http://localhost:8888/api/dashboard/health
# -> {"flood":"UP","drought":"UP","fire":"UP","zoonotic":"UP","mining":"UP"}
```

### 5. Lock it down (do this before showing anyone)

```bash
# Only the gateway and Eureka need to be public:
sudo ufw allow OpenSSH && sudo ufw allow 8888 && sudo ufw allow 8761 && sudo ufw enable
```

Direct service ports (8080-8088) then stay private — all traffic goes through
the gateway, which is exactly the design.

### 6. Update to a new version

```bash
git pull
docker compose up -d --build
```

---

## Option B — manual Java (no Docker)

```bash
sudo apt-get install -y openjdk-17-jdk mysql-server maven
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'Gr@nd\$0n'; FLUSH PRIVILEGES;"
# (databases are auto-created by the services via createDatabaseIfNotExist)

# start each service in its own tmux/screen session, or use systemd:
cd DPDMS
mvn -f discovery-server/pom.xml spring-boot:run    # first (port 8761)
mvn -f auth-service/pom.xml spring-boot:run        # 8080
mvn -f flood-service/pom.xml spring-boot:run       # 8081
mvn -f drought-service/pom.xml spring-boot:run     # 8082
mvn -f fire-service/pom.xml spring-boot:run        # 8083
mvn -f zoonotic-disease-service/pom.xml spring-boot:run   # 8084
mvn -f mining-accident-service/pom.xml spring-boot:run    # 8085
mvn -f report-service/pom.xml spring-boot:run      # 8086
mvn -f alert-service/pom.xml spring-boot:run       # 8087
mvn -f dashboard-service/pom.xml spring-boot:run   # 8088
mvn -f api-gateway/pom.xml spring-boot:run         # last (8888)
```

Build runnable jars instead (better for systemd):
```bash
mvn -f auth-service/pom.xml package -DskipTests
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
```

---

## Turning the notifications ON (Todzani's alert-service)

By default everything runs in **mock mode**: alert attempts are logged in
`dpdms_alert.alert_logs` with status SKIPPED and a reason — perfect for the
demo, nothing leaks. To go live, edit `.env` on the VPS and `docker compose up -d`:

### Email (Gmail, free)
1. Google Account -> Security -> enable 2-step verification.
2. Create an **App Password** (16 characters).
3. In `.env`: `ALERT_EMAIL_ENABLED=true`, `SPRING_MAIL_USERNAME=<your gmail>`,
   `SPRING_MAIL_PASSWORD=<16-char app password>`,
   `ALERT_RECIPIENTS_EMAIL=dc@province.gov.zw,provincial.admin@gov.zw`.

### WhatsApp (Meta Cloud API, free test tier)
1. developers.facebook.com -> My Apps -> Create App (type: Business) -> add WhatsApp.
2. Copy the **Phone number ID** and the temporary **access token**.
3. In `.env`: `ALERT_WHATSAPP_PROVIDER=meta`,
   `ALERT_WHATSAPP_PHONE_NUMBER_ID=<id>`, `ALERT_WHATSAPP_TOKEN=<token>`,
   `ALERT_RECIPIENTS_WHATSAPP=263771234567` (numbers in international format).

### Telegram (easiest live demo, 5 minutes)
1. In Telegram, message **@BotFather** -> `/newbot` -> follow prompts -> copy the bot token.
2. Message **@userinfobot** to get your chat id.
3. In `.env`: `ALERT_TELEGRAM_BOT_TOKEN=<token>`, `ALERT_RECIPIENTS_TELEGRAM=<chatid>`.

Then test from the VPS:
```bash
curl -X POST http://localhost:8888/api/alerts/send \
  -H "Content-Type: application/json" \
  -d '{"hazard":"flood","ward":"Mudzi","district":"Mudzi","severity":"HIGH","message":"Test alert from DPDMS"}'
```
Check the audit trail: `curl http://localhost:8888/api/alerts | head -50`

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Service keeps restarting in `docker compose ps` | MySQL not healthy yet | `docker compose logs mysql`; wait, it retries |
| Gateway returns 503 | target service not registered yet | wait ~30 s, check http://host:8761 |
| Gateway returns 401 everywhere | token missing/expired, or secrets differ | `.env` DPDMS_JWT_SECRET must be identical for auth-service & api-gateway |
| Eureka shows instances going UP/DOWN | VPS clock drift / thin swap | enable NTP (`timedatectl`), add swap |
| `docker compose: command not found` | plugin missing | `sudo apt-get install docker-compose-plugin` |
| Port already in use | something else on 3306/8888 | `sudo lsof -i :8888` then stop it, or change the left side of the port mapping |
