# cfir/cfir-tree/ — 生成式 CFIR 节点定义

CFIR 节点的"是什么"层：声明 / 表达式 / 类型引用 / 模式 / visitor / transformer 等核心数据节点全部由 `tree-generator` 子模块生成，避免手写大量样板代码。对齐 Kotlin K2 `compiler/fir/tree`。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.declarations` | 声明节点（class / function / property / extend / typealias 等），含 `impl` 实现与 builder |
| `cfir.expressions` | 表达式节点 |
| `cfir.types` | 类型引用节点（`CfirTypeRef`、`CfirErrorTypeRef` 等） |
| `cfir.references` | 引用节点 |
| `cfir.visitors` | Visitor / Transformer 基类 |
| `cfir.builder` | Builder DSL |
| `cfir.caches` | 节点级缓存 |
| `cfir.diagnostics` | 节点附带诊断模型 |
| `cfir.renderer` | 节点渲染（`CfirRenderer` 与 readability 配置） |

## 生成器

`tree-generator` 是独立 Kotlin 子模块，定义节点 schema（`CfirTree.kt`）并产出全部 `gen/` 文件。详见 [`tree-generator/Readme.md`](tree-generator/Readme.md)。

```bash
./gradlew :cfir:cfir-tree:generateTree    # 重新生成节点
./gradlew :cfir:cfir-tree:compileKotlin
./gradlew :cfir:cfir-tree:test
```

## 手写覆盖点

绝大多数节点不应手写。当前手写的少量例外（不会被生成器覆盖）：

- `CfirErrorTypeRef` / `CfirErrorTypeRefBuilder` / `CfirErrorTypeRefImpl`（接管 diagnostic 模型）

## 设计原则

- `toString()` 统一由 `CfirDeclarationToStringTest` 覆盖，文本输出通过 `CfirRenderer.withReadability().renderElementAsString(this)` 委派渲染，避免叶子类重复拼接。
- 错误节点使用 `ConeSimpleDiagnostic(...)` 装载错误原因，`reason` 读取一律走 `diagnostic.reason`。

## 相关文档

- `tree-generator/Readme.md` — 生成器入口
- `resolve-rollback-plan.md` — CFIR_RESOLVE 迁移回滚预案
- `../../docs/k2-module-alignment.md` — 与 Kotlin K2 `compiler/fir/tree` 对照
