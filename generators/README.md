# generators/ — 代码生成框架

通用代码生成基础设施，供项目内各类 tree-generator / checkers-component-generator / FlatBuffers 生成器复用。对齐 Kotlin K2 `compiler/util` 中的 generator 抽象。

## 关键包

| 包 | 职责 |
|---|---|
| `cangjie.generators.tree` | 树节点定义模型（fields / kinds / inheritance） |
| `cangjie.generators.tree.config` | 生成器配置 |
| `cangjie.generators.tree.imports` | Import 收集与渲染 |
| `cangjie.generators.tree.printer` | 输出 Kotlin 代码的 printer |
| `cangjie.generators.util` | 生成器通用工具 |

## 使用方

- `:cfir:cfir-tree:tree-generator` — CFIR 节点生成
- `:cfir:checkers:checkers-component-generator` — Checkers 组件骨架生成
- `:analysis:analysis-api-cfir:analysis-api-cfir-generator` — Analysis API CFIR 后端组件生成
- `:compiler:frontend-arguments-generator` — 前端参数生成
- `:flatbuffers-gen` — FlatBuffers schema 处理

## 设计原则

- 提供**通用**的树定义 + 节点字段 + visitor / builder / impl 生成能力
- 具体生成产物由各使用方的子模块（`*-generator`）通过定义 `Tree` 等 DSL 描述

## 依赖

- `:util`、`:common`

## 命令

```bash
./gradlew :generators:assemble
./gradlew :generators:test
```

## 相关文档

- `../cfir/cfir-tree/tree-generator/Readme.md` — CFIR 节点生成器使用
