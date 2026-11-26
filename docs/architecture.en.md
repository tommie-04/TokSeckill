# TokenFlash architecture

[中文](architecture.md) · [English](architecture.en.md) · [README](../README.en.md)

## Current system

The build uses Java 17, Spring Boot 2.7.18, MyBatis-Plus 3.5.3.1, MySQL Connector/J 8.0.33, Spring Kafka 2.8.11, Caffeine 2.9.3, and Redisson 3.17.7. LangChain4j 1.0.1 supports the local assistant reference.

| Domain concept | Current mapping |
| --- | --- |
| Model supplier | `tb_shop`, with text, reasoning, and multimodal demo entries |
| Token pack | `tb_voucher.token_units` stores the tokens per pack. Price fields use cents. |
| Stock | MySQL stores durable inventory. Redis tracks available units for reservations. |
| Order | `tb_voucher_order` enforces one order per user and pack. HTTP returns order IDs as strings. |
| HTTP entry points | `GET /token-packs/list/{providerId}`, `POST /token-pack-orders/seckill/{id}`, `GET /token-pack-orders/{id}` |
| Session | Redis Hash → `RefreshTokenInterceptor` → `UserHolder` → `LoginInterceptor` |

The catalog displays a stock snapshot. Lua checks current availability when a user reserves a pack.

## Architecture and request flow

TokenFlash runs as one Spring Boot application. Start in a business package, then follow its controller, service and mapper. The Kafka listener runs in the same application and opens a separate MySQL transaction for each order.

```mermaid
flowchart TD
    B["Browser"] --> N["Nginx"]
    N --> UI["frontend/html/tokenflash"]
    N -->|"/api/"| M["Spring MVC"]
    M --> U["user · login and session"]
    U --> R[(Redis)]
    M --> P["tokenpack · catalog"]
    P -->|caffeine| C["Caffeine L1"]
    P -->|redis| RC["Redis catalog cache"]
    C -->|miss| RC
    RC -->|miss| D[(MySQL)]
    P -->|mysql| D
    M --> S["seckill · reservePack"]
    S --> G["FlashSaleReservations"]
    G -->|"seckill.lua"| R
    S -->|"after admission"| K["OrderEventPublisher → Kafka"]
    K -->|"broker ACK"| ID["HTTP response · order ID"]
    K --> L["OrderEventConsumer"]
    L --> T["persistReservedOrder"]
    T -->|"insert order + decrement stock"| D
    M --> Q["seckill · queryOrderStatus"]
    Q --> D
```

## Directory map

```text
TokenFlash/
├── pom.xml
├── frontend/
│   ├── conf/nginx.conf
│   └── html/tokenflash/          # Catalog, login and assets
├── src/main/java/com/tokenflash/
│   ├── TokenFlashApplication.java
│   ├── user/                     # Login, profiles and request context
│   │   └── auth/ · controller/ · dto/ · entity/ · mapper/ · service/
│   ├── tokenpack/                # Catalog, catalog cache and sale stock
│   │   └── controller/ · entity/ · mapper/ · service/{cache,impl}/
│   ├── seckill/                  # Admission, order status and persistence
│   │   └── controller/ · entity/ · mapper/ · service/impl/
│   ├── provider/                 # Provider profiles, categories and nearby search
│   ├── community/                # Posts, follows and image uploads
│   ├── ai/                       # Local assistant, memory and tools
│   │   └── config/ · controller/ · entity/ · mapper/ · repository/ · service/ · tools/
│   ├── infrastructure/
│   │   ├── cache/                # Reusable cache strategies
│   │   ├── redis/                # Reservations, sessions, sign-ins and indexes
│   │   ├── lock/                 # Distributed locks and Redisson configuration
│   │   ├── limiter/              # Rate-limit annotations and aspect
│   │   └── mq/                   # Kafka publisher, listener and retry policy
│   ├── config/                   # MVC, MyBatis and exception handling
│   └── common/                   # API results and shared types
├── src/main/resources/
│   ├── application.yaml
│   ├── db/                       # Schema, migration and demo fixtures
│   ├── mapper/TokenPackMapper.xml
│   ├── lua/                      # Reservation, rate limiting and unlock
│   └── static/consultant.html
├── src/test/java/com/tokenflash/
├── scripts/warm-token-pack.sh
├── docs/
└── .github/workflows/build.yml
```

## Vue full-stack plan

```mermaid
flowchart LR
    Browser -->|HTTPS| Nginx
    Nginx -->|"Static files / SPA fallback"| Vue["Vue 3 + TypeScript + Vite dist"]
    Vue -->|"Same-origin /api"| Nginx
    Nginx -->|"Reverse proxy :8081"| Boot["Spring Boot MVC"]
    Boot --> Caffeine
    Boot --> Redis[(Redis)]
    Caffeine --> Redis
    Boot --> Kafka --> Consumer
    Boot --> DB[(MySQL)]
    Consumer --> DB
    Boot -. future .-> Tools["Assistant function calls"]
```

```text
frontend/
  app/                      Proposed Vue 3 application
    src/
      api/                  HTTP client and domain endpoints
      routes/               Catalog, login, and order details
      stores/               Pinia session state
      features/
        catalog/            Pack cards and filters
        orders/             Reservation feedback and status polling
      components/           Shared UI
    vite.config.ts
  conf/                     Nginx serves dist and proxies /api
  html/tokenflash/                Static demo during migration
src/main/                   Existing backend and migrations
```

1. Define the response types for catalog, reservation, and order status. Keep order IDs as strings and distinguish an existing order from an unknown or pending order.
2. Build the catalog, login, and order pages with Vue 3, Vite, TypeScript, Vue Router, and Pinia. Use the Composition API. Proxy `/api` during local development.
3. Serve `dist/` with Nginx, using `try_files $uri $uri/ /index.html` for browser routes and a dedicated `/api/` proxy to Spring Boot. Verify sign-in, reservation, and order lookup as a single flow.
4. Keep the backend as a Spring Boot monolith with a Kafka listener. Use cache hit ratios, consumer lag, and transaction latency to guide future scaling decisions.

The standalone Vue app, production deployment, and assistant tools are future work. [Vue Quick Start](https://vuejs.org/guide/quick-start.html)
