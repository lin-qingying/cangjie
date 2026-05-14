# analysis/analysis-internal-utils/ — Analysis 模块内部工具

对齐 Kotlin `analysis/analysis-internal-utils`。
仅在 `:analysis:*` 子模块之间共用的工具与抽象，**不**对外暴露（不打包进发布工件的 API 表面）。

## 关键包

| 包 | 职责 |
|---|---|
| `analysis.internal.projectStructure` | 项目结构内部抽象 |
| `analysis.utils.caches` | 缓存工具 |
| `analysis.utils.errors` | 错误处理工具 |

## 使用约束

- 不允许 `:analysis:*` 之外的模块依赖本模块
- 暴露给外部的能力应放到 `:analysis:analysis-api` 或 `:analysis:analysis-api-impl-base`

## 命令

```bash
./gradlew :analysis:analysis-internal-utils:assemble
./gradlew :analysis:analysis-internal-utils:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
