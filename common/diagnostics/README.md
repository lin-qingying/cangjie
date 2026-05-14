# common/diagnostics/ — 诊断框架核心

仓颉编译器的诊断子系统核心，独立于任何具体处理阶段。对齐 Kotlin K2 `compiler/util` + `compiler/frontend.common.psi/diagnostics`。

历史上从 `:cfir:cfir-common` 拆出，使之能被 `:cfir:*` 与 `:analysis:*`、未来后端模块共享。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.diagnostics` | 核心抽象：`DiagnosticFactory` / `Reporter` / `Severity` / `Collector` / `PositioningStrategy` |
| `cfir.diagnostics.impl` | 内部实现 |
| `cfir.diagnostics.rendering` | 渲染通用工具（具体 renderer 在 `:cfir:diagnostic-renderers`） |

## 设计要点

- **作为独立子系统**：多个处理阶段共享，必须独立于任何特定阶段
- **PositioningStrategy**：把 source 元素映射到精确的范围（offset/length），与 PSI 解耦
- 报告流水线：`DiagnosticFactory` → `Reporter` → `Collector`

## 调用方

- `:cfir:checkers`（定义并报告诊断）
- `:cfir:resolve`（cone 诊断映射）
- `:analysis:*`（暴露给 IDE）
- `:cfir:diagnostic-renderers`（渲染）

## 依赖

- `:common`

## 命令

```bash
./gradlew :common:diagnostics:assemble
./gradlew :common:diagnostics:test
```

## 相关文档

- `../../docs/official-compiler-diagnostics.md` — 官方诊断清单
- `../../docs/diagnostics-gap-vs-official-cpp-full.md` — 与官方对照
- `../../cfir/checkers/README.md` — 诊断定义与报告
- `../../cfir/diagnostic-renderers/README.md` — 诊断渲染
