## 新增需求

### 需求:resolution.common 必须遵循 common 的类型系统契约
系统必须使 `resolution.common` 仅使用 `common` 模块中已定义的仓颉类型系统概念；任何与 `TypeSystemContext.kt` 现有契约冲突的 Kotlin 专属概念都必须被删除、替换或封装为不暴露 Kotlin 语义的内部机制。

#### 场景:删除与公共契约冲突的类型概念
- **当** `resolution.common` 仍直接依赖 FlexibleType、Kotlin 风格 nullability、显式 variance、star projection、raw/dynamic type 等概念
- **那么** 实现必须删除这些依赖，或将相关逻辑改写为符合仓颉 `TypeSystemContext` 契约的唯一实现路径

### 需求:resolution.common 必须移除 K1 兼容路径
系统必须删除 `resolution.common` 中仅为 Kotlin K1/K2 迁移保留的兼容入口、遗留实现、特性分支和依赖注入通道；模块运行时不得再依赖 K1 专属逻辑决定行为。

#### 场景:存在 K1 遗留实现
- **当** 模块中存在 `@K1Deprecation`、Legacy 实现、DefaultForK1DependencyInjection、仅用于 K1 行为复现的条件分支
- **那么** 实现必须移除这些代码路径，或将其替换为单一的仓颉主路径实现

### 需求:LanguageFeature 必须仅表达仓颉实际语言概念
系统必须使 `LanguageFeature` 中被 `resolution.common` 使用的特性开关仅对应仓颉真实存在且仍需门控的语言概念；对于历史移植遗留且不再需要的特性判断，系统必须直接删除条件分支并保留唯一行为。

#### 场景:发现 Kotlin 专属特性判断
- **当** `resolution.common` 中的类型推断、近似或约束逻辑依赖 Kotlin 专属 `LanguageFeature` 判断
- **那么** 实现必须删除该特性判断，或将其重写为仓颉语言中的真实概念门控

### 需求:慢断言不得改变类型判断语义
系统必须在 `AbstractTypeChecker` 中提供 `RUN_SLOW_ASSERTIONS` 调试开关，但该开关只能用于校验仓颉类型系统与约束系统不变量，禁止改变 subtype、equalTypes 或公共父类型计算结果。

#### 场景:开启慢断言
- **当** `RUN_SLOW_ASSERTIONS` 为开启状态
- **那么** 系统必须额外执行内部一致性断言，但必须保持关闭该开关时的类型判断结果完全一致

### 需求:仓颉官方概念优先于 Kotlin 参考实现
系统必须以 `external/cangjie_compiler` 和 `common` 模块中的仓颉契约作为概念来源；Kotlin 参考实现只能用于借鉴算法结构或断言接线方式，禁止将 Kotlin 专属语言语义重新引入仓颉实现。

#### 场景:Kotlin 参考实现包含仓颉不存在的概念
- **当** Kotlin 参考代码中的某个类型系统概念在官方仓颉 C++ 实现和 `common` 契约中都没有对应证据
- **那么** 实现禁止保留该概念对应的语言语义，仅允许在有明确内部建模必要时保留经过重命名或重写的内部机制

## 修改需求

## 移除需求
