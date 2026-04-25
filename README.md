# Grid07 Backend Assignment

Spring Boot REST API for managing posts and nested bot/human comment threads, with Redis-backed rate limiting and concurrency control.

## Tech Stack

- Java 17, Spring Boot 3
- PostgreSQL (persistent storage)
- Redis (rate limiting, virality scores, notification queues)
- Docker

## Running Locally

```bash
docker-compose up -d
mvn spring-boot:run
```

Server starts at `localhost:8080`. Import `postman_collection.json` to test the endpoints.

---

## Key Features

**Virality Scoring** — Bot replies, human likes, and comments each increment a Redis key (`INCRBY`) in real-time. No DB hit on every interaction.

**Bot Guardrails**
- Horizontal Cap: max 100 bot replies per post
- Vertical Cap: max 20 levels of comment depth
- Cooldown Cap: a bot can only interact with a specific human once per 10 minutes

**Notification Batching** — First bot interaction notifies the user immediately. Follow-ups within 15 minutes get queued in a Redis List. A `@Scheduled` job runs every 5 minutes and logs a digest summary.

---

## Thread Safety

The Horizontal Cap uses Redis `INCR`. Since Redis is single-threaded, all 200 concurrent requests are processed one by one — thread 100 succeeds, thread 101 gets a `429` immediately. No race condition possible.

**Trade-off:** Redis checks run before the DB transaction opens. If Postgres fails after Redis approves, the counter gets inflated by +1. A Lua script would fix this in production.
