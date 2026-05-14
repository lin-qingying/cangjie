# cfir/diagnostic-renderers/ — CFIR 诊断渲染器

把 `:common:diagnostics` 定义的 `Diagnostic` 渲染为用户可读字符串。对齐 Kotlin K2 `compiler/fir/checkers` 中的 renderer 部分（仓颉中独立成模块）。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.diagnostics` | 渲染入口与各诊断的 renderer 实现 |

## 设计要点

- 渲染逻辑与诊断**定义**分离：定义在 `:cfir:checkers` 的 `CfirErrors`，渲染在本模块
- 渲染参数（类型 / 名称 / 修饰符等）走通用 renderer，避免在 message bundle 里嵌入复杂格式化逻辑
- 支持本地化（通过 IntelliJ message bundle 机制）

## 调用方

- `:cfir:checkers` — 报告诊断时附带 renderer
- `:cfir:entrypoint` — 默认 renderer 注册

## 依赖

- `:common:diagnostics`
- `:cfir:cfir-tree`、`:cfir:cfir-cones`

## 命令

```bash
./gradlew :cfir:diagnostic-renderers:assemble
./gradlew :cfir:diagnostic-renderers:test
```

## 相关文档

- `../../docs/official-compiler-diagnostics.md` — 官方诊断清单（消息对照）
- `../checkers/README.md` — 诊断定义与检查框架
