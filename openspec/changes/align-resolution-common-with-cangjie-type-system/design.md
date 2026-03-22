## 上下文

当前仓库在类型系统层已经形成明显的双轨状态。

一方面，`common/src/org/cangnova/cangjie/type/model/TypeSystemContext.kt`、`TypeSystemContextContextual.kt`、`TypeSystemInferenceExtensionContextContextual.kt` 已经把仓颉类型系统公共契约收敛为“刚性类型 + 泛型参数 + captured/stub 等内部推断中间态”的模型，并明确删除了 Kotlin 风格的 flexible type、`isMarkedNullable`/`withNullability`、显式 variance、star projection、raw/dynamic type 等 API。`common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt` 也已经按仓颉语义重写了核心子类型算法，说明公共契约的目标形状已基本稳定。

另一方面，`resolution.common` 仍大量保留 Kotlin 移植残留：
- `AbstractTypeApproximator.kt`、`NewCommonSuperTypeCalculator.kt`、`TypeCheckerStateForConstraintSystem.kt`、`ConstraintInjector.kt` 等文件持续依赖 flexible/nullability/variance/star projection/DefinitelyNotNull/raw/dynamic 等概念；
- `TypeApproximatorConfiguration.kt`、`NewConstraintSystemImpl.kt`、`ConstraintSystemUtilContext.kt`、`VariableFixationFinder.kt` 等文件仍保留 K1/K2 兼容分支、`@K1Deprecation`、Legacy 实现和仅为 Kotlin 迁移存在的配置；
- `LanguageVersionSettings.kt` 中当前只保留极少数仓颉侧语言特性，但 `resolution.common` 仍引用 Kotlin 风格语言特性，造成公共配置与实际消费点不一致。

本地参考实现进一步给出了概念边界：
- `external/cangjie_compiler` 中明确存在 generic + upper bounds、`Nothing`、`Option/Quest` 以及内部 `union/intersection` 语义；
- 同一参考中未找到 Kotlin 风格 flexible type、star projection、显式 variance modifier 的直接证据；
- `external/kotlin` 中 `AbstractTypeChecker.RUN_SLOW_ASSERTIONS` 提供了合适的接线模式：只做内部不变量断言，不改变主路径语义。

因此，本设计的任务不是“重新发明一套新的约束系统”，而是把 `resolution.common` 重新压回 `common` 已经声明的仓颉契约，并把仍然需要的内部机制与 Kotlin 语言语义彻底脱钩。

## 目标 / 非目标

**目标：**
- 让 `resolution.common` 仅依赖 `common` 中已经存在且被仓颉语义允许的类型系统概念。
- 删除模块内仅用于 Kotlin K1/K2 迁移的遗留代码、特性分支和兼容入口。
- 为仍需保留的内部推断概念建立明确边界：只保留 captured/stub/constraint-store 等与仓颉推断实现直接相关的内部机制，不再保留 Kotlin 专属语言语义。
- 将 `LanguageFeature` 的使用改写为仓颉真实概念，或直接删除不再需要的门控分支。
- 在 `AbstractTypeChecker` 中增加 `RUN_SLOW_ASSERTIONS`，并把它定义为调试/测试态的不变量检查开关。

**非目标：**
- 不重做整套 `BODY_RESOLVE` 架构，也不以此变更为契机重写候选解析器、tower、call completer 或完整推断流程。
- 不重新定义仓颉语言的官方类型系统语义；概念边界以 `common` 契约和 `external/cangjie_compiler` 的本地证据为准。
- 不把 Kotlin 参考实现中的 flexible/nullability/variance/star projection 语义重新包装后继续引入仓颉实现。
- 不在本次变更中引入新的外部依赖或新的配置系统。

## 决策

### 决策 1：以 `common` 契约为唯一公共类型系统边界

`resolution.common` 的所有公共使用面都必须服从 `TypeSystemContext.kt` 及其 contextual 扩展当前已经暴露的能力，而不是继续隐式依赖 Kotlin 原版 API 名称。

**原因：**
- `common` 已是仓颉编译器内部的类型系统抽象层；
- 若继续让 `resolution.common` 以 Kotlin 语义为中心，会持续制造“公共契约已删除，调用方仍在假设存在”的不一致；
- 这能把“应该保留什么概念”的决策提前固定，避免实现阶段边修边漂移。

**备选方案：**
- 保留 `resolution.common` 当前 Kotlin 形状，仅在 `common` 中补回缺失 API：拒绝，因为这会把已明确删除的 Kotlin 概念重新引回公共契约。
- 在 `resolution.common` 内部建立一层 Kotlin 兼容适配层：拒绝，因为这会继续隐藏不正确的语言语义，并增加未来删除成本。

### 决策 2：为每类 Kotlin 概念建立“删除 / 重写 / 保留”处置表

实现时不按文件随意清理，而按概念分组推进：

1. **直接删除**：FlexibleType、Kotlin nullability 标记、DefinitelyNotNull、显式 variance、star projection、raw/dynamic type、K1-only feature gating。
2. **保留但去 Kotlin 语义化**：captured type、stub type、constraint-store 相关结构，仅作为内部推断建模使用。
3. **重写为仓颉概念**：
   - 若当前逻辑意图表达的是“可选/quest/option”，则迁移到仓颉已有语义；
   - 若当前逻辑依赖公共父类型、整数字面量、intersection/union 等官方存在概念，则按仓颉现有概念改写，不再借道 Kotlin 语义。

**原因：**
- 用户的要求不是简单删文件，而是“删除仓颉没有的概念，同时保留必要的 K2 主路径”；
- 只有先定义概念处置表，才能避免把内部推断机制误删，或者把 Kotlin 语言语义伪装成内部机制保留下来。

**备选方案：**
- 按编译错误逐个修：拒绝，因为会遗漏静态仍可编译但语义已经错误的分支。
- 先整体迁回 Kotlin K2 再做仓颉改写：拒绝，因为方向相反，会扩大错误语义面。

### 决策 3：`LanguageFeature` 只保留仓颉真实概念，其他分支直接折叠

`resolution.common` 中的语言特性控制必须分成两类：
- 如果仓颉确实存在相应概念且仍需要显式开关，则在 `LanguageVersionSettings.kt` 中定义并统一使用；
- 如果只是 Kotlin 迁移遗留，则直接删除判断并保留唯一行为，不再制造“名义上有特性，实质上没有语言语义”的伪配置。

**原因：**
- 当前 `LanguageVersionSettings.kt` 非常精简，说明仓颉侧并不依赖 Kotlin 的特性矩阵；
- 留着无对应语义的特性判断，只会让代码路径继续伪装成“未来可配置”，增加维护负担。

**备选方案：**
- 把 Kotlin 现有 `LanguageFeature` 全量迁入仓颉：拒绝，因为这会把并不存在的语言概念制度化。

### 决策 4：`RUN_SLOW_ASSERTIONS` 采用 Kotlin 的接线方式，但只校验仓颉不变量

在 `common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt` 中增加与 Kotlin 类似的全局调试开关，并只在以下位置增加 guarded assertions：
- 类型检查输入形状是否合法；
- 约束系统状态推进是否合法；
- captured/stub/approximation 的递归或替换过程是否满足仓颉内部不变量；
- 公共父类型与约束合并过程中是否出现不应发生的内部状态。

禁止把慢断言用于保留或模拟 Kotlin nullability/flexible/variance/star-projection 语义。

**原因：**
- Kotlin 参考实现已经证明这是一种低风险模式：`RUN_SLOW_ASSERTIONS` 只做 debug-only 校验，不改变主路径算法；
- 用户明确要求增加该能力，但也明确要求“不修改类型判断语义”。

**备选方案：**
- 不引入慢断言：拒绝，因为会失去调试复杂迁移时的关键保护。
- 把慢断言嵌入到具体返回值逻辑中：拒绝，因为这会让 debug 开关影响语义结果。

### 决策 5：实施顺序按“契约收敛优先，算法修复随后”推进

实现顺序固定为：
1. 盘点并分类 `resolution.common` 中的 Kotlin 概念与 K1 兼容点；
2. 裁剪 `LanguageFeature` 与 K1-only 分支；
3. 处理 `TypeApproximator` / `CommonSuperTypeCalculator` / `ConstraintSystem` 中的 Kotlin 专属概念；
4. 引入并接线 `RUN_SLOW_ASSERTIONS`；
5. 编译、诊断与定向测试验证。

**原因：**
- 先裁剪契约，再调整算法，能减少“删一处补一处”的反复；
- `tasks.md` 可以直接沿着这个顺序构建可验证的小步骤。

## 风险 / 权衡

- [误删内部推断机制] → 先按“概念处置表”分类，再动实现；captured/stub 等仅在确认仍服务仓颉推断时保留。
- [把 Kotlin nullability/quest/option 混为一谈] → 明确以官方 C++ 的 `Option/Quest` 证据作为仓颉概念来源，不复用 Kotlin `isMarkedNullable` / `withNullability` 语义。
- [删除 K1 兼容代码后牵连隐藏调用点] → 在实现时对 `@K1Deprecation`、Legacy 类、默认注入入口和相关引用统一搜索并成组迁移，避免零散残留。
- [`RUN_SLOW_ASSERTIONS` 意外改变行为] → 只允许在 guarded block 中增加 `check/assert/error` 类校验，禁止改变 subtype/equality/CST 的返回值分支。
- [变更范围扩张为完整语义重写] → 明确非目标：不重做完整 `BODY_RESOLVE`，只对齐 `resolution.common` 契约与调试断言。

## Migration Plan

1. 在 `resolution.common` 中建立 Kotlin 专属概念与 K1-only 路径清单，确认每项的处置方式。
2. 先修改 `common` 与 `LanguageVersionSettings` 的必要契约点，使目标边界明确且可编译。
3. 按概念组重写 `TypeApproximator`、`NewCommonSuperTypeCalculator`、`TypeCheckerStateForConstraintSystem`、`ConstraintInjector` 等核心文件。
4. 删除 Legacy/K1 兼容实现与无效 feature gate，保证模块只剩仓颉主路径。
5. 引入 `RUN_SLOW_ASSERTIONS` 并将现有适合保留的断言迁移为仓颉不变量检查。
6. 对修改文件执行 LSP 诊断、模块编译和定向测试；若出现语义不确定点，仅对该点升级为显式待决问题。

回滚策略：若某个概念组的迁移导致 `resolution.common` 无法维持基本编译或核心测试失败，则按概念组整体回退，不保留半删除状态。

## Open Questions

- 官方 C++ 参考中存在 `Option/Quest` 相关语义，而用户口述中强调“仓颉没有可空类型概念”。实现阶段需要把这两者统一表述为“禁止 Kotlin 风格 nullability API，但允许仓颉自身的 Option/Quest 语义”，并在具体代码上严格区分。
- `captured type` 与 `stub type` 在 `common` 契约中仍然存在；实现阶段需要逐个调用点确认它们是否纯属内部推断机制，还是仍混入了 Kotlin 投影/nullability 假设。
- `LanguageFeature` 最终需要保留哪些仓颉侧开关，目前从仓库可见证据只支持极少量特性；若实施中出现无法从代码或官方参考直接判断的个别概念，需要单点升级为用户裁决，而不是扩大整体现有特性集合。
