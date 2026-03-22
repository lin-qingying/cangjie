## 为什么

`common` 模块已经把仓颉类型系统的基础契约收敛为刚性类型模型：无 FlexibleType、无 Kotlin 风格 nullability 标记、无显式 variance、无星号投影，并以 `TypeSystemContext.kt` 及其 contextual 扩展作为统一入口；但 `resolution.common` 仍保留大量 Kotlin K1/K2 迁移残留和 Kotlin 专属类型概念，导致模块内部语义与公共契约长期不一致，并持续引入编译错误、无效分支和错误的语言特性开关。

现在需要把 `resolution.common` 收敛为仅服务仓颉 K2 风格约束系统和类型检查的模块：删除仓颉不存在的概念、清理 K1 兼容层、将语言特性控制改写为仓颉实际语义，并补上仅用于调试校验的 `RUN_SLOW_ASSERTIONS` 守卫，以便后续实现可以在不改变类型判断语义的前提下稳定推进。

## 变更内容

- 清理 `resolution.common` 中仅为 Kotlin K1/K2 兼容保留的代码路径、注解、遗留实现和依赖注入入口，只保留当前仓颉前端实际需要的 K2 风格推断/约束系统主路径。
- 删除或重写 `resolution.common` 中与仓颉公共类型系统契约冲突的 Kotlin 专属概念，包括但不限于 FlexibleType、Kotlin 风格 nullability/DefinitelyNotNull、显式 variance、star projection、raw/dynamic 类型以及依赖这些概念的近似逻辑和公共父类型计算分支。
- 按 `common/src/org/cangnova/cangjie/type/model/TypeSystemContext.kt` 的现有能力重新定义 `resolution.common` 的使用边界：
  - 保留仓颉仍需要的内部推断中间概念（例如 captured/stub type）仅限于其确有内部建模价值的部分；
  - 对没有官方 C++ 语义支撑、也没有 `common` 契约支撑的概念直接删除；
  - 对必须保留但当前实现混入 Kotlin 假设的逻辑，改写为仓颉语义下的内部不变量。
- 重写 `LanguageFeature` 在 `resolution.common` 中的使用：
  - 与仓颉语言无关的 Kotlin 特性开关删除；
  - 仅在存在真实仓颉语言概念且仍需要版本/特性门控时保留；
  - 对仅因历史移植而存在的条件判断，直接去除特性门控并保留唯一实现路径。
- 在 `common` 的 `AbstractTypeChecker` 中引入 `RUN_SLOW_ASSERTIONS`，并将其设计为调试/测试态下的内部断言开关，只校验仓颉类型系统与约束系统不变量，不改变 subtype/equality 语义。
- **BREAKING**: `resolution.common` 内部 API、辅助类型和兼容层将被删除或重命名，任何仍依赖 Kotlin 专属概念的调用点都必须迁移到新的仓颉契约。

## 功能 (Capabilities)

### 新增功能
- `resolution-common-type-system-alignment`: 将 `resolution.common` 的类型推断、近似、公共父类型和约束系统辅助逻辑收敛到仓颉实际存在的类型概念，并补充不改变语义的慢断言机制。

### 修改功能

## 影响

- 主要影响模块：`resolution.common`、`common`。
- 重点影响文件族：
  - `resolution.common/src/org/cangnova/cangjie/types/*`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/NewCommonSuperTypeCalculator.kt`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/*`
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/*`
  - `common/src/org/cangnova/cangjie/type/model/TypeSystemContext*.kt`
  - `common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt`
  - `common/src/org/cangnova/cangjie/LanguageVersionSettings.kt`
- 参考输入来源：
  - `common` 模块中的类型系统契约与 contextual 扩展；
  - `external/cangjie_compiler` 中官方 C++ 实现体现的泛型、上界、Option/Quest、Nothing、union/intersection 概念；
  - `external/kotlin` 中 `AbstractTypeChecker.RUN_SLOW_ASSERTIONS` 的只读参考模式（仅用于断言接线方式，不用于引入 Kotlin 语义）。
- 风险边界：本变更只对齐类型系统契约与调试断言，不把范围扩张为完整 `BODY_RESOLVE` 语义重构。
