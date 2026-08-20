# cfir/resolve/ — CFIR 语义解析引擎

ordinary CFIR resolve 的核心实现。它以多个阶段渐进式完成语义解析，每个声明独立跟踪已完成的 resolve phase。对齐 Kotlin K2 `compiler/fir/resolve`。

## Phase 顺序

```
RAW_CFIR
  → IMPORTS         导入符号绑定
  → SUPER_TYPES     超类 / 接口层次
  → TYPES           显式类型引用
  → STATUS          声明状态（public / private / open / abstract / mut / static）
  → EXTENSIONS      extend 声明（仓颉特有）
  → IMPLICIT_TYPES  隐式类型推断
  → BODY_RESOLVE    方法体推断、重载解析
```

宏构造不属于 ordinary resolve phase。它在 source final provider 注册前完成：
`PreMacroRawBuildResult → MacroConstructionService → recordExpandedRawFilesOnce → ordinary resolve`。

每个 ordinary Phase 边界均可注入编译器插件（扩展点）。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.resolve` | Phase 推进框架与处理器 |
| `cfir.resolve.body` | BODY_RESOLVE 主体（`BodyResolveTransformerComponents`、`BodyResolveContext`） |
| `cfir.resolve.calls` | 调用解析（候选选择、参数绑定、重载） |
| `cfir.resolve.inference` | 类型推断 |
| `cfir.resolve.providers` | 解析阶段使用的 providers |
| `cfir.resolve.dfa` | 数据流分析（DFA） |
| `cfir.constraints` | 约束系统 |

## 设计要点

- **惰性分阶段**：同一文件中不同声明可处于不同 Phase，按需推进
- **诊断边界独立**：`:cfir:checkers` 在所需 resolve 信息可用后运行诊断管线；它不是 `CfirResolvePhase` 的枚举成员
- **CfirResolveComponentsRegistrar**：解析组件的集中装配点
- **WithExpectedType**：通过 `expectedTypeRef.coneType` 读取期望类型，lambda 期望类型传播时补齐 `CfirResolvedTypeRef`

## 依赖

- `:cfir:cfir-tree`、`:cfir:cfir-cones`、`:cfir:cfir-common`、`:cfir:semantics`、`:cfir:providers`、`:cfir:checkers`
- `:resolution.common`（类型推断 / 约束公共层）
- `:common`、`:common:diagnostics`、`:util`

## 命令

```bash
./gradlew :cfir:resolve:assemble
./gradlew :cfir:resolve:test
```

启用 slow assertions（验证 `resolution.common` 中的 guarded invariants）：

```bash
./gradlew :cfir:resolve:test -Dcangjie.slow.assertions=true
```

## 相关文档

- `../../docs/cjfir-compiler-stages.md` — 前端阶段与诊断边界
- `../README.md` — CFIR 子系统目录
- `../../docs/module-catalog.md` — Gradle 模块目录
