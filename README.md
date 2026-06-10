# NTI Backend

Spring Boot 3.5 · Java 21 · PostgreSQL · Flyway · JWT

---

## Quick start (Docker — recommended)

> Both repos must be cloned into the same parent directory:
> ```
> parent/
>   nti-backend/   ← this repo
>   nti-frontend/
> ```

```bash
# 1. Clone both repos
git clone <backend-repo-url> nti-backend
git clone <frontend-repo-url> nti-frontend

# 2. Create your .env from the example
cd nti-backend
cp .env .env
# Edit .env — fill in JWT_SECRET, RECAPTCHA_SECRET, and email credentials (see below)

# 3. Start everything (DB + backend + frontend)
docker compose up --build
```

| Service  | URL                   |
|----------|-----------------------|
| Frontend | http://localhost:3000 |
| Backend  | http://localhost:8080 |
| Database | localhost:5432        |

---

## Local development (without Docker)

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL 15 running locally

### Steps

```bash
# 1. Start only the database via Docker
docker compose up db -d

# 2. Copy and fill local overrides
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
# Edit application-local.properties with your SMTP and reCAPTCHA credentials

# 3. Run the application
./mvnw spring-boot:run
```

The API is available at **http://localhost:8080**.

### `application-local.properties.example`

```properties
# SMTP (Mailtrap — https://mailtrap.io)
spring.mail.username=your-mailtrap-username
spring.mail.password=your-mailtrap-password

# reCAPTCHA v2 secret
recaptcha.secret=your-recaptcha-secret

# Override JWT secret (optional — default works for local dev)
# jwt.secret=my-local-secret-min-32-chars
```

---

## Environment variables reference

| Variable          | Required | Description                                                         |
|-------------------|----------|---------------------------------------------------------------------|
| `DB_NAME`         | No       | PostgreSQL database name (default: `nti_db`)                        |
| `DB_USERNAME`     | No       | PostgreSQL user (default: `user`)                                   |
| `DB_PASSWORD`     | No       | PostgreSQL password (default: `password`)                           |
| `JWT_SECRET`      | **Yes**  | JWT signing secret — min 32 chars (`openssl rand -hex 32`)          |
| `MAIL_HOST`       | No       | SMTP host (default: `sandbox.smtp.mailtrap.io`)                     |
| `MAIL_PORT`       | No       | SMTP port (default: `587`)                                          |
| `MAIL_USERNAME`   | **Yes**  | SMTP username (Mailtrap inbox user or Gmail address)                |
| `MAIL_PASSWORD`   | **Yes**  | SMTP password (Mailtrap password or Gmail App Password)             |
| `MAIL_FROM`       | No       | Sender address shown in outgoing emails (default: `noreply@nti.sk`); must match `MAIL_USERNAME` when using Gmail |
| `MAIL_ADMIN`      | **Yes**  | Any email address that receives admin notifications (e.g. "new user registered") — independent of the sender address |
| `RECAPTCHA_SECRET`| **Yes**  | Google reCAPTCHA v2 secret key                                      |
| `APP_BACKEND_URL` | No       | Public backend URL                                                  |
| `APP_FRONTEND_URL`| No       | Public frontend URL                                                 |

### Email setup

**Development** — [Mailtrap](https://mailtrap.io): create a free inbox, copy SMTP credentials into `.env`. All emails are intercepted — nothing reaches real inboxes.

**Production** — Gmail with an App Password:
1. Enable 2-Step Verification on your Google account
2. Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) → create a password named `NTI`
3. Set in `.env`:
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your16charapppassword   # no spaces
MAIL_FROM=your-gmail@gmail.com        # must match MAIL_USERNAME
MAIL_ADMIN=any-address@example.com    # where admin alerts are delivered
```

---

## Tech stack

- **Spring Boot 3.5** — REST API
- **Spring Security + JWT** — stateless authentication
- **PostgreSQL 15** — primary database
- **Flyway** — database migrations (`src/main/resources/db/migration/`)
- **Thymeleaf** — email templates (`src/main/resources/templates/email/`)
- **Maven** — build tool

## Project structure

```
src/main/java/com/nti/nti_backend/
├── auth/           # Registration, login, JWT, password reset
├── user/           # User management, roles
├── application/    # Student applications workflow
├── program/        # Program A/B management
├── call/           # Application calls
├── email/          # Email templates + sending
├── notification/   # In-app notifications
├── gdpr/           # Data export & account deletion
├── audit/          # Audit log
├── bulk/           # Bulk messaging (admin)
├── config/         # Security, CORS, Flyway, Async
└── exception/      # Global error handling
```
