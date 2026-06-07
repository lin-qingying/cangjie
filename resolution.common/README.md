# resolution.common/ — 类型推断与调用解析公共层

对齐 Kotlin `compiler/resolution.common`。提供与 CFIR 解耦的类型系统模型、约束系统、调用解析基础组件，供 `:cfir:resolve` 复用。

模块名带点（`resolution.common` 而非 `resolution-common`）也是为了与 Kotlin 对齐。

## 关键包

| 包 | 职责 |
|---|---|
| `resolve.calls.model` | 调用模型（candidate、receiver、argument） |
| `resolve.calls.tower` | Tower-based scope walking |
| `resolve.calls.components` | 调用解析 components（参数映射、可见性、类型推导触发等） |
| `resolve.calls.inference` | 推断器接入（`ResultTypeResolver`、`PostponedArgumentInputTypesResolver` 等） |
| `resolve.calls.tasks` | 解析任务调度 |
| `resolve.calls.results` | 解析结果（成功 / 候选多 / 失败） |
| `type.model` | 类型系统抽象与 context 桥接（`TypeSystemContextBridge.kt`） |

## 已迁移内容

以下 Kotlin 风格组件已切换到仓颉刚性类型模型：

- `AbstractTypeChecker`（含 `RUN_SLOW_ASSERTIONS` 与 `prepareType` 契约）
- `NewCommonSuperTypeCalculator`
- `TypeApproximatorConfiguration` / `AbstractTypeApproximator`
- `TypeCheckerStateForConstraintSystem`
- `ConstraintInjector` / `ConstraintIncorporator`
- `ResultTypeResolver`
- `TrivialConstraintTypeInferenceOracle`
- `PostponedArgumentInputTypesResolver`

## Slow assertions 开关

`AbstractCangjieCompilerTest` 已接入：

```bash
./gradlew :cfir:resolve:test -Dcangjie.slow.assertions=true
```

默认关闭，不影响正常编译路径。测试 / 调试时可启用以执行 `resolution.common` 中已迁移的 guarded invariants。

## 桥接层

`type/model/TypeSystemContextBridge.kt` 是显式 context-argument 桥接层，用于消除历史 Kotlin 风格扩展调用在仓颉 `TypeSystemInferenceExtensionContext` 下的歧义。后续可视情况内联回核心 API。

## 命令

```bash
./gradlew :resolution.common:compileKotlin
./gradlew :resolution.common:test
```

## 调用方

主要被 `:cfir:resolve` 依赖；其它解析阶段如有跨阶段约束 / 推断需求，亦应优先复用本模块抽象。

## 相关文档

- `../docs/type-inference-four-systems-comparison.md` — 四套类型推断对照
- `../docs/cfir-body-resolve-constraint-system-design.md` — BODY_RESOLVE 约束系统
- `../docs/plan-conflicting-type-constraints.md`、`../docs/plan-operator-overload-numeric-widening.md`
