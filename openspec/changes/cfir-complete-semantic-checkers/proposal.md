## 为什么

当前提案的问题不是“checker 还不够多”这么简单，而是**整个 `CfirDiagnosticsList` 尚未形成一份以诊断定义为唯一基线的覆盖台账**。现有提案按 checker 分组描述工作范围，容易把“诊断分组”误写成“实现职责分组”，导致两个后果：

1. 没有明确承诺必须覆盖 `CfirDiagnosticsList` 中的**全部诊断定义**，因此无法回答“还剩哪些诊断没有归位、覆盖率是多少、由谁负责实现”。
2. 没有把 **resolve 管线职责** 与 **checker 管线职责** 明确切开，容易把本应在 `CallResolution`、`Constraint`、`TypeCheck`、`Unresolved`、`GenericDeep` 深层推断阶段完成的语义，错误地下沉到 checker 层做兜底。

当前项目的真实目标应当是：

- 以 `cfir/checkers/checkers-component-generator/.../CfirDiagnosticsList.kt` 为唯一基线；
- 对其中**每一个诊断定义**建立覆盖归属；
- 对仍未覆盖的诊断，明确标注其责任归属为：
  - 已由现有实现覆盖；
  - 应由 resolve 管线实现；
  - 应由 checker 管线实现；
- 且禁止用 checker 兜底 resolve 漏洞，禁止用“临时兼容检查”掩盖架构边界。

基于当前盘点，未完全落地的诊断中：

- **大部分（约 80-100 个）属于 resolve 管线职责**，集中在 `CallResolution`、`Constraint`、`TypeCheck`、`Unresolved`、`GenericDeep` 的深层推断部分；
- **其余约 80-90 个属于 checker 管线职责**，需要继续在 declaration / expression / type checker 中补齐；
- 另外还有一部分诊断虽然已有实现，但尚未被纳入统一覆盖率说明、责任表和验证闭环。

因此，本变更不再只是“补 checker”，而是升级为：**完成 CFIR 全诊断定义覆盖治理，并在该治理之下分别推进 resolve 与 checker 的剩余实现。**

## 变更内容

### 1. 建立全诊断定义覆盖台账

- 以 `CfirDiagnosticsList.kt` 中的每一个诊断定义为最小跟踪单元，建立完整覆盖台账。
- 台账必须至少包含以下字段：
  - 诊断组
  - 诊断名
  - 当前状态：已覆盖 / 未覆盖 / 部分覆盖 / 语义待确认
  - 责任层：existing / resolve / checker
  - 责任子域：如 `CallResolution`、`Constraint`、`TypeCheck`、`Unresolved`、`GenericDeep-Inference`、`GeneralChecker`、`ExpressionChecker`、`InteropChecker` 等
  - 对应实现入口
  - 对应测试入口
  - 官方 C++ 语义依据
- 首版台账文件固定落在 `openspec/changes/cfir-complete-semantic-checkers/diagnostic-coverage-ledger.md`
- 变更完成的判定标准不再是“若干 checker 分组已实现”，而是**`CfirDiagnosticsList` 中全部诊断定义均已归位且无遗漏**。

### 2. 明确 resolve / checker 责任边界

- 对全部诊断定义执行职责切分，形成覆盖率说明与分层实施计划。
- 明确下列诊断簇主要属于 **resolve 管线职责**，不得退化为 checker 兜底：
  - `CallResolution`
  - `Constraint`
  - `TypeCheck`
  - `Unresolved`
  - `GenericDeep` 中依赖候选选择、约束求解、类型变量收敛的深层推断部分
- 明确下列诊断簇主要属于 **checker 管线职责**，必须在 checker 层补齐：
  - `General`
  - `Function`
  - `Expression`
  - `InheritanceDeep`
  - `ClassStruct`
  - `Property`
  - `ConstDeclaration`
  - `AnnotationExtra`
  - `Inout`
  - `VArrayExtra`
  - `EffectsExtra`
  - `Deprecated`
  - `CommonSpecific`
  - `ExtendExtra`
  - `Spawn`
  - `Interface`
  - `JavaInterop`
  - `JavaMirror`
  - `CJMapping`
  - `ObjCInterop`
  - `ObjCCJMapping`
  - `ForeignName`
  - `IfAvailable`
  - `APILevel`
  - `Hide`
  - `Mock`
  - `Unused`
  - 以及 `DeclarationStatus` 中仍未闭环的 checker 语义

### 3. 将“覆盖全部诊断定义”写成硬性目标

- 本提案要求覆盖 `CfirDiagnosticsList` 中**所有诊断定义**，而不是仅覆盖“当前最容易补的一批”。
- `openspec/changes/cfir-complete-semantic-checkers/specs/` 必须对全部诊断组建立正式需求，不允许 proposal / tasks 已经要求，但 specs 中没有对应约束。
- 不允许再保留“批次外诊断”“暂不处理的子项”“后续再说的分支”而不进入台账。
- 即使某个诊断暂时不能立刻实现，也必须：
  - 写入覆盖台账；
  - 明确属于 resolve 还是 checker；
  - 明确阻塞点；
  - 明确后续子任务；
  - 明确验证方式。

### 4. 扩展任务拆分粒度

- 任务拆分必须从“按 checker 分组”升级为“按诊断簇 + 实现责任 + 验证入口”拆分。
- 所有子任务必须覆盖到：
  - 全量诊断盘点；
  - 全量职责归类；
  - resolve 剩余诊断补齐；
  - checker 剩余诊断补齐；
  - 对应测试补齐；
  - 覆盖率收敛与对齐验证。

## 功能 (Capabilities)

### 新增功能

- `diagnostic-coverage-governance`: 建立以 `CfirDiagnosticsList` 为基线的全诊断定义覆盖台账、覆盖率说明与实现责任分层规则，要求所有诊断都必须被归位到 existing / resolve / checker 三类之一。
- `resolve-diagnostics-completion`: 补齐所有属于 resolve 管线职责的剩余诊断，重点覆盖 `CallResolution`、`Constraint`、`TypeCheck`、`Unresolved`、`GenericDeep` 深层推断部分。
- `checker-diagnostics-completion`: 补齐所有属于 checker 管线职责的剩余诊断，并在 declaration / expression / type checker 体系内按架构边界完成注册与验证。

### 修改功能

- `general-semantics-checker`
- `function-semantics-checker`
- `expression-semantics-checker`
- `generic-deep-checker`
- `inheritance-deep-checker`
- `class-struct-semantics-checker`
- `property-semantics-checker`
- `const-declaration-checker`
- `annotation-extra-checker`
- `inout-semantics-checker`
- `varray-extra-checker`
- `effects-extra-checker`
- `deprecated-semantics-checker`
- `common-specific-checker`
- `extend-extra-checker`
- `spawn-semantics-checker`
- `interface-semantics-checker`
- `java-interop-checker`
- `objc-interop-checker`
- `foreign-name-checker`
- `if-available-api-level-hide-checker`
- `mock-semantics-checker`
- `declaration-status-extra-checker`

以上能力的含义从“补某组 checker”调整为：**在全诊断定义覆盖治理约束下，补齐这些分组中属于 checker 的全部剩余诊断**。

## 影响

- **诊断定义基线**：`cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt`
- **checker 实现与注册**：
  - `cfir/checkers/src/.../checkers/declaration/`
  - `cfir/checkers/src/.../checkers/expression/`
  - `cfir/checkers/src/.../checkers/type/`
  - `CommonDeclarationCheckers.kt`
  - `CommonExpressionCheckers.kt`
  - `CommonTypeCheckers.kt`
- **resolve 实现入口**：`cfir` 中承担 `CallResolution`、`Constraint`、`TypeCheck`、`Unresolved`、`GenericDeep` 推断职责的相关模块与管线
- **测试**：
  - `cfir/analysis-tests/`
  - `cfir/analysis-tests/testData/diagnostics2/`
  - 以及针对 resolve / checker 的定向测试入口
- **外部对齐依据**：`external/cangjie_compiler/src/Sema/` 下对应的 C++ 语义实现

## 完成标准

- `CfirDiagnosticsList` 中**每一个诊断定义**都有唯一、明确、可追踪的责任归属。
- 全量诊断覆盖率说明可回答：
  - 总数是多少；
  - 已覆盖多少；
  - resolve 负责多少；
  - checker 负责多少；
  - 各自剩余多少；
  - 每一项缺口对应哪个子任务。
- 所有属于 resolve 的诊断均在 resolve 管线完成，不使用 checker 兜底。
- 所有属于 checker 的诊断均在 checker 管线完成，并完成注册与测试。
- 不存在未入账、未归类、未分配、未验证的诊断子项。
- 不存在 proposal / design / tasks 已声明，但 `specs/` 未建模的诊断组需求。
