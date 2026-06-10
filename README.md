# NTI — Getting Started

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)
- Git

---

## 1. Clone both repositories side by side

```bash
git clone https://github.com/dante8s/nti-backend.git
git clone https://github.com/dante8s/nti-frontend.git
```

The folder structure must be:

```
project-nti/
├── nti-backend/
└── nti-frontend/
```

---

## 2. Configure backend environment

```bash
cd nti-backend
cp .env.example .env
```

Open `.env` and fill in the required values:

| Variable | What to set |
|---|---|
| `JWT_SECRET` | Any random string, min 32 chars. Generate: `openssl rand -hex 32` |
| `MAIL_USERNAME` | Your Gmail address |
| `MAIL_PASSWORD` | Gmail [App Password](https://myaccount.google.com/apppasswords) (16 chars) |
| `MAIL_FROM` | Same Gmail address |
| `MAIL_ADMIN` | Email that receives admin notifications |
| `RECAPTCHA_SECRET` | Secret key from [Google reCAPTCHA admin](https://www.google.com/recaptcha/admin) |

Database and Redis values can stay as defaults for local development.

---

## 3. Configure frontend environment

```bash
cd ../nti-frontend
cp .env.example .env
```

Open `.env` and set:

| Variable | What to set |
|---|---|
| `VITE_RECAPTCHA_SITE_KEY` | **Site key** from the same reCAPTCHA registration (not the secret key) |

> **Note:** reCAPTCHA site key and secret key must come from the same registration at [google.com/recaptcha/admin](https://www.google.com/recaptcha/admin). Register `localhost` as the domain for local development.

---

## 4. Start everything

From the `nti-backend` folder:

```bash
docker compose up --build
```

First run takes a few minutes (downloads images, compiles Java, builds Vue).

---

## 5. Access the app

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |

---

## Stopping

```bash
docker compose down
```

To also delete the database volume:

```bash
docker compose down -v
```
