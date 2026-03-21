## 1. 迁移基线与依赖闭包梳理

- [x] 1.1 基于 Kotlin 上游 K2/FIR 源码梳理 `FirCallCompleter` 完整依赖清单（接口、抽象、实现、辅助文件）并落盘为迁移对照表
- [x] 1.2 标记对照表中的 K1 入口依赖（含 `org.jetbrains.kotlin.resolve.calls` 与 `resolution.common` 整个约束层），并为每项定义处置方式：K2/FIR 等价迁移 / K1调用改写 / 直接删除
- [x] 1.3 盘点仓库内现有相关 `Cfir*` 声明并标注"可复用/需修正/需替换/缺失"状态，重点核查 `CfirConstraintStorage`、`CfirResultTypeResolver`、`CfirTypeVariable`、`InferenceLogger`（自研版）的处置决定
- [x] 1.4 建立"上游符号 -> 本仓符号"追溯矩阵，覆盖方法、接口、抽象、具体实现类与依赖声明；矩阵格式符合 proposal 中的追溯矩阵格式规范
- [x] 1.5 对 `resolve.calls.inference` 依赖做最终分类治理，形成三类清单：
    - `K2/FIR 可迁移`（`compiler/fir/resolve` 路径）：按批次迁移
    - `K1-only 禁止引入`（`compiler/resolution.common` 路径）：在追溯矩阵标注"不适用，K1 体系"
    - `K1调用改写`（K2/FIR 源码中残留的 ConstraintStorage 调用点）：逐一识别并在追溯矩阵登记改写映射

  **注意**：`ConstraintStorage`、`MutableConstraintStorage`、`ConstraintSystemCompleter`、`ConstraintInjector`、`ConstraintIncorporator`、`NewConstraintSystem`、`NewConstraintSystemImpl`、`ConstraintSystemBuilder` 及 `resolution.common/inference/components` 下全部组件层文件一律归入"K1-only 禁止引入"。

## 2. K2 推断会话层就位（批次一）

- [x] 2.1 迁移 `ConeConstraintSystemUtilContext.kt`，删除 `cfir/.../CfirConstraintStorage.kt`（若存在）及 `CfirTypeVariable.kt`
- [x] 2.2 迁移 `InferenceComponents.kt`
- [ ] 2.3 迁移 `FirInferenceSession.kt`（K2 约束能力核心载体，替代 K1 `ConstraintStorage` 语义入口）
- [ ] 2.4 迁移 `FirPCLAInferenceSession.kt`、`FirDelegatedPropertyInferenceSession.kt`
- [ ] 2.5 迁移 `FirInferenceLogger.kt`，替换并删除 `cfir/.../InferenceLogger.kt` 自研版本（含 `CfirConstraintKind`、`CfirInitialConstraint` 等自研模型）
- [ ] 2.6 迁移 `InferenceUtils.kt`（K2/FIR 层），删除 `CfirResultTypeResolver.kt`
- [ ] 2.7 执行批次一编译门禁：CFIR 推断会话相关模块编译通过；静态扫描确认无 `org.jetbrains.kotlin.resolve.calls.inference` K1 路径直接引用

## 3. 调用解析接口与候选模型层（批次二）

- [ ] 3.1 迁移 `ConeResolutionAtoms.kt`；同步替换并删除 `common/.../PostponedResolvedAtomMarker.kt`，切换所有调用点
- [ ] 3.2 迁移 `ResolutionContext.kt`、`CallKind.kt`、`CallInfo.kt`
- [ ] 3.3 迁移 `CheckerSink.kt`、`FirNamedReferenceWithCandidate.kt`、`errorCandidateUtils.kt`
- [ ] 3.4 迁移 `Candidate.kt`
- [ ] 3.5 迁移 `CandidateFactory.kt`；识别并改写所有 K1 `ConstraintStorage` 调用点（`setBaseSystem`、`addOtherSystem`、`asReadOnlyStorage` 等）为 K2 推断会话等价操作，在追溯矩阵以 `K1调用改写` 状态登记
- [ ] 3.6 迁移 `CandidateCollector.kt`、`CandidateTraversal.kt`
- [ ] 3.7 迁移 `TypeArgumentMapping.kt`、`ArgumentUtils.kt`
- [ ] 3.8 删除 `common/.../Constraint.kt`（K1 风格接口）；评估 `CfirFlatSignature.kt` 并形成处置决定（删除或保留）
- [ ] 3.9 执行批次二编译门禁：候选模型相关模块编译通过；无 K1 约束接口残留引用

## 4. 具体实现层迁移（批次三）

- [ ] 4.1 迁移 Tower 层（顺序：`TowerGroup` → `TowerLevels` → `TowerLevelHandler` → `TowerResolveManager` → `FirTowerResolveTask` → `FirInvokeResolveTowerExtension` → `FirTowerResolver`）
- [ ] 4.2 迁移 Stages 层（顺序：`ResolutionStages` → `CreateFreshTypeVariableSubstitutorStage` → `CollectTypeVariableUsagesInfo` → `FirArgumentsToParametersMapper` → `CheckArguments` → `CheckCallableReferenceExpectedType` → `ArgumentCheckingProcessor` → `ResolutionStageRunner`）；同步删除 `CfirMapArguments.kt`、`CfirInferTypeArguments.kt`；`CollectTypeVariableUsagesInfo` 中 `currentStorage().notFixedTypeVariables` 调用点须改写并在追溯矩阵标注 `K1调用改写`
- [ ] 4.3 迁移 Overloads 层（顺序：`ConeCallConflictResolver` → `ConeOverloadConflictResolver` → `ConeCompositeConflictResolver` → `ConeEquivalentCallConflictResolver` → `ConeIntegerOperatorConflictResolver` → `FirOverloadByLambdaReturnTypeResolver` → `FirDeclarationOverloadabilityHelperImpl` → `ReplOverloadCallConflictResolver`）
- [ ] 4.4 迁移 calls 根目录（顺序：`QualifierReceiver` → `VisibilityUtils` → `CallableReferenceAdaptation` → `ConstructorProcessing` → `SuperCalls` → `FirCallResolver`）
- [ ] 4.5 迁移补全器层（顺序：`CompletionModeCalculator` → `PostponedArgumentsAnalyzer` → `CollectionLiteralBoundsCollector` → `FirCallCompleter`）；所有 K1 `ConstraintStorage` 调用点须改写并在追溯矩阵标注 `K1调用改写`；**注意**：`ConstraintSystemCompleter`（K1）不迁移，其在 `FirCallCompleter` 中的依赖改写为 K2 推断会话操作

## 5. K1 设施清理验收

- [ ] 5.1 确认 `CfirConstraintStorage.kt` 已删除（若存在），无任何 CFIR 代码引用 `ConstraintStorage` 概念
- [ ] 5.2 确认 `CfirResultTypeResolver.kt`、`CfirTypeVariable.kt` 已删除，调用点已切换
- [ ] 5.3 确认 `common/.../Constraint.kt`、`PostponedResolvedAtomMarker.kt`、`ForkPointData.kt` 已删除
- [ ] 5.4 执行静态扫描：`org.jetbrains.kotlin.resolve.calls.inference` 包路径在调用补全链路中零引用
- [ ] 5.5 执行追溯矩阵完整性审查：无"状态=待处理"或"状态=空"条目；无将 `resolution.common` K1 符号标注为"已迁移"的条目

## 6. 验证与回归保护

- [ ] 6.1 为 `CfirCallCompleter` 与关键依赖契约添加/更新模块级测试：
    - 编译可通过，无 K1 包引用
    - `CfirCallCompleter` 可在 CFIR 上下文中实例化
    - 候选补全阶段可被触发，输出结果结构符合预期（以上游同等输入的黄金数据对比）
    - `FirInferenceSession` 路径下的约束操作可正常执行（类型变量创建、substitutor 构建）
- [ ] 6.2 执行受影响模块的定向构建与测试，记录结果与未覆盖风险
- [ ] 6.3 验证 `CFirExpressionsResolveTransformer` 未新增 `CfirCallCompleter` 接线（diff 检查），并在变更说明中明确后续接线待办
- [ ] 6.4 执行"禁止自主实现"审查：所有核心实现均有上游 K2/FIR 映射依据；所有 K1 调用点改写均有追溯矩阵记录；未映射项须给出适配理由并评审通过
