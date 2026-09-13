# URL Shortener

A Spring Boot REST API that shortens URLs, stores them in **PostgreSQL (Neon)**, and caches redirects in **Redis (Upstash)**.

---

## Tech Stack

| Layer        | Technology                              |
|--------------|-----------------------------------------|
| Framework    | Spring Boot 4.x (Java 21)               |
| Database     | PostgreSQL via [Neon](https://neon.tech) |
| Cache        | Redis via [Upstash](https://upstash.com) |
| Hosting      | [Render](https://render.com)            |
| Build        | Maven (mvnw wrapper included)           |

---

## Endpoints

| Method | Path                  | Description                              |
|--------|-----------------------|------------------------------------------|
| POST   | `/api/v1/shorturl`    | Shorten a URL                            |
| GET    | `/{hash}`             | Redirect to the original URL (302 Found) |

### POST `/api/v1/shorturl`

**Request body:**
```json
{
  "url": "https://example.com/very/long/path",
  "userId": "user-123"
}
```

**Response (200 OK):**
```json
{
  "originalUrl": "https://example.com/very/long/path",
  "shortUrl": "https://your-domain.com/Ab3Cd7E",
  "userId": "user-123",
  "createdAt": 1700000000000
}
```

---

## Environment Variables

Copy `.env.example` and fill in your values.  
On **Render**, set these in **Environment → Environment Variables** in the dashboard.

| Variable            | Description                                                     |
|---------------------|-----------------------------------------------------------------|
| `DATABASE_URL`      | JDBC URL from Neon (include `?sslmode=require`)                 |
| `DATABASE_USERNAME` | Neon database username                                          |
| `DATABASE_PASSWORD` | Neon database password                                          |
| `REDIS_URL`         | Upstash Redis URL — **must** use `rediss://` (TLS)             |
| `BASE_URL`          | Public base URL for short links (e.g. `https://yourdomain.com/`) |
| `PORT`              | HTTP port — Render injects this automatically                   |

---

## Local Development

1. **Clone the repo**
   ```bash
   git clone https://github.com/Mr-Vishal-Ranjan/urlshortner.git
   cd urlshortner
   ```

2. **Start local PostgreSQL & Redis** (Docker is the easiest way)
   ```bash
   docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=urlshortener postgres:16
   docker run -d -p 6379:6379 redis:7
   ```

3. **Run with the `local` profile** (uses `application-local.properties` — already git-ignored)
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```
   Or on Windows:
   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

---

## Deploying to Render

1. Push your code to GitHub.
2. In the Render dashboard → **New → Web Service** → connect your repo.
3. Set the following:
   - **Environment**: `Java`
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/urlshortner-0.0.1-SNAPSHOT.jar`
4. Add all environment variables listed in the table above under **Environment → Environment Variables**.
5. Deploy — Render will build and start the service automatically.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/bitly/urlshortner/
│   │   ├── config/          # Redis & other Spring config
│   │   ├── controller/      # REST controllers & DTOs
│   │   ├── dao/             # JPA models & repositories
│   │   ├── exception/       # Custom exceptions & global handler
│   │   ├── service/         # Business logic
│   │   └── utils/           # Hash generator, Redis service
│   └── resources/
│       ├── application.properties        # Main config (env-driven)
│       └── application-local.properties  # Local overrides (git-ignored)
└── test/
    └── java/...                          # Unit tests
```
