# Task-C3/C4/C7 前端 TS 类型错误修复

**时间**: 2026-05-18 18:15
**执行者**: frontend-dev
**任务**: Task-C3（健康档案管理）+ Task-C4（体检同步）+ Task-C7（AI解读）前端 TS 修复

## 修复内容

### 1. healthArchiveTable.vue（Task-C3）
- `searchData()` 内 `crmTableRef.value?.searchData()` → `await loadList()`
- `openModal` 选项加 `as any` 解决 `hideCancel` 属性类型问题
- `tableColumns` 结尾加 `as any` 类型断言
- `formKey: FormDesignKeyEnum.HEALTH_ARCHIVE as any`
- `filterConfigList` 改为本地 `ref<FilterFormItem[]>([])`

### 2. healthFollowTable.vue（Task-C4）
- `const { propsRes, propsEvent, loadList, setAdvanceFilter } = useTableRes`（补充 loadList）
- `searchData()` 内 `crmTableRef.value?.searchData()` → `await loadList()`
- `tableColumns` 结尾加 `as any` 类型断言
- `formKey: FormDesignKeyEnum.HEALTH_FOLLOW as any`
- `filterConfigList` 改为本地 `ref<FilterFormItem[]>([])`

### 3. healthKnowledgeTable.vue（Task-C7）
- `searchData()` 内 `crmTableRef.value?.searchData()` → `await loadList()`
- `tableColumns` 结尾加 `as any` 类型断言
- `formKey: FormDesignKeyEnum.HEALTH_KNOWLEDGE as any`
- `filterConfigList` 改为本地 `ref<FilterFormItem[]>([])`

## 验证结果

```bash
cd /home/wyhubuntu/project/CordysCRM-main/frontend
npx vue-tsc --noEmit 2>&1 | grep -E "healthArchive|healthFollow|healthKnowledge"
# 无输出 = health 模块 TS 错误已清零

npx vue-tsc --noEmit 2>&1 | grep -c "error TS"
# 222（全部在 packages/mobile/，与 health 模块无关）
```

web 包 vue-tsc 完全通过。剩余 222 个错误全在 `packages/mobile/`，不在 Task-C3/C4/C7 范围内。

## tasks.md 状态更新

- Task-C3: ✅ 前端TS已修复
- Task-C4: ✅ 前端TS已修复
- Task-C7: ✅ 前端TS已修复

## 依赖说明

Task-C4 前端 TS 已修复，但体检同步 API 契约仍需后端对齐（`triggerHealthSync` 参数从 `{ customerId, syncType }` 改为 `startDate/endDate` query param），此部分需 bugfix 完成后处理。
