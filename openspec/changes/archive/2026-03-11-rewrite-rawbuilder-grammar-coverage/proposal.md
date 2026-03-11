## 为什么

`cfir/raw-cfir/psi2cfir/testData/rawBuilder` 当前用例分布不均、命名与分层不统一，无法直接回答“哪些仓颉语法已覆盖、哪些未覆盖”。随着 `PsiRawCfirBuilder` 持续演进，现有测试组织方式已成为补齐语法转换能力的主要阻力。

## 变更内容

- 重新规划 `rawBuilder` 测试目录，按“声明语法 / 类型语法 / 表达式语法 / 语句与控制流 / 异常与恢复 / 仓颉特性”分层，统一命名与文件布局。
- 重写并扩展 `.cj` 输入和对应 golden 输出（`.txt` / `.lazyBodies.txt`），覆盖仓颉语法到 Raw CFIR 的核心与边界转换。
- 增加语法覆盖矩阵与缺口检查机制，要求新增语法节点必须同步新增或更新 rawBuilder 用例。
- 保持现有 golden 驱动测试框架与生成器机制，避免引入不必要的测试基础设施改造。

## 功能 (Capabilities)

### 新增功能

- `rawbuilder-grammar-coverage-tests`: 为 `psi2cfir` 建立按语法域组织的全覆盖测试资产与覆盖矩阵，确保语法节点到 Raw CFIR 转换具备可追踪验证。

### 修改功能

- `raw-cfir-implementation`: 明确 rawBuilder 测试作为 Raw CFIR 构建阶段的规范性入口，要求目录结构、命名规则、覆盖门禁与新增语法联动策略。

## 影响

- 代码与数据：
- `cfir/raw-cfir/psi2cfir/testData/rawBuilder/**`
- `cfir/raw-cfir/psi2cfir/tests-gen/**`（若目录重构触发生成结果更新）
- `cfir/raw-cfir/psi2cfir/test/**`（若需要补充覆盖矩阵校验逻辑）
- 文档与规范：
- `openspec/specs/raw-cfir-implementation/spec.md`（增量规范）
- `openspec/changes/rewrite-rawbuilder-grammar-coverage/specs/**`
- 流程影响：
- 影响 `:cfir:raw-cfir:psi2cfir:test` 与 golden 更新流程，需在迁移期间保持可回归和可逐步切换。
