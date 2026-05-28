# CordysCRM 测试账号

> ⚠️ Placeholder — 待 PM 提供真实测试账号

## 已知账号

| 角色 | 账号 | 密码 | 备注 |
|------|------|------|------|
| 管理员 | admin | CordysCRM | 仅验证可用，未确认权限范围 |
| 待补充 | — | — | — |
| 待补充 | — | — | — |

## 凭证信息

- **登录接口**: `POST /login`
- **Body 格式**:
  ```json
  {
    "username": "admin",
    "password": "CordysCRM",
    "platform": "web",
    "authenticate": "LOCAL"
  }
  ```
- **认证头**: `X-AUTH-TOKEN: <sessionId>` 或 Cookie `rememberMe`
- **CSRF Token**: `X-CSRF-TOKEN` header

## 多角色测试需求

需覆盖以下角色测试权限矩阵：
- [ ] admin — 全部菜单可见
- [ ] 普通用户 — 权限受限
- [ ] 团队负责人 — 部分管理功能
- [ ] 财务 — 财务相关菜单

## 待 PM 补充

- [ ] 各角色测试账号（username/password）
- [ ] 各角色的菜单权限预期（哪些菜单可见/不可见）
- [ ] 测试数据初始化脚本（如需要）
