# cfir/providers/ — 符号 / 扩展点 Providers

CFIR 中的 Symbol / Extension / Call providers，对齐 Kotlin K2 `compiler/fir/providers`。负责回答"这个名字对应哪个声明 / 哪个扩展能用 / 哪些候选可调"。

历史上与 `:cfir:semantics` 一起从已弃用的 `:cfir:symbols` 拆分而来。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.resolve.providers` | `CfirSymbolProvider`、`CfirSyntheticFunctionProvider` 等核心 provider 接口 |
| `cfir.declarations` | 声明级 provider 实现 |
| `cfir.calls` | 调用候选 provider（与 `:cfir:resolve` 的 calls 子包协同） |
| `cfir.extensions` | extend 声明的扩展查找 |
| `cfir.resolve` | provider 与解析的接缝 |

## 典型 Provider

- 内置 provider（builtin types / functions）
- 源码 provider（当前编译单元）
- 反序列化 provider（来自 `:cfir:cfir-serialization` 的外部包）
- 扩展 provider（处理 `extend Type <: Interface { ... }`）

## 依赖

- `:cfir:cfir-tree`、`:cfir:cfir-cones`、`:cfir:cfir-common`、`:cfir:semantics`
- `:common`

## 命令

```bash
./gradlew :cfir:providers:assemble
./gradlew :cfir:providers:test
```

## 相关文档

- `../../docs/k2-module-alignment.md` — 与 Kotlin K2 `providers` 对照
