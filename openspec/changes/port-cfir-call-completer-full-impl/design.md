## 上下文

本变更目标是在仓颉编译器中移植 Kotlin K2/FIR `FirCallCompleter` 及其依赖实现，且保持"实现闭包完整"而非最小可运行版本。当前项目已有部分 CFIR 调用解析声明，但与上游行为和分层关系存在偏差或缺失。  
同时，仓库约束要求接口优先、模块边界清晰、避免泄露实现细节；明确要求删除所有 K1（`org.jetbrains.kotlin.resolve.calls` 及 `resolution.common` 整个约束层）相关接入，并统一使用 `cfir` 前缀。  
本阶段只交付补全器及依赖基础设施，不修改 `CFirExpressionsResolveTransformer` 的调用路径接线。

## 目标 / 非目标

**目标：**
- 在 CFIR 体系中提供语义等价的 `CfirCallCompleter` 及其直接/间接依赖实现，确保依赖闭包完整，且不含任何 K1 约束体系依赖。
- 对现有不完整或不准确的相关接口/抽象进行对照修正，并保持 `Cfir*` 命名体系一致性。
- 彻底移除所有 K1 约束体系耦合（含 `ConstraintStorage`、`ConstraintSystemCompleter`、`ConstraintInjector` 等 `resolution.common` 全部约束组件）；调用补全所需约束能力以 K2 `FirInferenceSession` / `ConeInferenceContext` 体系在 `cfir` 命名空间重写。
- 形成可独立构建与可测试的调用补全基础设施，为后续接线预留清晰扩展点。
- 形成"上游符号 -> 本仓符号"的逐项追溯矩阵，覆盖方法、接口、抽象、具体类与依赖声明；K1 调用点改写须单独标注。

**非目标：**
- 本阶段不将 `CfirCallCompleter` 接入 `CFirExpressionsResolveTransformer`。
- 不在本阶段重构无关调用解析流程或做性能优化专项。
- 不引入与调用补全无关的新语言特性或诊断策略变更。
- 不迁移任何 `compiler/resolution.common` 路径下的 K1 文件。

## 决策

### 决策 1：采用"上游 K2/FIR 结构保真迁移 + CFIR 命名映射"策略
- 选择：尽量保持 Kotlin K2/FIR 原始结构与行为语义，仅做包名、类型名、依赖入口的 CFIR 化映射；K2/FIR 源码中残留的 K1 调用点逐一识别并改写为 K2 等价操作。
- 理由：该策略可减少行为漂移，降低后续与上游对照维护成本；同时确保约束体系完全对齐 K2。
- 备选方案：
  - 方案 A：按当前仓库抽象重新设计最小实现。  
    放弃原因：与"禁止最小实现、复制粘贴级"的约束冲突，且易引入语义缺口。
  - 方案 B：仅迁移核心类，依赖按需补齐。  
    放弃原因：无法保证依赖闭包完整，后续整合风险高。

### 决策 2：按"K2 推断会话层 -> 候选模型层 -> 具体实现层"顺序迁移
- 选择：先就位 K2 `FirInferenceSession` / `ConeInferenceContext` 体系（替代 K1 `ConstraintStorage` 的语义基础），再迁移抽象基类与候选结构，最后落地具体补全器与协作组件。
- 理由：K2 推断会话层是批次二、三的编译前置；符合项目接口优先架构，减少循环依赖与临时适配层。
- 备选方案：
  - 方案 A：先迁移具体实现后反推接口。  
    放弃原因：会放大返工面，易污染模块边界。

### 决策 3：K1 约束体系"零容忍"，以 K2 FirInferenceSession 体系替代
- 选择：`ConstraintStorage` 及整个 `resolution.common` 约束组件层（`ConstraintInjector`、`ConstraintIncorporator`、`ConstraintSystemCompleter`、`ResultTypeResolver`、`VariableFixationFinder` 等）全部属于 K1 旧设施，**禁止迁移，禁止引用**。K2/FIR 调用补全所需约束能力以 K2 `FirInferenceSession` / `ConeInferenceContext` 体系在 CFIR 命名空间重写。
- 理由：`ConstraintStorage` 是 K1 体系的核心模型，其语义载体在 K2 已由 `FirInferenceSession` 体系承接；将 K1 模型引入 CFIR 会造成架构层次混乱，引入不可控的 K1 传递依赖链。
- 备选方案：
  - 方案 A（原决策 6，已废弃）：完整迁移 `ConstraintStorage` 并替换简化版 `CfirConstraintStorage`。  
    放弃原因：`ConstraintStorage` 属于 K1 体系，迁移本身即引入 K1 依赖，与"K1 零容忍"约束直接冲突。
  - 方案 B：保留 `CfirConstraintStorage` 简化版并持续扩展。  
    放弃原因：该实现为 K1 概念在 CFIR 的投影，语义覆盖不足，且形成持续的 K1 架构污染。
  - 方案 C：把 `resolve.calls.inference` 全部视为 K1 旧设施并删除。  
    放弃原因：`compiler/fir/resolve/inference/` 路径下的文件（`FirInferenceSession`、`FirPCLAInferenceSession` 等）属于 K2/FIR 层，不在删除范围；需要精确区分路径边界。

### 决策 4：显式保留"未接线"边界
- 选择：为后续 transformer 接线预留入口，但在本变更中不触发执行路径变更。
- 理由：降低一次性改动风险，便于先完成基础设施正确性验证。
- 备选方案：
  - 方案 A：同步完成接线。  
    放弃原因：测试和回归面显著扩大，不符合当前变更范围。

### 决策 5：禁止自主实现，采用"源码对位迁移 + K1 调用改写"验收
- 选择：对 `FirCallCompleter` 及其 K2/FIR 依赖采用源码级对位迁移；K2/FIR 源码中对 K1 `ConstraintStorage` 等 API 的残留调用点，识别语义后改写为 K2 推断会话等价调用，并在追溯矩阵以 `K1调用改写` 状态登记。
- 理由：确保语义保真与长期可维护性；K1 调用点改写有明确语义映射，不构成"自主实现"。
- 备选方案：
  - 方案 A：原样迁移 K2/FIR 源码（含 K1 调用点）。  
    放弃原因：会直接引入 K1 依赖，违反"K1 零容忍"约束。

## 风险 / 权衡

- [风险] K2/FIR 源码中 K1 调用点分布广泛，识别和改写工作量被低估  
  -> 缓解措施：在追溯矩阵中为每个源文件单独列出所有 K1 调用点；批次间编译门禁强制验证无 K1 路径引用。
- [风险] K1 调用点改写引入语义偏差（改写后行为与 K2 原始行为不一致）  
  -> 缓解措施：以 K2 `FirInferenceSession` API 文档与上游行为为参照，每个改写点须在追溯矩阵说明语义映射；增加单元测试验证关键约束操作路径。
- [风险] 上游依赖链过长导致遗漏文件或接口不匹配  
  -> 缓解措施：按批次迁移并在每批后执行模块级编译检查，建立迁移清单逐项勾选。
- [风险] 现有 CFIR 声明与迁移目标冲突，产生破坏性改名或签名调整  
  -> 缓解措施：先做对照表，采用"旧声明短期保留 + 调用点分批切换"策略（仅限 CFIR 内部）。
- [风险] 无接线状态下难以验证端到端行为  
  -> 缓解措施：增加结构级与单元级测试（API 合同、候选补全规则、推断会话操作、诊断产物）并使用黄金数据对齐。
- [风险] 团队在迁移中不自觉引入 K1 依赖（以"ConstraintStorage 语义更直接"为由）  
  -> 缓解措施：在追溯矩阵门禁中强制检查：不得存在将 `resolution.common` 符号标注为"已迁移"的条目；批次间静态扫描验证。

## 迁移计划

1. 梳理 K2/FIR `FirCallCompleter` 依赖闭包，区分"K2/FIR 可迁移"与"K1 禁止引入"，形成"源符号 -> CFIR 目标符号"映射清单。
2. 识别 K2/FIR 源码中所有 K1 `ConstraintStorage` 残留调用点，逐一确认 K2 推断会话等价操作，登记改写清单。
3. 建立迁移门禁：未在映射清单登记的新增实现不得进入主分支；K1 路径引用在批次间静态扫描中零容忍。
4. 批次一：就位 K2 推断会话层（`FirInferenceSession` 体系），同步删除所有 K1 风格自研实现（`CfirConstraintStorage`、`CfirResultTypeResolver`、`CfirTypeVariable`、自研 `InferenceLogger`）。
5. 批次二：迁移候选模型与调用解析接口层，改写 `CandidateFactory` 中的 K1 调用点。
6. 批次三：迁移 Tower / Stages / Overloads / calls 根目录及补全器层，完成所有 K1 调用点改写。
7. 补充/更新测试，验证编译通过、零 K1 引用、关键语义一致。
8. 保持 `CFirExpressionsResolveTransformer` 无接线变更，输出后续接线待办项。

回滚策略：
- 以提交粒度分阶段推进；若出现范围外回归，按批次回滚最近迁移提交，保留已验证的推断会话层改动。

## 开放问题

- 当前仓库中哪些 `Cfir*` 声明已被下游模块隐式依赖，删除 `CfirConstraintStorage` / `CfirTypeVariable` 是否需要兼容过渡期？（建议在批次一前完成摸排）
- 是否需要在 `analysis:analysis-api-cfir` 同步增加仅用于补全器验证的测试夹具？

## 接线核查（6.3）

- 核查对象：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt`
- 核查结果：未发现 `CfirCallCompleter` 的创建、注入或 `completeCall(...)` 调用；当前表达式解析流程仍经由 `callResolver` 与现有阶段化调用解析路径执行。
- 结论：本变更当前实现满足"本阶段不接入 `CFirExpressionsResolveTransformer`"约束。
- 后续待办：在后续变更中引入受控接线点（建议在 `resolveCallWithPhase3` 成功候选后、结果写回前增加可开关的 completion hook），并配套回归测试验证行为差异。