## 1. 渲染器架构重构

- [x] 1.1 在 `cfir/cfir-tree` 中定义 `CfirRenderer` 的组件化接口（至少拆分 declaration/type/reference 渲染职责）并提供默认实现。
- [x] 1.2 重构现有 `CfirRenderer` 主体为“组件编排 + 访问流程控制”，确保输出行为由可替换组件驱动。
- [x] 1.3 提供统一 `withXxx()` 命名的渲染 profile 工厂（至少包含 `withGoldenCompat`、`withDebug`、`withReadability`）并约定其稳定语义。
- [x] 1.4 为 resolve phase 渲染仅预留组件与调用扩展点（本次不输出新增 phase 文本内容）。
- [x] 1.5 落地实例渲染 API（如 `renderElementAsString`）并引入 `CfirPrinter` 作为缩进/换行抽象。

## 2. 兼容性与调用方迁移

- [x] 2.1 保留并实现 `CfirRenderer.render(element)` 兼容入口，使其显式委托到 `golden-compat` profile。
- [x] 2.2 迁移 `AbstractRawCfirBuilderTestCase.dumpCfirFile()` 与 `AbstractRawCfirBuilderSourceElementMappingTestCase.doRawCfirTest()` 到显式 `withGoldenCompat` profile 入口，避免“默认实现即测试专用”语义耦合。
- [x] 2.3 核对 `rawBuilder`、`sourceElementMapping`、`lazyBodies` 三类测试路径在迁移后仍走统一 golden 兼容输出。

## 3. 测试与验证

- [x] 3.1 为 `golden-compat` profile 增加回归验证，确保现有 `.txt` 与 `.lazyBodies.txt` 基线稳定。
- [x] 3.2 新增非 golden 场景测试，验证 `debug` 与 `readability` profile 对同一 CFIR 元素可产生策略化输出。
- [x] 3.3 在 `:cfir:cfir-tree` 新增 renderer API 单测并运行 `:cfir:cfir-tree:test`，确认公开 API 行为稳定。
- [x] 3.4 运行 `:cfir:raw-cfir:psi2cfir:test` 并记录结果，确认无编译/类型错误与关键路径回归。
- [x] 3.5 验证本次不引入 `CfirRendererOptions` 仍能覆盖既定 profile 需求，并记录后续引入条件。

## 4. 文档与规范对齐

- [x] 4.1 更新 `openspec/specs/raw-cfir-implementation/spec.md` 对应实现说明，明确渲染器是“多场景复用能力”且保留 golden 兼容。
- [x] 4.2 在 `README.md` 或相关模块文档中补充 renderer profile 用法与适用场景，避免后续仅按 golden 工具理解。
