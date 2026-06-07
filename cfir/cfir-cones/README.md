# cfir/cfir-cones/ — CFIR 类型系统核心

CFIR 类型系统的"是什么"层。对齐 Kotlin K2 `compiler/fir/cones`。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.types` | `ConeCangjieType` 及其子类（`ConeClassLikeType` / `ConePrimitiveType` / `ConeFunctionType` / `ConeTupleType` / `ConeTypeParameterType` / `ConeIntersectionType` / `ConeErrorType` 等） |
| `cfir.resolve.substitution` | 类型替换（`ConeSubstitutor`、`ConeSubstitutorByMap` 等） |
| `cfir.resolve` | 类型解析辅助（与替换协同） |
| `cfir.render` | 类型渲染（用于 `toString()` / 错误信息） |
| `cfir.util` | 类型工具函数 |

## 依赖

- `:cfir:cfir-common`
- `:common`

无 `:psi` / `:cfir:cfir-tree` 依赖——本模块是 IR 树层下的纯类型模型。

## 命令

```bash
./gradlew :cfir:cfir-cones:assemble
./gradlew :cfir:cfir-cones:test
```

## 相关文档

- `../../docs/cjfir-compiler-stages.md` — CFIR_RESOLVE 中类型推断与替换设计
- `../../docs/cfir-body-resolve-constraint-system-design.md` — 类型系统在 BODY_RESOLVE 中的使用
- `../../docs/k2-module-alignment.md` — 与 Kotlin K2 `cones` 对照
