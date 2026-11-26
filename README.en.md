<div align="center">

# TokenPack Flash Sale

[![中文](https://img.shields.io/badge/README-%E4%B8%AD%E6%96%87-1d624b)](README.md)
[![English](https://img.shields.io/badge/README-English-2563eb)](README.en.md)

**A flash-sale platform for LLM token packs**

Browse text, reasoning, and multimodal model packs, reserve one during a campaign, and check your order status.

[![Build](https://github.com/tommie-04/CityPerks/actions/workflows/build.yml/badge.svg)](https://github.com/tommie-04/CityPerks/actions/workflows/build.yml)
![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 2.7.18](https://img.shields.io/badge/Spring_Boot-2.7.18-6DB33F?logo=springboot&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Lua-DC382D?logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?logo=apachekafka&logoColor=white)
![Caffeine](https://img.shields.io/badge/Caffeine-L1_Cache-795548)
![MySQL](https://img.shields.io/badge/MySQL-MyBatis--Plus-4479A1?logo=mysql&logoColor=white)

[Architecture](docs/architecture.en.md) · [Quick start](#quick-start) · [Learning and interviews](docs/learning-guide.md) · [Vue full-stack plan](docs/architecture.en.md#vue-full-stack-plan)

</div>

## How it works

- **Browse packs.** The catalog shows the model category, token allowance, price, and sale window. It can read from MySQL directly, from Redis with MySQL fallback, or through Caffeine → Redis → MySQL.
- **Reserve a pack.** A Redis Lua script verifies the sale window, available inventory, and one-pack-per-user rule, then reserves one unit atomically. The API returns an order ID upon Kafka broker acknowledgment.
- **Create the order.** The Kafka consumer inserts the order and decrements durable stock in one MySQL transaction. The order primary key and user/pack unique constraint support idempotent processing. A signed-in user can check whether their order has been created.

The demo covers the pack catalog, reservation, and order creation. Payment, real model-credit fulfillment, order expiry, recovery of failed events, and token-pack tools for the assistant are future work.

| Module | Code entry point |
| --- | --- |
| Catalog and two-level caching | [TokenPackController](src/main/java/com/hmdp/tokenpack/controller/TokenPackController.java) → [TokenPackCatalogCache](src/main/java/com/hmdp/tokenpack/service/cache/TokenPackCatalogCache.java) |
| Flash sale and order lookup | [TokenPackOrderController](src/main/java/com/hmdp/seckill/controller/TokenPackOrderController.java) → [TokenPackOrderServiceImpl](src/main/java/com/hmdp/seckill/service/impl/TokenPackOrderServiceImpl.java) → [seckill.lua](src/main/resources/lua/seckill.lua) |
| Consumer, retries, and transaction | [OrderEventConsumer](src/main/java/com/hmdp/infrastructure/mq/OrderEventConsumer.java) · [KafkaConfig](src/main/java/com/hmdp/infrastructure/mq/KafkaConfig.java) |
| Login and rate limiting | [MvcConfig](src/main/java/com/hmdp/config/MvcConfig.java) · [limiter/](src/main/java/com/hmdp/limiter/) |
| Course assistant reference | [ai/](src/main/java/com/hmdp/ai/) contains a LangChain4j and Redis chat history example, disabled by default |

## Quick start

Install JDK 17, Maven 3.6+, MySQL, Redis, Kafka, and Nginx. Use a fresh local database. The base SQL recreates the training tables in that database.

```bash
mysql -u root -p -e "CREATE DATABASE hmdp CHARACTER SET utf8mb4;"
mysql -u root -p hmdp < src/main/resources/db/hmdp.sql
mysql -u root -p hmdp < src/main/resources/db/token-packs.sql
mysql -u root -p hmdp < src/main/resources/db/token-pack-demo.sql
```

For an existing learning database, back it up and apply `token-packs.sql` once. If the unique index fails to build, check for duplicate `(user_id, voucher_id)` pairs. Demo fixtures use provider and pack IDs 101–103 and open a 30-day campaign upon import.

After starting MySQL, Redis, and Kafka, create the order topic with the Kafka CLI. This example uses a single broker.

```bash
kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --create --if-not-exists \
  --topic seckill-voucher-order --partitions 3 --replication-factor 1
```

Build and start the backend. The settings below enable the Caffeine mode and demo login. The training implementation writes the verification code to the local DEBUG log.

```bash
export MYSQL_USER=root
export MYSQL_PASSWORD='your-local-mysql-password'
export REDIS_PASSWORD='your-local-redis-password'
export TOKEN_PACK_CACHE_MODE=caffeine
export APP_LOG_LEVEL=debug
./scripts/warm-token-pack.sh 101 102 103
mvn -DskipTests package
java -jar target/hm-dianping-0.0.1-SNAPSHOT.jar
```

Leave `REDIS_PASSWORD` empty if Redis does not require a password. The backend defaults to port `8081`, Redis DB 0 at `127.0.0.1:6379`, Kafka at `127.0.0.1:9092`, and the `mysql` catalog mode. See [application.yaml](src/main/resources/application.yaml). Match the warmup script's MySQL and Redis settings to the backend. The script sets missing stock keys and updates sale windows. Use a separate Redis database for the demo to isolate any flash-sale state from older versions.

From the repository root in a second terminal, start Nginx.

```bash
mkdir -p frontend/logs
nginx -p "$PWD/frontend/" -c conf/nginx.conf
```

Open the [local catalog](http://127.0.0.1:8080/). Choose a pack, sign in, submit the reservation, and refresh the order state. Displayed stock is a snapshot. The Redis Lua script checks availability when users submit their requests.

## Implementation status and roadmap

The Kafka listener commits offsets after each record is handled. It retries failures up to three times at one-second intervals and logs exhausted records. The Redis reservation, Kafka publication, and MySQL transaction span separate systems. Uncertain publication or exhausted retries leave reservations for reconciliation. Durable recovery, replay, and compensation are future work.

The learning order is **Caffeine and Guava → Kafka acknowledgments, retries, and idempotency → Vue 3 frontend → Function Calling**. The course assistant is available under a local reference profile. Future token-pack tools start with read-only catalog queries. [Learning and interview guide, in Chinese](docs/learning-guide.md)

## Credits

Based on the [Heima Redis course](https://www.bilibili.com/video/BV1cr4y1671t), its [base code](https://github.com/cs001020/hmdp), and the [extended study materials](https://www.yuque.com/kdoxioc/qp0wtr/zvp8tfp6sunsglpt). Original author attribution is preserved in the source. The repository retains its historical name, CityPerks.
