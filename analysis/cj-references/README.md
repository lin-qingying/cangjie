# analysis/cj-references/ — 仓颉引用与查找

对齐 Kotlin `compiler/idea/references` 的部分能力。提供仓颉 PSI 上的引用解析、查找、用法搜索支持。

## 关键包

| 包 | 职责 |
|---|---|
| `analysis.references` | 引用抽象（与 analysis API 协同） |
| `idea.references` | IDE 引用具体实现 |
| `idea.search` | 用法搜索 / 符号搜索 |
| `references` | 引用工具入口 |

## 调用方

- `intellij-ide/` — IDE 中"跳转到定义"、"查找用法"、refactor 等功能
- `:analysis:analysis-api-cfir` — 引用解析后端

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`
- `:psi`
- `:cfir:cfir-tree`（用于引用目标）

## 命令

```bash
./gradlew :analysis:cj-references:assemble
./gradlew :analysis:cj-references:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
