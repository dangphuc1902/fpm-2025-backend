# FPM-2025 Backend: Modular Monolith Production Deployment Guide

This guide describes how to deploy the modernized **Lean, Production-Ready Modular Monolith** of the FPM-2025 Backend on a single, cost-effective Virtual Private Server (VPS) using Docker Compose, Nginx, PostgreSQL, and Redis.

---

## 🏗️ Architecture Overview

The system transitions from an expensive, distributed microservices network to a low-overhead, highly performant monolith:

```
                  Client (Next.js / Android)
                             │
                             ▼ (HTTPS / port 80/443)
                       [ Nginx Proxy ]
                             │
                             ▼ (HTTP / port 8080)
            [ Spring Boot Monolith (fpm-monolith) ]
               ├── auth-module
               ├── wallet-module
               ├── transaction-module
               ├── reporting-module
               └── notification-module
               
            /                         \ (JPA)
           ▼                           ▼
    [ Redis Caching ]          [ PostgreSQL Database ]
   (Port 6379 / HSL)          (Port 5432 / Multi-schema)
```

### Key Performance Benefits:
- **Low RAM Overhead**: Under **1.2GB idle memory usage** (originally >8GB for 9 microservice instances).
- **Fast Startup**: Booting in **under 15 seconds** on a standard 2 vCPU VPS (originally >3 minutes due to Zookeeper/Eureka/Config sync).
- **In-Memory Pub/Sub**: Extremely low-latency event-driven state updates using Spring `ApplicationEventPublisher` and `@Async` handlers.

---

## 🚀 Pre-requisites & Server Setup

Ensure your budget VPS meets the following:
- **OS**: Ubuntu 22.04 LTS or newer.
- **Hardware**: Minimum 1 vCPU, 2GB RAM (4GB RAM, 2 vCPU recommended for G1GC headroom).
- **Tools Installed**: Docker Engine, Docker Compose.

---

## 🛠️ Step-by-Step Deployment

### 1. Prepare Environment Configuration
Clone the repository onto your VPS. Duplicate the `.env.template` file to `.env`:
```bash
cp .env.template .env
```
Open `.env` and fill in secure passwords for your database and JWT secret:
```ini
DB_USER=dev
DB_PASS=vps_secure_db_password_here
JWT_SECRET=production_jwt_secret_key_minimum_64_characters_long_for_security
```

### 2. Build and Start the Stack
Run Docker Compose in detached mode. This command compiles the entire Maven project inside a secure build container and launches the production runtime:
```bash
docker compose up --build -d
```

### 3. Verify Container Status
Check that all containers are healthy and running:
```bash
docker compose ps
```
You should see:
- `fpm-nginx` running on ports `80` and `443` (State: healthy).
- `fpm-monolith` exposing port `8080` internally (State: healthy).
- `fpm-postgres` running on port `5432` (State: healthy).
- `fpm-redis` running on port `6379` (State: healthy).

---

## 📈 Monitoring & Logging

### Viewing Application Logs
To stream logs from the Spring Boot Monolith:
```bash
docker compose logs -f fpm-monolith
```

### Health Check Endpoint
Nginx routes Actuator endpoints securely. You can verify overall system health:
```bash
curl http://localhost/actuator/health
```
Response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "redis": { "status": "UP" }
  }
}
```

---

## 🔐 Security Hardening & VPS Best Practices

1. **Database Firewalling**: PostgreSQL and Redis are protected inside the Docker private bridge network. Do NOT expose port `5432` or `6379` directly to the public web via VPS firewall (e.g., `ufw`).
2. **Nginx SSL Configuration**:
   For actual production, configure Let's Encrypt SSL inside `nginx.conf`:
   ```nginx
   server {
       listen 443 ssl;
       server_name yourdomain.com;
       ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
       ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
       ...
   }
   ```
3. **JVM Memory Tuning**:
   The monolith's JVM parameters in the `Dockerfile` are configured for optimum container resource bounds:
   - `-XX:+UseG1GC` to optimize pause times and memory compaction.
   - `-XX:MaxRAMPercentage=75.0` to prevent OOM killer issues on small VPS servers.
