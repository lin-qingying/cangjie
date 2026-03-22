## 为什么

当前 CFIR 的 `BODY_RESOLVE` 语义核心仍明显受 Kotlin 内部约束系统和类型检查桥接层影响，导致表达式求解、调用补全、类型比较、类型变量传播与官方仓颉 C++ 编译器语义之间存在持续偏差。尤其是在 `ConstraintSystemCompleter`、`CfirTypeCheckerContext`、`ConeTypeContext`、`ConeStubType`、`ConeCapturedType` 等路径上，现有设计更像迁移中的中间态，而不是仓颉原生的长期架构。

现在需要提出一条新的架构变更：在保留 `ConeCangJieType` 及其主要具体子类作为 CFIR 统一类型表示的前提下，重构 `BODY_RESOLVE` 的语义中枢，使其以官方仓颉 C++ 编译器的双向类型检查、候选局部求解、类型兼容性分级、`QuestTy`、理想数值类型、`extend` 与泛型完备性语义为基线，并最终能够由 `CfirCallCompleter` 直接接入。

## 变更内容

- 重新定义 `BODY_RESOLVE` 的语义核心，把约束系统、类型对比系统、类型变量/占位类型语义和调用补全接入边界纳入同一套 CFIR 设计。
- 明确保留 `ConeCangJieType` 作为统一类型表示，不再引入平行的 `CjType*` 或 `CfirType*` 类型表示层。
- 重新规划 `Cone*` 子类职责边界：保留稳定的真实语义类型表示，降级或替换 `ConeStubType`、`ConeCapturedType` 及其他 Kotlin 风格推断中间产物。
- 将类型比较从当前的 Kotlin 风格标记/上下文桥接模型中解耦，建立更贴合仓颉官方语义的类型关系与兼容性模型。
- 建立以候选局部求解为中心的约束系统，覆盖占位类型变量、构造子形状约束、理想数值类型、`QuestTy` 回退、`extend` 过滤与泛型完备性。
- 重新定义 `CfirCallCompleter` 的接入角色，使其作为 CFIR 调用补全入口，直接消费新的 BODY 语义求解结果，而不是继续耦合旧的补全/约束存储体系。
- 明确该变更对 `CfirAbstractBodyResolveTransformer` 的影响边界：保留 transformer / dispatcher 外壳，重构 `BodyResolveTransformerComponents` 及 `expressionsTransformer -> callResolver -> callCompleter` 协作链。

## 功能 (Capabilities)

### 新增功能
- `cfir-body-resolve-semantic-core`: 重新定义 CFIR `BODY_RESOLVE` 的语义核心，包括双向类型检查、候选局部求解、类型关系、约束传播、调用补全接入和与 transformer 架构的协作边界。

### 修改功能
- `cfir-type-system`: 调整 CFIR 类型系统需求，明确保留 `ConeCangJieType` 作为统一类型表示，同时重构其子类职责边界和与类型关系系统、约束系统的协作方式。
- `cfir-call-resolution`: 调整调用解析与补全需求，使 `CfirCallCompleter` 能直接接入新的 BODY 语义体系，并重新定义与候选求解、重载排序、结果写回之间的关系。

## 影响

- 受影响代码：`cfir/resolve`、`cfir/cfir-cones`、`cfir/providers`、`cfir/checkers` 中与类型比较、约束求解、调用补全和 body resolve 组件装配相关的路径。
- 受影响架构：`BODY_RESOLVE` 语义核心、`CfirAbstractBodyResolveTransformer` 组件装配、`CfirCallCompleter` 接口边界、`Cone*` 推断中间类型体系。
- 受影响行为：表达式类型合成、期望类型检查、候选局部推断、重载排序、`QuestTy` 回退、理想数值类型具体化、`extend` 参与解析和泛型完备性检查。
- 依赖关系：需要后续设计和规范明确 `cfir-body-resolve-semantic-core`、`cfir-type-system`、`cfir-call-resolution` 之间的契约，作为后续 `design.md`、`specs/` 和 `tasks.md` 的基础。
