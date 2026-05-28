# P1 Frontend 开发报告

**时间**: 2026-05-18
**执行者**: frontend-dev
**任务**: CordysCRM Health Module P1 前端开发

---

## 一、需求覆盖

| 任务 | 描述 | 状态 |
|------|------|------|
| C3 | Archive 列表 + 详情抽屉（查看/编辑/删除） | done |
| C4 | Sync 触发表单 + 轮询 + 同步日志 | done |
| C5 | Archive 编辑（过敏史/病史/疫苗 CRUD） | done |
| C7 | AI 解读页面（表单 + 结果展示） | done |

---

## 二、文件改动清单

### 2.1 新增/替换组件

**`frontend/packages/web/src/views/health/components/healthArchiveDetail.vue`** (24,661 bytes)
- 替换原 CrmFormCreateDrawer 为独立抽屉组件
- n-tabs 布局：基本信息 / 过敏史 / 病史 / 疫苗 / 体检记录
- 过敏史/病史/疫苗 inline CRUD（添加/编辑/删除确认）
- 删除后自动刷新列表

**`frontend/packages/web/src/views/health/components/healthArchiveTable.vue`** (patch)
- 移除 `getHealthArchiveList`、`healthAiInterpret` 无用 import
- 移除 `CrmFormCreateDrawer`，改用 `<HealthArchiveDetail>` 抽屉
- `handleOperationSelect` 新增 `case 'detail'` 分支打开详情抽屉

**`frontend/packages/web/src/views/health/components/healthSyncPanel.vue`** (10,171 bytes)
- 两栏布局：左侧同步控制表单 + 右侧同步日志
- 表单字段：startDate、endDate（日期范围）、note
- 提交后 3 秒轮询 `healthGetSyncStatus`
- 日志区：状态徽章（pending=蓝/processing=黄/completed=绿/failed=红）+ 时间戳
- 错误处理：轮询失败时停止并提示

**`frontend/packages/web/src/views/health/ai/index.vue`** (14,475 bytes)
- 两栏布局：左侧输入表单 + 右侧结果面板
- 表单字段：archiveId（档案选择）、reportType（报告类型）、reportData（验血数据：血糖/胆固醇/甘油三酯/HDL/LDL/ALT/AST/血压）
- 结果面板：interpretation（综合解读）、suggestions（健康建议）、warnings（注意事项）、一键复制
- 轮询逻辑：异步模式下每 3 秒轮询结果

### 2.2 国际化更新

**`frontend/packages/web/src/views/health/locale/zh-CN.ts`**
新增 keys（C7 + C4）：selectArchive, reportType, reportData, startInterpret, aiResult, interpretation, suggestions, warnings, copyResult, bloodGlucose, totalCholesterol, triglyceride, hdl, ldl, alt, ast, bloodPressurePlaceholder, bloodPressureUnit, syncLog, syncStatus, syncStarted, syncPending, syncProcessing, syncCompleted, dateRangeInvalid, stopPolling, syncStatusFailed, statusPending, statusProcessing, statusCompleted, statusFailed
公共 keys: common.reset, common.clear, common.copySuccess, common.copyFailed, common.loadFailed, common.deleteFailed, common.saveFailed, common.noData

**`frontend/packages/web/src/views/health/locale/en-US.ts`**
同上英文翻译（44 keys）。

### 2.3 CRUD 子记录 keys（同步到中英）
allergen, severity, diseaseName, diagnosisDate, treatmentStatus, vaccineName, vaccinateDate, nextDoseDate, examNo, examDate, hospital, examType, abnormalCount, findings, viewDetail, basicInfo

---

## 三、API 调用规范

所有 API 调用统一使用 `healthApi`（来自 `@/api/modules/index.ts` 的别名）：

| 用途 | 方法 |
|------|------|
| 获取档案列表 | `healthApi.getHealthArchiveList(params)` |
| 删除档案 | `healthApi.deleteHealthArchive(id)` |
| AI 解读 | `healthApi.healthAiInterpret(params)` |
| 发起体检同步 | `healthApi.healthExaminationSync(params)` |
| 查询同步状态 | `healthApi.healthGetSyncStatus()` |

---

## 四、组件架构

```
views/health/
├── archive/
│   └── index.vue          # 档案列表父页面（不变）
├── components/
│   ├── healthArchiveTable.vue    # 列表 + 详情抽屉入口
│   ├── healthArchiveDetail.vue   # 详情抽屉（tabs + inline CRUD）
│   └── healthSyncPanel.vue       # 体检同步面板（表单 + 轮询日志）
├── ai/
│   └── index.vue          # AI 解读页面
└── locale/
    ├── zh-CN.ts           # 中文 i18n
    └── en-US.ts           # 英文 i18n
```

---

## 五、验收标准

### C3 档案列表 + 详情
- [ ] 列表显示档案数据，包含姓名/性别/年龄/建档日期
- [ ] 点击"查看详情"打开抽屉
- [ ] 抽屉内 tabs 切换：基本信息/过敏史/病史/疫苗/体检记录
- [ ] 过敏史支持增删改（弹确认）
- [ ] 病史支持增删改
- [ ] 疫苗支持增删改
- [ ] 删除后列表自动刷新

### C4 体检同步
- [ ] 表单填写日期范围 + 备注
- [ ] 提交后右侧日志实时更新状态
- [ ] pending=蓝色 processing=黄色 completed=绿色 failed=红色
- [ ] 同步完成后自动停止轮询
- [ ] 日期校验：开始日期不能晚于结束日期

### C7 AI 解读
- [ ] 选择档案 + 报告类型
- [ ] 输入验血指标（血糖/胆固醇/血压等）
- [ ] 点击"开始解读"调用 AI 接口
- [ ] 右侧展示：综合解读/健康建议/注意事项
- [ ] 一键复制结果内容

---

## 六、已知限制

1. Linting: Vue 文件写入时无语法检查，建议后续手动 review
2. 单元测试: 未执行（tester 未介入）
3. 权限控制: 所有 health 模块路由目前对所有角色开放（待 RBAC 确认）
4. 后端联调: 依赖后端 `/api/v1/health/*` 接口就绪

---

## 七、附件

- 路由定义: `frontend/packages/web/src/router/routes/modules/health.ts`
- API 模块: `frontend/packages/lib-shared/api/modules/health.ts`
- API 索引: `frontend/packages/web/src/api/modules/index.ts`
