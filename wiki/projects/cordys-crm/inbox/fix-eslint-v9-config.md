# ESLint v9 配置修复

**时间**: 2026-05-18 18:30
**执行者**: frontend-dev
**根因**: ESLint 9.39.4 需要 flat config（eslint.config.js/cjs），项目只有 .eslintrc-auto-import.json（v8 格式），导致 "Failed to load config eslint-plugin-vue" 错误覆盖所有页面

**修复**: 复制 `packages/web/eslint.config.cjs` → 根目录 `eslint.config.cjs`

**验证**:
```bash
npx eslint --version
# v9.39.4 ✓

npx eslint packages/web/src/views/health/components/healthArchiveTable.vue
# 正常 lint 输出，无 "Failed to load" 错误 ✓

curl http://127.0.0.1:5173/
# 200 OK ✓
```

**注意**: 剩余 lint errors（import sort、no-use-before-define）属于正常代码风格检查，不阻塞页面运行。
