# 安全审计报告：CordysCRM 健康管理模块

**审计范围**：体检 API 签名、Dify API Key、腾讯短信 SDK、权限控制
**审计时间**：2025-05-18
**审计人**：security-auditor

---

## 发现汇总

| 严重度 | 数量 |
|--------|------|
| Critical | 1 |
| High | 2 |
| Medium | 3 |
| Low | 2 |

---

## 🔴 Critical

### [C-1] 健康模块所有 Controller 缺失权限控制

- **位置**：`backend/crm/src/main/java/cn/cordys/crm/health/controller/*.java`（全部 8 个 Controller）
- **影响**：
  - 所有 `/health/*` 端点无任何权限校验
  - 任何已登录用户可访问全部健康档案（包含身份证号、手机号、体检数据）
  - 任意用户可触发体检数据同步（耗尽第三方 API 配额）
  - 任意用户可调用 AI 解读（消耗 Dify token）
  - 任意用户可推送短信（恶意营销骚扰）
- **证据**：
  ```java
  // HealthArchiveController.java - 无任何权限注解
  @PostMapping("/page")
  public Map<String, Object> page(@RequestBody ...) { ... }
  
  // 对比 product 模块（有权限控制）
  @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
  ```
- **修复建议**：
  1. 在 `PermissionConstants.java` 添加健康模块权限常量
  2. 为所有 Controller 方法添加 `@RequiresPermissions` 注解

---

## 🟠 High

### [H-1] 体检 API AppId 硬编码

- **位置**：`HealthSyncService.java:35`
  ```java
  @Value("${health.api.app-id:166660309689670637991}")
  private String appId;
  ```
- **影响**：AppId 暴露在代码中，攻击者可针对该 AppId 进行暴力猜解签名
- **修复建议**：将 AppId 从代码默认值中移除，强制要求配置文件提供

### [H-2] Dify API Key 暴露在 Wiki 文档中

- **位置**：`wiki/projects/cordys-crm/context.md:131`
  ```markdown
  **API Key**: `app-05l01mm5RV8dfxUJdIxY6ALG`
  ```
- **影响**：API Key 泄露，任何人都能用该 key 调用 AI 健康解读
- **修复建议**：从 Wiki 文档中删除真实 API Key，使用占位符如 `***`

---

## 🟡 Medium

### [M-1] 签名异常时静默失败

- **位置**：`HealthSyncService.java:382-384`
  ```java
  } catch (Exception e) {
      return "";
  }
  ```
- **影响**：签名生成失败时返回空字符串 `""`，导致所有体检 API 请求签名无效或异常
- **修复建议**：签名失败应抛出异常或拒绝请求，而非静默返回空

### [M-2] 体检 API 使用弱哈希算法 MD5

- **位置**：`HealthSyncService.java:375`
  ```java
  MessageDigest md = MessageDigest.getInstance("MD5");
  ```
- **影响**：MD5 已被证明不安全，攻击者可通过彩虹表反推 secret
- **说明**：这是第三方 API 签名规则，修改需协调对方升级

### [M-3] 体检 API 签名中 idCardNo 为空字符串

- **位置**：`HealthSyncService.java:280`
  ```java
  String sign = generateSign(appId, "", secret); // idCardNo 用空字符串
  ```
- **影响**：查询全量数据时使用固定签名，攻击者可枚举时间范围获取全部数据
- **修复建议**：评估此设计是否满足数据安全合规要求（GDPR/个保法）

---

## 🟢 Low

### [L-1] Dify API Key 为空时继续执行

- **位置**：`HealthAiService.java:53-55`
  ```java
  if (difyApiKey != null && !difyApiKey.isBlank()) {
      headers.set("Authorization", "Bearer " + difyApiKey);
  }
  ```
- **影响**：Key 为空时请求不带 Authorization，第三方 API 应返回 401，但代码未处理此情况
- **修复建议**：Key 为空时应拒绝请求并抛出配置异常

### [L-2] 短信发送失败时错误信息写入日志

- **位置**：`SmsSender.java:123`
  ```java
  log.error("短信发送异常 - 手机号：{}，错误码：{}，错误信息：{}", phoneNumber, e.getErrorCode(), e.getMessage(), e);
  ```
- **影响**：日志中记录手机号，存在隐私泄露风险（日志文件访问控制需审查）
- **修复建议**：日志脱敏处理，如 `phoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")`

---

## 修复优先级建议

| 优先级 | 漏洞编号 | 说明 |
|--------|----------|------|
| P0 | C-1 | 立即添加权限控制（高危暴露） |
| P1 | H-2 | 从 Wiki 删除真实 API Key |
| P1 | H-1 | 移除 AppId 硬编码默认值 |
| P2 | M-1 | 签名异常处理改进 |
| P3 | L-1 | Dify Key 空值校验 |
| P3 | L-2 | 日志脱敏 |

---

## 附录：审计方法

- **静态代码审查**：扫描所有 health 模块 Java 文件
- **对比分析**：对比 product/order 模块的权限控制实现
- **配置文件审查**：检查 @Value 注入是否正确
- **文档审查**：检查 Wiki 中的敏感信息暴露
