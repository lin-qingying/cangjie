# cfir/resolve/ — CFIR 语义解析引擎

前端管线阶段 7 `CFIR_RESOLVE` 的核心实现。多 Phase 渐进式语义解析，每个声明独立跟踪已完成的 Phase。对齐 Kotlin K2 `compiler/fir/resolve`。

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
  → CHECKERS        诊断检查器
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
- **不依赖 checkers**：硬约束，CHECKERS Phase 由 `:cfir:checkers` 提供，resolve 只负责推进到 CHECKERS
- **CfirResolveComponentsRegistrar**：单一切换点，便于按 Phase 回滚（见 `../cfir-tree/resolve-rollback-plan.md`）
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

- `../../cjfir-compiler-stages.md` 第 7 阶段
- `../../docs/cfir-body-resolve-constraint-system-design.md` — BODY_RESOLVE 约束系统
- `../../docs/type-inference-four-systems-comparison.md` — 四套类型推断对照
- `../../docs/plans/2026-03-23-resolution-common-inference-migration.md` — 推断迁移计划
- `../cfir-tree/resolve-rollback-plan.md` — Phase-by-phase 回滚预案
