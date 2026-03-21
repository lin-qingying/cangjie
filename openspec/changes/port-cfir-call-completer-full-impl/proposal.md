## 为什么

当前仓颉编译器在调用补全过程（call completion）相关能力上与 Kotlin 编译器 FIR 存在明显实现缺口，导致调用候选补全、约束补全与诊断一致性难以对齐。现在推进完整移植可以尽早建立稳定的 CFIR 调用补全基础设施，降低后续调用解析与类型系统演进的重复成本。

## 变更内容

- 将 Kotlin 编译器 `FirCallCompleter` 及其完整 K2/FIR 依赖链路迁移到本项目的 CFIR 体系，采用"复制粘贴级"保真迁移策略，不做最小化裁剪。
- 对迁移过程中涉及的接口、抽象类与辅助实现进行成套引入或重建，确保依赖闭包完整可编译。
- **彻底移除所有 K1 旧设施依赖**：`ConstraintStorage` 及其整个组件层（`ConstraintInjector`、`ConstraintIncorporator` 等）均位于 `compiler/resolution.common` K1 体系，**不得迁移，不得引用**。调用补全所需的约束能力以 K2/FIR 的 `ConeInferenceContext` + `FirInferenceSession` 体系为基础，在 CFIR 命名空间重写实现。
- 对本项目中已存在但不完整或与 K2 上游偏离的相关声明进行对照修订，确保与迁移目标一致。
- 统一命名前缀为 `cfir`（如 `Cfir*`）并保持模块边界清晰。
- 明确本阶段**不**将 `FirCallCompleter` 接入 `CFirExpressionsResolveTransformer` 执行路径。

## 上游基线

- **仓库**：`external/kotlin`（本仓库内置镜像）
- **分支/版本**：与仓库现有 `external/kotlin` 版本一致（对应 JetBrains/kotlin master，copyright 年份 2026）
- **可迁移参考路径根（K2/FIR 层）**：`external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/`
- **禁止迁移路径根（K1 层）**：`external/kotlin/compiler/resolution/src/org/jetbrains/kotlin/resolve/calls/inference/`（含 `ConstraintStorage`、`ConstraintSystemCompleter` 等整个 `resolution.common` 约束体系）

所有迁移追溯必须精确到上游文件路径 + 符号名称，禁止以"与上游语义类似"代替精确映射。

## 强制约束

- 禁止"自行实现"或"基于理解重写"调用补全逻辑；实现来源必须是 Kotlin 编译器 K2/FIR 对应源码的完整迁移。
- **禁止引入任何 `compiler/resolution.common` / `org.jetbrains.kotlin.resolve.calls.inference` 路径下的 K1 声明**，包括但不限于：`ConstraintStorage`、`MutableConstraintStorage`、`ConstraintSystemCompleter`、`ConstraintInjector`、`ConstraintIncorporator`、`NewConstraintSystem`、`NewConstraintSystemImpl`、`ConstraintSystemBuilder` 及其全部组件层文件。
- 必须完整迁移以下要素，不得按需删减：方法、接口、抽象定义、具体实现类、类型定义、依赖声明（含导入依赖与模块依赖）。
- 仅允许进行 CFIR 适配性变更（详见下方"适配性变更判定规则"）；禁止语义性简化。
- 对 K2/FIR 调用补全中涉及约束操作的调用点（如 `asReadOnlyStorage`、`currentStorage`、`buildCurrentSubstitutor` 等），以 K2 的 `ConeInferenceContext` / `FirInferenceSession` 体系提供的等价能力替换，在 CFIR 命名空间重写。
- 每个迁移项必须可追溯到上游 K2/FIR 来源（源文件与符号映射），并在迁移清单中记录"已迁移/已替换/不适用及理由"。

### 适配性变更判定规则

以下变更属于"允许的 CFIR 适配性变更"，无需额外评审：

| 变更类型 | 允许示例 | 禁止示例 |
|---|---|---|
| 包名/前缀映射 | `org.jetbrains.kotlin.fir` → `org.cangnova.cangjie.cfir`；`Fir*` → `Cfir*` | 重命名与包路径无关的类型语义 |
| K1 入口剔除 | 删除对 `resolution.common` 的直接导入，改写为 K2 `ConeInferenceContext` / `FirInferenceSession` 的 CFIR 等价实现 | 将 K1 接口以"桥接层"形式保留 |
| 编译环境对齐 | 用仓颉等效基础类型替换 Kotlin stdlib 中本项目已有对应类型（须在追溯矩阵标注） | 用自研类型替换 K2 FIR 核心模型（如 `ConeInferenceContext`） |
| 语言语法适配 | Kotlin 特有语法改写为仓颉等效语法，不改变逻辑结构 | 合并/拆分方法、改变参数语义、省略分支 |

当出现**上游无等效项但仓颉环境缺少对应类型**的情况时：
1. 在追溯矩阵中标注"环境缺失，需在 CFIR 重实现"并注明理由。
2. 提交至架构评审，明确"重实现"边界（新实现必须有 K2/FIR 语义来源）。
3. 不得以"环境限制"为由绕过追溯矩阵登记。

## 功能 (Capabilities)

### 新增功能
- `cfir-call-completer-port`: 提供完整的 CFIR 调用补全器与依赖基础设施，实现与 Kotlin K2/FIR `FirCallCompleter` 语义等价的可用实现（不含 transformer 接线，不含任何 K1 约束体系依赖）。
- `cfir-call-completion-dependency-closure`: 提供调用补全所需接口、抽象层与支持组件的完整依赖闭包迁移/重建能力，并彻底移除 K1 入口依赖（含 `resolution.common` 整个约束层）。

### 修改功能
- 无。

## 影响

- 受影响模块：`cfir:cfir-common`、`cfir:cfir-cones`、`cfir:cfir-tree`、`cfir:raw-cfir:*`、`analysis:analysis-api-cfir`（按实际依赖落点调整）。
- 受影响代码类型：调用解析相关接口/抽象、候选补全过程、诊断与上下文传递结构。
- API 影响：新增或调整 CFIR 内部 SPI；要求维持接口优先设计，不向跨模块泄露实现细节。
- 兼容性：不引入任何 K1 包依赖；不改动 `CFirExpressionsResolveTransformer` 的现有接线行为。

## 验收标准

- 存在覆盖完整依赖闭包的迁移追溯矩阵，且无未解释缺口。
- 代码中不存在以"自研最小实现"替代上游迁移的调用补全核心逻辑。
- **静态扫描确认**：调用补全链路中不存在 `org.jetbrains.kotlin.resolve.calls.inference`（K1 路径）下任何符号的直接引用。
- 受影响模块可独立编译通过。
- `CFirExpressionsResolveTransformer` 无新增 `CfirCallCompleter` 接线路径（由 diff 检查验证）。
- 追溯矩阵中每条记录均包含"上游文件路径 + 符号名 + CFIR 目标符号 + 状态"四要素；无"状态=空"或"状态=待处理"的条目；不存在将 K1 `resolution.common` 符号标注为"已迁移"的条目。

## 迁移文件与声明清单（逐项）

以下清单以 Kotlin 上游 K2/FIR 层（`compiler/fir/resolve`）中 `inference` 与 `calls` 目录为准，作为本变更"必须迁移"的文件级基线。K1 层（`compiler/resolution.common`）文件**一律不在迁移范围内**，不得出现在追溯矩阵的"已迁移"状态中。

### A. `resolve/inference`（必须全量迁移，K2/FIR 层）

- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirCallCompleter.kt`  
  关键声明：`class FirCallCompleter`、`fun Candidate.computeCompletionMode` 调用路径依赖  
  注意：文件中对 `ConstraintStorage` 等 K1 API 的调用点须改写为 K2 `ConeInferenceContext` / `FirInferenceSession` 等价调用，在追溯矩阵中以 `K1调用改写` 状态标注
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/PostponedArgumentsAnalyzer.kt`  
  关键声明：`data class ReturnArgumentsAnalysisResult`、`interface LambdaAnalyzer`、`class PostponedArgumentsAnalyzer`、`fun ConeLambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda`
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/CompletionModeCalculator.kt`  
  关键声明：`fun Candidate.computeCompletionMode`
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/InferenceComponents.kt`  
  关键声明：`class InferenceComponents`
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/InferenceUtils.kt`  
  关键声明：`fun extractLambdaInfoFromFunctionType`（及同文件辅助函数）
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/ConeConstraintSystemUtilContext.kt`  
  关键声明：`object ConeConstraintSystemUtilContext`
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt`  
  关键声明：`abstract class FirInferenceLogger`（及其记录结构）
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceSession.kt`  
  关键声明：`class FirInferenceSession`（K2 约束操作的核心载体，替代 K1 `ConstraintStorage` 的语义入口）
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirPCLAInferenceSession.kt`  
  关键声明：`class FirPCLAInferenceSession`、`class FirTypeVariablesAfterPCLATransformer`
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirDelegatedPropertyInferenceSession.kt`  
  关键声明：`class FirDelegatedPropertyInferenceSession`、`fun FirElement.isAnyOfDelegateOperators`
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/CollectionLiteralBoundsCollector.kt`  
  关键声明：`sealed class CollectionLiteralBounds`、`class CollectionLiteralBoundsCollector`

**从原清单移除（K1，禁止迁移）：**
- ~~`ConstraintSystemCompleter.kt`~~：位于 K1 `resolution.common` 路径，不得迁移。`FirCallCompleter` 对其的依赖须在 CFIR 内以 `CfirInferenceSession` 体系重写。

### B. `resolve/calls` 根目录（必须全量迁移，K2/FIR 层）

- `.../calls/ConeResolutionAtoms.kt`：`sealed class ConeResolutionAtom` 及全部 postponed atom 子类
- `.../calls/FirCallResolver.kt`：`class FirCallResolver`、`data class OverloadCandidate`、`class AllCandidatesCollector`
- `.../calls/ResolutionContext.kt`：`class ResolutionContext`
- `.../calls/ArgumentUtils.kt`：`fun FirExpression.getExpectedType` 等参数期望类型工具
- `.../calls/CallableReferenceAdaptation.kt`：`sealed class CallableReferenceConversionStrategy`
- `.../calls/QualifierReceiver.kt`：`ClassQualifierReceiver`、`PackageQualifierReceiver`、`createQualifierReceiver`
- `.../calls/ConstructorProcessing.kt`：构造调用处理函数族
- `.../calls/SuperCalls.kt`：`findTypesForSuperCandidates` 等 super 调用处理
- `.../calls/VisibilityUtils.kt`：`isVisible` 等可见性工具

### C. `resolve/calls/candidate`（必须全量迁移）

- `CallInfo.kt`：`enum class ImplicitInvokeMode`、`class CallableReferenceInfo`
- `CallKind.kt`：`sealed class CallKind`、`class ResolutionSequenceBuilder`、`buildCallKindWithCustomResolutionSequence`
- `Candidate.kt`：`class Candidate`
- `CandidateCollector.kt`：候选收集器实现
- `CandidateFactory.kt`：`class CandidateFactory`、`addSubsystemFromAtom`（注意：原 K1 `setBaseSystem`/`addOtherSystem` 等 ConstraintStorage 调用点须改写为 K2 推断会话操作，标注 `K1调用改写`）
- `CandidateTraversal.kt`：`processCandidatesAndPostponedAtoms`、`processPostponedAtoms`
- `CheckerSink.kt`：`class CheckerSinkImpl`
- `FirNamedReferenceWithCandidate.kt`：`class FirErrorReferenceWithCandidate`
- `errorCandidateUtils.kt`：`createErrorReferenceWithErrorCandidate`、`createErrorReferenceWithExistingCandidate`、`createErrorCandidate`、`fullyProcessCandidate`

### D. `resolve/calls/stages`（必须全量迁移）

- `ResolutionStages.kt`：所有标准 `ResolutionStage` 对象（接收者/上下文参数/隐式遮蔽等）
- `ResolutionStageRunner.kt`：`class ResolutionStageRunner`
- `TypeArgumentMapping.kt`：`sealed class TypeArgumentMapping`
- `FirArgumentsToParametersMapper.kt`：`data class ArgumentMapping`、`mapArguments`
- `CheckArguments.kt`：`object CheckArguments`
- `CheckCallableReferenceExpectedType.kt`：`class FirFakeArgumentForCallableReference` 等
- `CreateFreshTypeVariableSubstitutorStage.kt`：`object CreateFreshTypeVariableSubstitutorStage`
- `CollectTypeVariableUsagesInfo.kt`：`object CollectTypeVariableUsagesInfo`（注意：对 `currentStorage().notFixedTypeVariables` 的读取须改写为 K2 推断上下文等价调用，标注 `K1调用改写`）
- `ArgumentCheckingProcessor.kt`：参数校验处理器

### E. `resolve/calls/tower`（必须全量迁移）

- `FirTowerResolver.kt`：`class FirTowerResolver`
- `TowerGroup.kt`：`sealed class TowerGroupKind`、`class TowerGroup`、`enum class InvokeResolvePriority`
- `TowerResolveManager.kt`：`class TowerResolveManager`
- `TowerLevels.kt`：`enum class ProcessResult` 及 tower level 实现
- `TowerLevelHandler.kt`：`class TowerLevelProcessor`
- `FirTowerResolveTask.kt`：tower 任务模型
- `FirInvokeResolveTowerExtension.kt`：invoke 扩展解析

### F. `resolve/calls/overloads`（必须全量迁移）

- `ConeOverloadConflictResolver.kt`：`class ConeOverloadConflictResolver`、`ConeSimpleConstraintSystemImpl`
- `ConeCallConflictResolver.kt`：冲突解析基类
- `ConeCompositeConflictResolver.kt`：组合冲突解析器
- `ConeEquivalentCallConflictResolver.kt`：等价候选冲突解析
- `ConeIntegerOperatorConflictResolver.kt`：整数字面量操作符冲突解析
- `FirOverloadByLambdaReturnTypeResolver.kt`：按 lambda 返回类型重载解析
- `FirDeclarationOverloadabilityHelperImpl.kt`：声明可重载性辅助实现
- `ReplOverloadCallConflictResolver.kt`：REPL 冲突解析器

## 本项目无上游对应、需删除或替换的声明（逐项）

以下为当前仓库中"非 K2/FIR 结构"的声明，必须在迁移过程中删除或被 K2/FIR 对位实现替换。

- `common/src/org/cangnova/cangjie/resolve/calls/Constraint.kt`：`interface Constraint`  
  处理要求：删除该 K1 风格接口，约束概念统一由 K2 `ConeInferenceContext` / `FirInferenceSession` 体系承接。
- `common/src/org/cangnova/cangjie/resolve/calls/PostponedResolvedAtomMarker.kt`：`PostponedResolvedAtomMarker`、`CollectionLiteralAtomMarker`、`PostponedAtomWithRevisableExpectedType`、`PostponedCallableReferenceMarker`、`LambdaWithTypeVariableAsExpectedTypeMarker`  
  处理要求：用 `cfir.resolve.calls`（对位 `ConeResolutionAtoms.kt`）内声明替换，删除 `common.resolve.calls` 路径暴露。
- `common/src/org/cangnova/cangjie/resolve/calls/ForkPointData.kt`：当前文件无有效声明（仅注释）  
  处理要求：删除空壳文件。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/InferenceLogger.kt`：`CfirConstraintKind`、`CfirInitialConstraint`、`CfirVariableConstraint`、`CfirFixationReadiness`、`CfirSimpleFixationReadiness`、`InferenceLogger`  
  处理要求：替换为上游 `FirInferenceLogger.kt` 对位迁移版本，删除自研日志模型。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirResultTypeResolver.kt`：`class CfirResultTypeResolver`  
  处理要求：删除，结果类型推断回归 K2 `FirInferenceSession` 推断链路。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirTypeVariable.kt`：`data class CfirTypeVariable`  
  处理要求：删除简化版自研结构，类型变量定义从 K2 FIR 推断模型迁移。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirConstraintStorage.kt`（若存在）  
  处理要求：**直接删除，无替代品**。`ConstraintStorage` 是 K1 概念，不应在 CFIR 体系中存在任何形式的对应物（无论简化版还是"完整迁移版"）。约束能力由 K2 `FirInferenceSession` 体系统一提供。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirMapArguments.kt`：`object CfirMapArguments`  
  处理要求：并入 `FirArgumentsToParametersMapper.kt` 对位迁移实现后删除该自定义 stage。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirInferTypeArguments.kt`：`object CfirInferTypeArguments`  
  处理要求：由上游 `CollectTypeVariableUsagesInfo` + `ResolutionStages` 对位实现替换后删除。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/overloads/CfirFlatSignature.kt`：`class CfirFlatSignature`  
  处理要求：若与上游 `ConeOverloadConflictResolver` 路线不一致，删除并改用上游对位签名模型。

说明：上述"删除/替换"是提案约束，不要求在本步骤立即改代码；实现阶段必须按清单逐项落地并在 `tasks` 打勾验证。

## K1/K2 约束体系边界说明（架构决策）

本节为实现阶段提供边界判断的唯一参照，防止 K1 代码以任何形式渗入 CFIR 体系。

### K1 约束体系（禁止迁移，禁止引用，全部标注"不适用"）

位于 `compiler/resolution.common/src/org/jetbrains/kotlin/resolve/calls/inference/` 路径，包括：

- 核心模型：`ConstraintStorage`、`MutableConstraintStorage`、`ConstraintKind`、`Constraint`、`VariableWithConstraints`、`InitialConstraint`
- 组件层：`ConstraintInjector`、`ConstraintIncorporator`、`ConstraintSystemCompleter`、`ResultTypeResolver`、`VariableFixationFinder`、`TypeVariableDependencyInformationProvider`、`PostponedArgumentInputTypesResolver`、`InferenceLogger`（K1 版本）等
- 入口层：`NewConstraintSystem`、`NewConstraintSystemImpl`、`ConstraintSystemBuilder`、`ConstraintSystemMarker`、`ConstraintSystemUtilContext`、`ConstraintSystemCompletionMode`、`TypeVariableDirectionCalculator`（K1 版本）等

这些文件在追溯矩阵中**一律标注状态为 `不适用`**，理由统一填写："K1 体系（resolution.common），由 K2 FirInferenceSession / ConeInferenceContext 体系替代，禁止迁移"。

### K2 约束能力载体（迁移时的语义参照基准）

K2/FIR 的约束操作通过以下 K2 原生接口完成：

- **`ConeInferenceContext`**：K2 类型推断上下文接口，提供约束添加、类型变量创建、substitutor 构建等能力。
- **`FirInferenceSession`**：K2 推断会话，管理推断生命周期、约束传播、PCLA 支持。
- **`FirPCLAInferenceSession`**：PCLA 推断会话。
- **`FirDelegatedPropertyInferenceSession`**：委托属性推断会话。

**K1 调用改写原则**：凡 K2/FIR 源码中调用 `ConstraintStorage` 相关 API（如 `asReadOnlyStorage`、`currentStorage`、`buildCurrentSubstitutor`、`buildAbstractResultingSubstitutor`、`notFixedTypeVariables`）的位置，在 CFIR 迁移时均须识别其语义目的，并改写为对应的 `CfirInferenceSession` / `CfirInferenceContext` 等价调用。此类改写属于**强制 K1 剔除**，不属于"自主实现"，须在追溯矩阵中以 `K1调用改写` 状态标注并说明语义映射。

## 迁移批次与执行顺序

迁移按三个批次执行，批次间设编译验证门禁，批次内顺序依赖须遵守。

### 批次一：K2 推断会话与约束上下文层（所有迁移的编译前置）

这是替代 K1 `ConstraintStorage` 体系的 K2 等价基础设施，必须先于一切调用补全实现就位。

**必须按以下顺序迁移/重建：**

1. `ConeConstraintSystemUtilContext.kt`（K2 约束工具上下文，无其他迁移依赖）
2. `InferenceComponents.kt`（K2 推断组件容器，依赖 1）
3. `FirInferenceSession.kt`（K2 推断会话基础，依赖 1、2）
4. `FirPCLAInferenceSession.kt`（依赖 3）
5. `FirDelegatedPropertyInferenceSession.kt`（依赖 3）
6. `FirInferenceLogger.kt`（依赖 3；同步替换并删除 `cfir/.../InferenceLogger.kt` 自研版本）
7. `InferenceUtils.kt`（K2/FIR 层辅助函数，依赖 3）

**同步删除（批次一完成前执行）：**
- `cfir/.../CfirConstraintStorage.kt`（若存在）：直接删除，无替代品
- `cfir/.../InferenceLogger.kt`（自研版本）：由步骤 6 的迁移版替换
- `cfir/.../CfirResultTypeResolver.kt`：删除，结果推断归入 K2 推断会话链路
- `cfir/.../CfirTypeVariable.kt`：删除，类型变量从 K2 FIR 模型获取

**批次一门禁**：CFIR 推断会话相关模块编译通过；静态扫描确认无 `org.jetbrains.kotlin.resolve.calls.inference`（K1 路径）直接引用。

### 批次二：调用解析接口与候选模型层（依赖批次一）

**必须按以下顺序迁移：**

1. `ConeResolutionAtoms.kt`（postponed atom 模型，无 candidate 依赖）
2. `ResolutionContext.kt`（依赖批次一 `InferenceComponents`）
3. `CallKind.kt`、`CallInfo.kt`（依赖 2）
4. `CheckerSink.kt`、`FirNamedReferenceWithCandidate.kt`、`errorCandidateUtils.kt`（依赖 3）
5. `Candidate.kt`（依赖 1、3、4）
6. `CandidateFactory.kt`（依赖 5；K1 `ConstraintStorage` 调用点改写为 K2 推断会话操作，标注 `K1调用改写`）
7. `CandidateCollector.kt`、`CandidateTraversal.kt`（依赖 5、6）
8. `TypeArgumentMapping.kt`、`ArgumentUtils.kt`（依赖 5）
9. 同步删除 `common/.../PostponedResolvedAtomMarker.kt`，切换调用点到 `ConeResolutionAtoms`
10. 同步删除 `common/.../Constraint.kt`（K1 风格接口）

**批次二门禁**：候选模型相关模块编译通过；无 K1 约束接口残留引用；`CfirFlatSignature.kt` 评估完成并形成处置决定。

### 批次三：具体实现层（依赖批次一、二）

**必须按以下顺序迁移：**

1. Tower 层：`TowerGroup.kt` → `TowerLevels.kt` → `TowerLevelHandler.kt` → `TowerResolveManager.kt` → `FirTowerResolveTask.kt` → `FirInvokeResolveTowerExtension.kt` → `FirTowerResolver.kt`
2. Stages 层：`ResolutionStages.kt` → `CreateFreshTypeVariableSubstitutorStage.kt` → `CollectTypeVariableUsagesInfo.kt`（K1 `currentStorage` 调用点改写，标注 `K1调用改写`）→ `FirArgumentsToParametersMapper.kt` → `CheckArguments.kt` → `CheckCallableReferenceExpectedType.kt` → `ArgumentCheckingProcessor.kt` → `ResolutionStageRunner.kt`  
   同步删除 `CfirMapArguments.kt`、`CfirInferTypeArguments.kt`
3. Overloads 层：`ConeCallConflictResolver.kt` → `ConeOverloadConflictResolver.kt` → `ConeCompositeConflictResolver.kt` → `ConeEquivalentCallConflictResolver.kt` → `ConeIntegerOperatorConflictResolver.kt` → `FirOverloadByLambdaReturnTypeResolver.kt` → `FirDeclarationOverloadabilityHelperImpl.kt` → `ReplOverloadCallConflictResolver.kt`
4. calls 根目录：`QualifierReceiver.kt` → `VisibilityUtils.kt` → `CallableReferenceAdaptation.kt` → `ConstructorProcessing.kt` → `SuperCalls.kt` → `FirCallResolver.kt`
5. 补全器层：`CompletionModeCalculator.kt` → `PostponedArgumentsAnalyzer.kt`（所有 K1 ConstraintStorage 调用点改写，标注 `K1调用改写`）→ `CollectionLiteralBoundsCollector.kt` → `FirCallCompleter.kt`（所有 K1 调用点改写）

**批次三门禁：**
- 受影响所有模块编译通过
- 静态扫描确认零 K1 约束体系引用（`org.jetbrains.kotlin.resolve.calls.inference` 包路径）
- `CFirExpressionsResolveTransformer` diff 检查无新增接线
- 追溯矩阵所有条目状态已填写（无空状态项，无"待处理"项）

## 追溯矩阵格式规范

每个迁移文件对应一份追溯记录表，格式如下：

```
## 追溯记录：<上游文件路径>

| 上游符号 | 上游类型 | CFIR 目标符号 | CFIR 所在文件 | 状态 | 备注 |
|---|---|---|---|---|---|
| FirCallCompleter | class | CfirCallCompleter | cfir/resolve/.../inference/CfirCallCompleter.kt | 已迁移 | 包名映射 + Cfir 前缀 |
| FirCallCompleter.completeCall | fun | CfirCallCompleter.completeCall | 同上 | 已迁移 | — |
| currentStorage() 调用点 | 调用点 | CfirInferenceSession.currentConstraints | cfir/resolve/.../inference/CfirInferenceSession.kt | K1调用改写 | 语义：读取当前约束快照，改写为 K2 推断会话等价调用 |
| ConstraintSystemCompleter | class | — | — | 不适用 | K1 体系（resolution.common），由 K2 FirInferenceSession 替代，禁止迁移 |
```

**字段说明：**

- **上游符号**：上游 K2/FIR 源文件中的符号全名，或 K2/FIR 源码中对 K1 API 的关键调用点描述
- **上游类型**：`class` / `interface` / `abstract class` / `object` / `fun` / `typealias` / `调用点` 等
- **CFIR 目标符号**：迁移后在本项目中的符号名
- **CFIR 所在文件**：相对于仓库根的文件路径
- **状态**：必须为以下之一：
  - `已迁移`：K2/FIR 符号已完整迁移到 CFIR，仅做适配性变更
  - `已替换`：由其他 K2/FIR 等价实现承接（须说明替代符号）
  - `不适用`：K1-only 旧设施或 K2 调用链未使用，已删除（须说明原因）
  - `环境重实现`：仓颉环境缺失，经架构评审按 K2/FIR 语义重实现
  - `K1调用改写`：K2/FIR 源码中对 K1 `ConstraintStorage` 等 API 的调用点，已改写为 K2 推断会话等价调用（须说明语义映射）
  - `待处理`：实现阶段临时标记，**不得在 PR 合并时保留**
- **备注**：说明变更原因或与上游的差异点；`已迁移` 且无差异可填 `—`

**门禁要求**：PR 合并前，矩阵中不得存在"状态=待处理"或"状态=空"的条目；不得存在将 `resolution.common` K1 符号标注为"已迁移"的条目。

## 仓颉语言兼容性冲突处理策略

本节关闭 design.md 中悬而未决的开放问题。

### 冲突类型分类与处理规则

**类型 A：编译环境差异（不构成语义冲突）**

例如：Kotlin 标准库类型在仓颉中有对应等效类型，或语法形式不同但逻辑结构一致。

处理规则：按"适配性变更判定规则"处理，在追溯矩阵标注"环境重实现"，无需评审升级。

**类型 B：行为语义差异（仓颉语言规范与 K2/FIR 行为不一致）**

例如：某个解析阶段的优先级顺序、可见性规则或错误恢复策略在仓颉编译器规范中有明确不同定义。

处理规则：
1. 在追溯矩阵中标注"语义冲突"并描述冲突点。
2. 提交至编译器规范评审，输出"本阶段保留上游行为"或"本阶段适配仓颉规范"的决定。
3. 若决定适配仓颉规范：变更必须有编译器规范文档引用作为依据，且在追溯矩阵记录"已适配，偏离上游原因：[引用]"。
4. 若决定保留上游行为：在追溯矩阵标注"保留上游，后续规范对齐待办"，并在 tasks 中创建后续跟进项。

**本阶段默认立场**：在未完成规范评审的情况下，**默认保留 K2/FIR 上游行为**，不做主动适配。规范适配作为后续独立变更处理。

**类型 C：功能缺失（仓颉编译器尚未实现某个上游依赖的基础能力）**

处理规则：
1. 在追溯矩阵标注"能力缺失，暂不迁移"并说明缺失能力。
2. 标记为本阶段迁移闭包的"已知豁免项"，在变更说明中显式列出。
3. 在 tasks 中创建后续补齐待办，不阻塞本次迁移。

## 接线核查（4.3）

- 核查对象：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt`
- 核查结果：未发现 `CfirCallCompleter` 的创建、注入或 `completeCall(...)` 调用；当前表达式解析流程仍经由 `callResolver` 与现有阶段化调用解析路径执行。
- 结论：本变更当前实现满足"本阶段不接入 `CFirExpressionsResolveTransformer`"约束。
- 后续待办：在后续变更中引入受控接线点（建议在 `resolveCallWithPhase3` 成功候选后、结果写回前增加可开关的 completion hook），并配套回归测试验证行为差异。