# cfir/checkers/ — 诊断检查器框架

CFIR_RESOLVE 末尾 Phase `CHECKERS` 的实现。按 Kotlin K2 风格分层：声明 checkers / 表达式 checkers / 类型 checkers，每层下挂多个具体 checker，由生成的 `Common*Checkers` 注册链路装配。

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

`CommonExpressionCheckers`、`CommonTypeCheckers` 同理。当前部分扩展点为空，详见 `../analysis-tests/diagnostics-coverage-gap-vs-cpp.md`。

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

- `../analysis-tests/diagnostics-coverage-gap-vs-cpp.md` — 当前诊断覆盖缺口
- `../../docs/diagnostics-gap-vs-official-cpp-sema-status-2026-04-06.md` — 与官方 sema 诊断对照
- `../../docs/official-compiler-diagnostics.md` — 官方诊断清单
