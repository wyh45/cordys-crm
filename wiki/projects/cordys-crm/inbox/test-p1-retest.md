# CordysCRM P1 Retest — TypeScript 编译验证

**测试者**: tester Agent
**测试时间**: 2026-05-18 下午
**测试目标**: 验证前端声称"TS 编译零错误"是否属实
**结论**: ❌ **FAILS** — 声称与实际不符

---

## 测试 1: npx tsc --noEmit（根目录，全量）

```bash
cd /home/wyhubuntu/project/CordysCRM-main/frontend && npx tsc --noEmit
```

**结果**: ❌ **288 errors** — 全部 `TS2307` (Cannot find module) 和 `TS7006` (implicit any)

**根因**: 根目录 `tsconfig.json` 用 `moduleResolution: "node"`，但子包 `@/` 路径映射 (`paths`) 定义在 `packages/web/tsconfig.json` 和 `packages/mobile/tsconfig.json` 中，根目录 tsconfig **不继承** 子包的 paths 配置。

```
packages/web/tsconfig.json:
  "paths": { "@/*": ["./src/*"] }  ← 只有子包 tsconfig 有此配置

root tsconfig.json:
  "paths": { "#/*": ["/types/*"] } ← 只有 #/*，没有 @/*
```

因此根目录 tsc 扫描所有 `packages/**` 时，数百个 `@/` 引用全部解析失败 → 288 errors。

**判定**: ❌ **FAILS** — 声称零错误，实际 288 个。

---

## 测试 2: pnpm vue-tsc --noEmit（packages/web）

```bash
cd /home/wyhubuntu/project/CordysCRM-main/frontend/packages/web && pnpm vue-tsc --noEmit
```

**结果**: ❌ **56 errors**

**按文件分类**:

| 文件 | 错误数 | 代表性错误 |
|------|--------|-----------|
| healthSyncPanel.vue | ~11 | `HealthSyncResult` has no 'data' property |
| healthKnowledgeTable.vue | ~8 | `HealthKnowledgeListItem` not exported, `propsRes`/`propsEvent` don't exist |
| healthPushPanel.vue | ~4 | `getHealthPushPage`/`HealthPushRecord` not exported, `viewId` missing |
| useFormCreateApi.ts | ~15+ | implicit any (e.g. `Parameter 'e' implicitly has an 'any' type`) |
| useFormCreateTable.ts | ~5+ | implicit any |
| 其他文件 | ~13 | mix of missing modules + implicit any |

**判定**: ❌ **FAILS** — 健康模块组件仍有 TS 类型错误，且业务 hooks 有大量 implicit any。

---

## 测试 3: pnpm vue-tsc --noEmit（packages/mobile）

```bash
cd /home/wyhubuntu/project/CordysCRM-main/frontend/packages/mobile && pnpm vue-tsc --noEmit
```

**结果**: ❌ **6 errors**

```
crm-follow-list/useFollowApi.ts:5:46  Cannot find module '@/config/follow'
crm-follow-list/useFollowApi.ts:7:33  Cannot find module '@/enums/routeEnum'
crm-follow-list/config.ts:6:25        Cannot find module '@/store/modules/app'
crm-follow-list/config.ts:7:34        Cannot find module '@/utils/permission'
useFollowApi.ts:5:46                  Cannot find module '@/config/follow'
useFollowApi.ts:7:33                  Cannot find module '@/enums/routeEnum'
(6 total, 2 duplicates)
```

**判定**: ❌ **FAILS** — mobile 仍有路径解析错误。

---

## 测试 4: 对比上次 P1 测试（test-p1.md）

上次 `test-p1.md` (2026-05-18) 记录的错误与本次对比:

| 包 | 上次 (test-p1.md) | 本次 (npx tsc) | 本次 (vue-tsc) |
|----|------------------|----------------|----------------|
| web (health组件) | 10+ errors | ~56 errors (含非health) | ~11 (health only) |
| mobile | 3 errors | 6 errors | 6 errors |

healthSyncPanel.vue 的 `HealthSyncResult.data` 问题**仍未修复**（上次 test-p1.md 第 88 行已记录，本次仍在）。

---

## 验证总结

| 检查项 | 命令 | 结果 | 通过标准 |
|--------|------|------|----------|
| 根目录 tsc 无错 | `npx tsc --noEmit` | ❌ 288 errors | 0 errors |
| web vue-tsc 无错 | `pnpm vue-tsc --noEmit` (web) | ❌ 56 errors | 0 errors |
| mobile vue-tsc 无错 | `pnpm vue-tsc --noEmit` (mobile) | ❌ 6 errors | 0 errors |

**总判定**: ❌ **FAILS**

---

## 前端声称 vs 实际对比

| 声称 | 实际 |
|------|------|
| "TS 编译零错误" | 根 tsc: **288 errors** |
| "vue-tsc web: zero errors" | vue-tsc web: **56 errors** |
| "vue-tsc mobile: zero errors" | vue-tsc mobile: **6 errors** |

---

## 发现的持续性 Bug（未修复）

### Bug-Frontend-TS-01（来自 test-p1.md，仍未修复）
- **文件**: `packages/web/src/views/health/components/healthSyncPanel.vue`
- **行**: 162-194
- **问题**: `HealthSyncResult` has no 'data' property
- **状态**: ❌ 未修复（自 2026-05-18 test-p1.md 至今）

### Bug-Frontend-TS-02（来自 test-p1.md，仍未修复）
- **文件**: `packages/web/src/views/health/components/healthPushPanel.vue`
- **行**: ~113
- **问题**: `getHealthPushPage`/`HealthPushRecord` not exported from `@/api/modules`
- **状态**: ❌ 未修复

### Bug-Frontend-TS-03（来自 test-p1.md，仍未修复）
- **文件**: `packages/web/src/views/health/components/healthKnowledgeTable.vue`
- **行**: 68-195
- **问题**: `HealthKnowledgeListItem` not exported, `propsRes`/`propsEvent`/`searchData` don't exist
- **状态**: ❌ 未修复

### 新增：隐式 any 类型错误（来自 vue-tsc web）
- **文件**: `useFormCreateApi.ts` (15+ 处), `useFormCreateTable.ts` (5+ 处), `useTableStore.ts` 等
- **问题**: `Parameter implicitly has an 'any' type` — `strict: true` 下不允许
- **状态**: ❌ 新发现

---

## 状态

- [ ] 待修复

## 下一步

1. 前端 dev 必须修复 health 模块 3 个组件的 TS 类型错误（TS-01/02/03）
2. 前端 dev 修复 web hooks 的 implicit any 问题
3. 前端 dev 修复 mobile 包的 `@/` 路径解析错误
4. 修复完成后重新运行 `npx tsc --noEmit` + `vue-tsc --noEmit` 验证
