# 全项目测试约定（Testing Conventions）

本约定适用于仓库内所有一方模块（`settings.gradle.kts` 中包含的模块）。
目标：对齐 Kotlin K2 项目的测试组织方式，同时控制维护成本。

## 1. 测试分类（必须先分类再实现）

### A. 框架型测试（必须接入 test-infrastructure）

满足任一条件即属于框架型测试：
- 使用 `testData` 文件驱动（如 `*.cj` 输入）
- 需要 golden 对比（如 `*.txt`、`*.diagnostics.txt`）
- 需要 directive / 多模块测试结构 / facade-handler 流水线
- 需要 all-files-present 类漏测防护

要求：
- 基于 `:tests:test-infrastructure`（`TestFacade`、`AnalysisHandler`、`AbstractCangjieCompilerTest`）
- `testData` 与测试代码按模块归属放置
- 支持 `-Dupdate.test.data=true` 的显式 golden 更新

### B. 单元型测试（允许直接 JUnit）

满足全部条件可使用直接 JUnit（JUnit4/JUnit5）：
- 不依赖 `testData` 与 golden 文件
- 仅验证纯 API 行为、数据结构、局部算法
- 构造成本低、断言稳定、无文件驱动需求

要求：
- 测试应短小、无跨模块耦合
- 优先覆盖边界条件与回归点

## 2. Generated 测试类使用规则

仅在以下场景使用 `Generated`（自动扫描 `testData`）：
- `testData` 数量较多或增长快
- 目录层级复杂，容易漏加测试入口
- 已存在稳定生成器与任务接线（例如 tests-gen + `testGenerator(...)`）

不建议为小规模、稳定集合强制引入 `Generated`，避免过度工程化。

## 3. 目录与命名约定

- 源集目录遵循 `projectDefault()`：
  - `main` -> `src` / `resources`
  - `test` -> `test` / `tests` / `testResources`
  - `testFixtures` -> `testFixtures` / `testFixturesResources`
- 测试类名以 `Test` / `TestCase` 结尾
- golden 文件与输入文件同目录、同名不同扩展

## 4. 新增测试的决策顺序（评审基线）

新增测试时按顺序决策：
1. 是否文件驱动（`testData`）？
2. 是否需要 golden 比对？
3. 是否需要 framework 流水线（facade/handler）？
4. 是否存在漏测风险（需要 all-files-present / Generated）？

若 1~3 任一为“是”，默认走框架型测试。

## 5. 迁移策略（存量测试）

- 不做“一刀切”全量迁移
- 新增/修改时按本约定就近收敛
- 对高频变更目录优先迁移到框架型测试
- 纯单元测试保持轻量即可

