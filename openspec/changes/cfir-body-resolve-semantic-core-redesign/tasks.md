## 1. 类型关系基础重建

- [x] 1.1 盘点 `cfir/resolve`、`cfir/providers`、`cfir/checkers` 中现有 `CfirTypeCheckerContext`、`ConeTypeContext`、`AbstractTypeChecker` 相关入口，标记哪些路径属于长期保留壳层、哪些属于待替换比较中心
- [x] 1.2 在 `cfir/resolve` 中引入 `CfirTypeRelations` 的核心接口，明确 compare / subtype / equality / conversion / extend relation 的入口边界
- [x] 1.3 设计并落地 `CfirTypeRelationResult` 分级结果，覆盖完全相同、子类型、可转换兼容、可通过变量约束收敛、`QuestTy` 回退兼容与不兼容等判定
- [x] 1.4 将现有调用解析、重载排序、类型检查工具中的直接 `isSubtype` 依赖迁移到新的类型关系入口

## 2. 类型表示与变量状态整理

- [x] 2.1 盘点 `cfir/cfir-cones` 中所有 `Cone*` 类型，区分稳定具体类型、过渡推断类型和待清理的 Kotlin 风格中间产物
- [x] 2.2 保留 `ConeCangJieType` 作为统一类型根，并在文档和代码结构中明确稳定保留的 `Cone*` 具体类型集合
- [x] 2.3 为类型变量状态引入独立表示，明确 `ConeTypeVariableId`、`ConeTypeVariableState`、`ConeTypeVariableRef` 与 `ConeTypeVariableType` 的边界
- [x] 2.4 重新规划 `ConePlaceholderType`、`ConeDeferredType`、`ConeStubType`、`ConeCapturedType` 的职责，并将后两者降级为过渡实现或待替换类型

## 3. 约束系统重建

- [x] 3.1 在 `cfir/resolve` 中定义 `CfirConstraint`、`CfirConstraintStore`、`CfirConstraintIssues` 的基础结构
- [x] 3.2 实现 `CfirVariableManager`，支持占位类型变量、实例化变量和延迟分析边界变量的分配与跟踪
- [x] 3.3 实现 `CfirConstraintPropagation`，覆盖相等、子类型、兼容性、构造子形状、`extend` 相关条件和泛型上界约束传播
- [x] 3.4 实现 `CfirConstraintCompleter`，按候选局部求解流程处理延迟分析单元、变量固定、理想数值类型收敛和 `QuestTy` 回退
- [x] 3.5 实现 `CfirConstraintResultBuilder`，把局部求解会话转换为可供调用补全与重载排序消费的结果对象

## 4. 调用解析与补全接线

- [x] 4.1 重构 `CfirCallResolver`，使其从“解析 + 旧推断协作”转向“候选收集 + 候选局部求解 + 候选结果输出”
- [x] 4.2 重构 `CfirCallCompleter`，使其直接依赖 `CfirCallCompletion` 并消费新的候选局部求解结果
- [x] 4.3 将期望类型正式接入调用求解路径，确保带期望类型的调用能够进入候选局部约束系统
- [x] 4.4 将 `extend` 参与信息、理想数值类型具体化和 `QuestTy` 回退信息接入重载排序输入模型

## 5. BODY_RESOLVE 组件装配重构

- [x] 5.1 保留 `CfirAbstractBodyResolveTransformer` / dispatcher 外壳，梳理 `BodyResolveTransformerComponents` 当前装配的全部服务
- [x] 5.2 以 `CfirTypeRelations`、`CfirConstraints`、`CfirCallCompletion` 为中心重组 `BodyResolveTransformerComponents` 的服务图
- [x] 5.3 调整 `CfirExpressionsResolveTransformer -> CfirCallResolver -> CfirCallCompleter` 协作链，使其符合新的 BODY 语义核心边界
- [x] 5.4 验证 declarations / expressions 分发外壳不被破坏，同时确保新的语义服务能够覆盖调用、控制流和表达式求解主路径

## 6. 迁移清理与验证

- [x] 6.1 将旧 `ConstraintSystemCompleter`、旧类型比较桥接入口和过时中间类型标记为过渡实现或待删除路径
- [x] 6.2 补充覆盖类型关系分级、候选局部求解、调用补全、`QuestTy` 回退、理想数值类型和 `extend` 参与解析的测试
- [x] 6.3 验证 `BODY_RESOLVE`、`CfirCallCompleter`、`CfirAbstractBodyResolveTransformer` 和 `Cone*` 类型表示层之间的边界与设计文档一致
- [x] 6.4 清理与新设计冲突的命名、注释和过时协作路径，确保最终代码结构与规范、设计、任务三者一致
