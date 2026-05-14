# cfir/cfir-common/ — CFIR 公共基础设施

CFIR 树与处理链共享的最底层抽象。对齐 Kotlin K2 `compiler/fir/fir-common`。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.common` | 跨子模块的公共抽象（`CfirElement`、source element、模块数据等） |
| `cfir.session` | `CfirSession`：CFIR 处理上下文的根容器 |
| `cfir.session.services` | Session 注册的 components / services（`CfirComponentArrayOwner` 等） |

## 设计要点

- `CfirSession` 是所有 CFIR 处理的统一入口，按 component-array 方式注册并 O(1) 查找服务
- 不依赖具体 IR 树形态，因此 `:cfir:cfir-tree`、`:cfir:raw-cfir:*` 都可以以它为底座

## 依赖

- `:common`、`:common:diagnostics`、`:util`、`:compiler:config`

不依赖 `:psi` 与 `:cfir:cfir-tree`。

## 命令

```bash
./gradlew :cfir:cfir-common:assemble
./gradlew :cfir:cfir-common:test
```

## 相关文档

- `../../docs/k2-module-alignment.md` — 与 Kotlin K2 `fir-common` 对照
- `../../docs/current-module-organization.md` — 在 CFIR 数据模型层中的位置
