# 学习与面试路线

按简历中的业务问题学习。每一部分先顺着代码走一次，再做验证，最后练习口述。缓存和消息队列部分参考《黑马点评》扩展材料中的多级缓存、Ack 模式、失败重试和幂等章节。

## 1. Token 包目录与 Caffeine

代码路径从 `TokenPackController` 到 `TokenPackServiceImpl`、`TokenPackCatalogCache`，最后到 `TokenPackMapper.xml`。用相同请求逐个切换 `TOKEN_PACK_CACHE_MODE=mysql|redis|caffeine`，观察 L1 本地命中、Redis 共享命中和 MySQL 回源。默认值是 `mysql`，本地配置示例使用 `caffeine`。

- Caffeine 的 `maximumSize=1000` 限制单个 JVM 缓存条目数。W-TinyLFU 同时利用近期访问和历史频率挑选值得保留的条目，对短时突发和大量一次性查询有区分能力。Guava Cache 也提供容量限制、过期和加载。它有不同的基于 LRU 的容量淘汰实现，默认 `reload` 同步，也可自定义成异步。这里选择 Caffeine，是因为项目主要需要 JVM 本地缓存、热点保留与执行器上的异步刷新。命中率与延迟差异应以相同负载的测试为依据。[Caffeine 与 Guava](https://github.com/ben-manes/caffeine/wiki/Guava) · [Caffeine 淘汰策略](https://github.com/ben-manes/caffeine/wiki/Efficiency) · [Guava CacheBuilder](https://guava.dev/releases/33.4.5-jre/api/docs/com/google/common/cache/CacheBuilder.html)
- `refreshAfterWrite=5s` 到期后，下一次读触发刷新，刷新期间可返回旧值。 `expireAfterWrite=10m` 是硬过期，过期后的读取走加载。L1 刷新也可能读到 Redis 中尚有效的旧值。L2 TTL 为 30 分钟，多实例的 L1 各自持有副本，目录库存允许短时滞后，秒杀资格由 Redis Lua 决定。[Caffeine Refresh](https://github.com/ben-manes/caffeine/wiki/Refresh)
- 扩展追问覆盖热点击穿、空值防穿透、批量过期、两级失效和线程池。区分 `CacheClient` 的商户逻辑过期示例和 `TokenPackCatalogCache` 的目录刷新。后续可加入缓存命中率、回源次数、刷新失败次数和 P95 延迟。

口述示例：“Token 包目录读多写少，我用 Caffeine 缓存各实例的热点目录，Redis 提供共享缓存，未命中时回 MySQL。展示库存允许短时滞后，真正的扣减资格由 Lua 统一检查。”

## 2. Kafka 异步创建订单与扣库存

顺序是 **Redis Lua → Kafka broker → 订单消费者 → MySQL 事务**。Lua 原子判断窗口、库存、用户资格并预扣一份；HTTP 在收到 Kafka 发布确认后给出订单号；消费者落单并扣减 MySQL 库存。

| 面试问题 | 项目中的回答 |
| --- | --- |
| 生产者的 `acks=all` 确认了什么？ | broker 按 ISR 复制条件确认写入，持久性还取决于副本数和 `min.insync.replicas`。本地只有一个 broker。 |
| 消费者何时提交 offset？ | 关闭 Kafka 自动提交，容器用 `AckMode.RECORD`，监听方法成功返回后按记录提交。异常从事务方法抛出，由 Spring Kafka 执行重试。 |
| `RECORD`、`BATCH` 和 `MANUAL_IMMEDIATE` 有什么区别？ | `RECORD` 以成功处理的单条记录为提交单位，`BATCH` 按 poll 批次，`MANUAL_IMMEDIATE` 由监听器显式确认，在 consumer 线程调用时立即提交。参考文档的手动 ack 示例可以用来对照当前代码。 |
| 数据提交了，提交位点前进程崩溃怎么办？ | 记录会再次投递。已有订单先被识别，唯一索引兜底并发冲突。首次创建的订单和扣库存都在同一事务里，异常整笔回滚。 |
| 重试耗尽怎么办？ | 当前默认有限重试并记录 topic、partition、offset 和 key。下一步引入 DLT、告警、人工重放及对账。错误分类防止无效消息反复执行。 |
| Kafka producer 幂等能替代订单幂等吗？ | Producer 幂等约束的是 broker 的写入重试。订单幂等由 MySQL 的业务唯一键和事务实现。 |
| Lua 成功后进程退出，或者 Kafka 发布超时怎么办？ | 此时 Redis 和 Kafka 之间存在故障窗口。当前会保留不确定的预扣状态。改进方向是持久化预扣记录与可重试发布任务，再按订单和消息状态对账补偿。先确定最终是否有订单，再决定是否回补。 |

[Spring Kafka 2.8 消费、异常和恢复](https://docs.spring.io/spring-kafka/docs/2.8.x/reference/html/)

验证时先检查一条消息对应一条订单，再重放相同事件、并发提交同一个包，以及模拟数据库异常。对照 MySQL 事务前后的订单与库存。后续单独练习恢复前后的 offset、DLT payload、重放条件和 Redis 补偿。如果用 DLT 承接失败，失败记录应足够做对账，恢复成功后再允许跳过原记录。

口述示例：“接口把持久化订单和扣库存放到 Kafka 消费侧。MySQL 事务提交后才成功结束消费方法。遇到重复投递，通过用户与包的唯一约束及事务保证同一订单只扣一次库存。”

## 3. Vue 3 与 Nginx

先实现目录、登录、秒杀反馈和订单确认，沿用 Java API。开发使用 Vite 代理，部署时 Nginx 提供构建产物并转发 `/api`。目录与结构见[架构文档](architecture.md#vue-全栈方案)。

## 4. Function Calling

先理解 `ChatController → ConsultantService → AiConfig` 如何注册工具，及 `RedisChatMemoryStore` 如何保存会话窗口。课程示例在 `consultant-local` profile 中供本机阅读和调试。Token 包版本优先从只读的目录和活动查询开始，订单查询绑定登录用户。涉及写入的工具等业务权限和确认流程设计完成后再加入。

## 简历草稿

- 搭建基于 Spring Boot、MySQL、Redis Lua、Kafka 的大模型 Token 包秒杀平台，使用 Lua 原子预扣活动库存与校验一人一包。
- 将创建订单和数据库扣库存移至 Kafka 消费侧，通过记录级 offset 提交、有限重试、数据库事务与业务唯一索引处理重复消费。
- 实现基于 Caffeine 与 Redis 的 Token 包目录缓存，支持配置切换 MySQL、Redis、两级缓存三种路径。

压测、跨系统补偿、Vue SPA 与客服上线能力等实际完成后，再补进简历。
