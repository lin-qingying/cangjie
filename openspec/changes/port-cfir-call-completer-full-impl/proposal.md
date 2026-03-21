## 为什么

当前仓颉编译器在调用补全过程（call completion）相关能力上与 Kotlin 编译器 FIR 存在明显实现缺口，导致调用候选补全、约束补全与诊断一致性难以对齐。现在推进完整移植可以尽早建立稳定的 CFIR 调用补全基础设施，降低后续调用解析与类型系统演进的重复成本。

## 变更内容

- 将 Kotlin 编译器 `FirCallCompleter` 及其完整依赖链路迁移到本项目的 CFIR 体系，采用“复制粘贴级”保真迁移策略，不做最小化裁剪。
- 对迁移过程中涉及的接口、抽象类与辅助实现进行成套引入或重建，确保依赖闭包完整可编译。
- 删除/隔离所有 K1 接入点（例如 `org.jetbrains.kotlin.resolve.calls` 相关声明）；若 CFIR 侧存在等价需求，在 `cfir` 命名空间内重建对应实现。
- 对本项目中已存在但不完整或与上游偏离的相关声明进行对照修订，确保与迁移目标一致。
- 统一命名前缀为 `cfir`（如 `Cfir*`）并保持模块边界清晰。
- 明确本阶段**不**将 `FirCallCompleter` 接入 `CFirExpressionsResolveTransformer` 执行路径。

## 强制约束

- 禁止“自行实现”或“基于理解重写”调用补全逻辑；实现来源必须是 Kotlin 编译器对应源码的完整迁移。
- 必须完整迁移以下要素，不得按需删减：方法、接口、抽象定义、具体实现类、类型定义、依赖声明（含导入依赖与模块依赖）。
- 仅允许进行 CFIR 适配性变更：包名/前缀映射、K1 入口剔除与 `cfir` 内等价重建、必要的编译环境对齐；禁止语义性简化。
- 每个迁移项必须可追溯到上游来源（源文件与符号映射），并在迁移清单中记录“已迁移/已替换/不适用及理由”。

## 功能 (Capabilities)

### 新增功能
- `cfir-call-completer-port`: 提供完整的 CFIR 调用补全器与依赖基础设施，实现与 Kotlin FIR `FirCallCompleter` 语义等价的可用实现（不含 transformer 接线）。
- `cfir-call-completion-dependency-closure`: 提供调用补全所需接口、抽象层与支持组件的完整依赖闭包迁移/重建能力，并移除 K1 入口依赖。

### 修改功能
- 无。

## 影响

- 受影响模块：`cfir:cfir-common`、`cfir:cfir-cones`、`cfir:cfir-tree`、`cfir:raw-cfir:*`、`analysis:analysis-api-cfir`（按实际依赖落点调整）。
- 受影响代码类型：调用解析相关接口/抽象、候选补全过程、诊断与上下文传递结构。
- API 影响：新增或调整 CFIR 内部 SPI；要求维持接口优先设计，不向跨模块泄露实现细节。
- 兼容性：不引入 K1 包依赖；不改动 `CFirExpressionsResolveTransformer` 的现有接线行为。

## 验收标准

- 存在覆盖完整依赖闭包的迁移追溯矩阵，且无未解释缺口。
- 代码中不存在以“自研最小实现”替代上游迁移的调用补全核心逻辑。
- 调用补全相关实现不再依赖 `org.jetbrains.kotlin.resolve.calls`，且等价能力已在 `cfir` 命名空间落地。
