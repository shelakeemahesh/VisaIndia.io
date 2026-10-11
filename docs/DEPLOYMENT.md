# Production Deployment Guide: Vercel & Render

This guide walks you through deploying **VisaIndia.io / VisaSetu** into production using **Render** (Backend Spring Boot 3 + PostgreSQL 16) and **Vercel** (Frontend React 18 SPA).

---

## Architecture Overview

```
                      ┌────────────────────────────────────────┐
                      │          Traveler Browser              │
                      └──────────────────┬─────────────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 │ HTTPS                                         │ HTTPS / CORS
                 ▼                                               ▼
   ┌───────────────────────────┐                   ┌───────────────────────────┐
   │       Vercel CDN          │                   │        Render Edge        │
   │  (Frontend React SPA)     │                   │     (Spring Boot API)     │
   │                           │                   │                           │
   │ • vercel.json SPA routing │                   │ • Docker Container ($PORT)│
   │ • Security headers        │                   │ • Auto-Flyway Migrations  │
   │ • VITE_API_URL            │                   │ • Razorpay Verification   │
   └───────────────────────────┘                   └─────────────┬─────────────┘
                                                                 │ JDBC
                                                                 ▼
                                                   ┌───────────────────────────┐
                                                   │ Render Managed PostgreSQL │
                                                   │ • visasetu_db (PG 16)     │
                                                   └───────────────────────────┘
```

---

## Part 1: Deploy Backend & PostgreSQL on Render

### Method A: One-Click Blueprint (`render.yaml`) — Recommended

1. Log into your [Render Dashboard](https://dashboard.render.com/).
2. Click **New +** in the top navigation and select **Blueprint**.
3. Connect your GitHub repository: `shelakeemahesh/VisaIndia.io`.
4. Render detects [`render.yaml`](../render.yaml) automatically and plans 2 resources:
   - `visaindia-postgres`: Managed PostgreSQL 16 Database
   - `visaindia-backend`: Docker Web Service
5. In the Environment Variables prompt, supply your Razorpay credentials:
   - `RAZORPAY_KEY_ID`: Your Razorpay key ID (`rzp_test_...` or `rzp_live_...`)
   - `RAZORPAY_KEY_SECRET`: Your Razorpay key secret
   - `RAZORPAY_WEBHOOK_SECRET`: Your Razorpay webhook secret
   *(Note: `JWT_SECRET` and `PII_ENCRYPTION_KEY` are auto-generated with cryptographically secure random values by Render).*
6. Click **Apply**.
7. Render provisions the database, executes Flyway migrations (V1–V3), builds the container, and verifies health on `/actuator/health`.
8. Once deployed, copy your backend URL (e.g. `https://visaindia-backend.onrender.com`).

---

### Method B: Manual Render Setup

If you prefer configuring services individually in the Render UI:

#### 1. Create PostgreSQL Database
1. Go to **New +** -> **PostgreSQL**.
2. **Name**: `visaindia-postgres`
3. **Database**: `visasetu_db`
4. **User**: `visasetu_user`
5. **Region**: Oregon (or your preferred region)
6. **Plan**: Free or Starter
7. Click **Create Database**. Copy the **Internal Database URL** once created.

#### 2. Create Backend Web Service
1. Go to **New +** -> **Web Service**.
2. Select your GitHub repository: `shelakeemahesh/VisaIndia.io`.
3. Configure the service:
   - **Name**: `visaindia-backend`
   - **Runtime**: `Docker`
   - **Root Directory**: `backend` (or specify `Dockerfile Path: backend/Dockerfile` and `Docker Context: backend`)
   - **Plan**: Free or Starter
   - **Health Check Path**: `/actuator/health`
4. Add the following **Environment Variables**:
   | Key | Value / Source |
   |---|---|
   | `PORT` | `10000` |
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DATABASE_URL` | Select *Add from Database* -> `visaindia-postgres` -> *Internal Connection String* |
   | `JWT_SECRET` | 32+ character secure random secret (or click *Generate*) |
   | `PII_ENCRYPTION_KEY` | 32-character key or Base64 32-byte secret |
   | `RAZORPAY_KEY_ID` | `rzp_test_...` or `rzp_live_...` |
   | `RAZORPAY_KEY_SECRET` | `...` |
   | `RAZORPAY_WEBHOOK_SECRET` | `...` |
   | `VISASETU_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,https://*.vercel.app` |
5. Click **Create Web Service**.

---

## Part 2: Deploy Frontend on Vercel

1. Log into your [Vercel Dashboard](https://vercel.com/dashboard).
2. Click **Add New…** -> **Project**.
3. Import `shelakeemahesh/VisaIndia.io`.
4. Configure the Project:
   - **Framework Preset**: `Vite`
   - **Root Directory**: Click *Edit* and select `frontend` (if prompted).
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. Under **Environment Variables**, add:
   | Variable | Value |
   |---|---|
   | `VITE_API_URL` | `https://visaindia-backend.onrender.com/api/v1` *(replace with your real Render URL)* |
6. Click **Deploy**.
7. Vercel builds the bundle and deploys to a global Edge CDN URL (e.g. `https://visaindia.vercel.app`).

---

## Part 3: Verify the End-to-End System

### 1. Test Backend Health
Run:
```bash
curl -i https://visaindia-backend.onrender.com/actuator/health
```
Expected response:
```json
HTTP/2 200
{"status":"UP"}
```

### 2. Test Cross-Origin Communication
Open your Vercel URL in your browser:
1. Navigate to `/start-application`.
2. Fill preliminary particulars and select a nationality (e.g., Japan `JPN`).
3. Click **Continue / Register Application** — it generates a persistent application reference (e.g. `VS-2026-XXXX`).
4. Click **Pay Government Fee ($25.00 USD)**:
   - Official Razorpay checkout modal opens securely.
   - Enter test card credentials in Test Mode.
   - On completion, `/api/v1/payments/verify` validates the cryptographic HMAC signature.
   - Status updates to `Under Consular Review` / `Approved`.

---

## Part 4: Configure Razorpay Webhooks (Optional for Async Events)

1. Open your [Razorpay Dashboard](https://dashboard.razorpay.com/) -> **Settings** -> **Webhooks**.
2. Click **Add New Webhook**:
   - **Webhook URL**: `https://<your-render-service>.onrender.com/api/v1/payments/webhook`
   - **Secret**: The same value you set in `RAZORPAY_WEBHOOK_SECRET`
   - **Active Events**:
     - `payment.captured`
     - `order.paid`
     - `payment.failed`
     - `refund.processed`
3. Click **Save**.

---

## Summary of Production Enhancements Implemented

- **SPA Direct Routing**: `frontend/vercel.json` rewrites all deep URLs to `/index.html` to prevent 404 errors on browser page reloads.
- **Dynamic Port Binding**: `server.port: ${PORT:${APP_PORT:8080}}` automatically adapts to Render's allocated container port.
- **Database URL SPI**: `DatabaseUrlEnvironmentPostProcessor` converts Render's `postgres://` connection string into valid Spring Boot JDBC syntax with credentials.
- **CORS Allowed Origin Patterns**: Allows `https://*.vercel.app` and `https://*.onrender.com` dynamically while maintaining credentials support (`AllowCredentials: true`).
- **Container Memory Ergonomics**: JVM flags configured with `-XX:MaxRAMPercentage=75.0` to eliminate container memory kills on 512MB RAM cloud tiers.
- **Actuator Health Resilience**: Actuator health checks isolate PostgreSQL and system uptime, disabling external service alerts when optional queues/caches are absent.
