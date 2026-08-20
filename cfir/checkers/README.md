# cfir/checkers/ — 诊断检查器框架

在所需 CFIR resolve 信息可用后运行的独立诊断管线。它按 Kotlin K2 风格分为声明、表达式和类型 checker；每层下挂多个具体 checker，并由生成的 `Common*Checkers` 注册链路装配。`CHECKERS` 不是 `CfirResolvePhase` 的枚举成员。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.analysis.checkers` | Checker 注册链路（`CommonDeclarationCheckers` / `CommonExpressionCheckers` / `CommonTypeCheckers` 等） |
| `cfir.analysis.collectors` | 诊断收集器（遍历 CFIR、调度 checker） |
| `cfir.analysis.diagnostics` | 诊断定义入口（`CfirErrors` 等） |
| `cfir.analysis.extensions` | 检查器扩展点 |

诊断模型本身位于 `:common:diagnostics`；本模块只承载"在 CFIR 上执行检查并报告"。

## 注册扩展点

按 Kotlin K2 风格分桶（`CommonDeclarationCheckers` 当前提供）：

- `declarationCheckers` / `memberDeclarationCheckers` / `callableDeclarationCheckers` / `invalidDeclarationCheckers`
- `propertyCheckers` / `typeAliasCheckers` / `valueParameterCheckers`
- `mainFunctionCheckers` / `anonymousFunctionCheckers` / `enumConstructorCheckers`

`CommonExpressionCheckers`、`CommonTypeCheckers` 同理。可用 checker 及其注册位置以这些组件和生成代码为准。

## 子生成器

`checkers-component-generator` 子模块负责生成 checker 组件骨架，避免手写重复样板。

```bash
./gradlew :cfir:checkers:assemble
./gradlew :cfir:checkers:checkers-component-generator:run
./gradlew :cfir:checkers:test
```

## 设计原则

- **不持有可变状态**：每个 checker 是纯函数，从输入 CFIR 节点 + reporter 报告诊断
- **复用现有错误实体**：`ConeErrorType`、`CfirErrorExpression`、`CfirErrorReference` 等已有错误模型不重复造
- **位置策略统一**：通过 `PositioningStrategy`（在 `:common:diagnostics` 中）定位 source range

## 相关文档

- `../../docs/official-compiler-diagnostics.md` — 官方诊断清单
- `../../docs/cjfir-compiler-stages.md` — resolve 与诊断的管线边界
