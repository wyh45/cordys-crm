# Bug-Env-01: Redis 不可用导致 CordysCRM 后端无法启动

## 基本信息
- 发现时间: 2026-05-19
- 发现者: tester Agent
- 严重程度: **P0 阻塞**
- 模块: 基础设施

## 问题描述

CordysCRM 后端依赖 Redis（通过 Redisson 实现分布式锁和会话存储），但当前环境：
1. **Redis 未安装** — `redis-server` 不存在
2. **Redis 配置需要密码** — `commons.properties` 配置了 `spring.data.redis.password=CordysCRM@redis`
3. **环境 Redis 无密码** — Redisson 尝试 AUTH 但 Redis 未设置密码，导致连接失败

## 错误日志

```
Caused by: org.redisson.client.RedisException: ERR Client sent AUTH, 
but no password is set. channel: [... L:/127.0.0.1:47436 - R:127.0.0.1/127.0.0.1:6379] command: (AUTH), params: (password masked)
```

## 影响范围

- **全部** CordysCRM 后端功能不可用（Redisson 连接失败导致 Spring Boot 启动失败）
- P1 功能测试完全无法进行

## 修复方案（3选1）

1. **安装 Redis 并设置密码**（生产推荐）
   ```bash
   apt-get install redis-server
   redis-server --requirepass 'CordysCRM@redis'
   ```

2. **修改配置去掉密码**（开发环境）
   - 修改 `commons.properties` 或 `commons-local.properties` 中的 `spring.data.redis.password`
   - 或在运行时覆盖：`--spring.data.redis.password=`

3. **禁用 Redis**（如果不需要分布式功能）
   - 设置 `redis.embedded.enabled=false` 并配置无 Redis 模式

## 当前环境信息

| 组件 | 状态 |
|------|------|
| Java 21 | ✅ 已安装 (`/home/wyhubuntu/.local/java/jdk-21.0.10+7/bin/java`) |
| MySQL | ✅ 运行中 (127.0.0.1:3306, cordys_crm) |
| Redis | ❌ 未安装，无法连接 |
| CordysCRM JAR | ✅ `app-main.jar` (2026-05-19 09:45, 有 Main-Class 入口) |

## 状态

- [ ] 待修复（Redis 基础设施问题）
