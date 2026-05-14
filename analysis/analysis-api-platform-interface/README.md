# analysis/analysis-api-platform-interface/ — Analysis API 平台接口抽象

对齐 Kotlin `analysis/analysis-api-platform-interface`。
定义 Analysis API 对**平台**（IntelliJ / Standalone / 其它）的所有契约，是 `:analysis:analysis-api` 与具体平台实现（IntelliJ host plugin、`analysis-api-standalone` 等）之间的边界。

## 关键包

`org.cangnova.cangjie.analysis.api.platform.*` — 平台契约抽象（模块结构 / 项目结构 / 生命周期 / 锁 / 资源等）。

## 设计原则

- 上层只依赖本模块接口，不耦合具体平台
- 平台实现需要满足本模块定义的全部契约
- 仓颉独有契约新增到本模块时，需要文档化与 Kotlin 的偏离

## 调用方

- `:analysis:analysis-api` — 接口层依赖
- `:analysis:analysis-api-impl-base` — 实现基础层
- `:analysis:analysis-api-cfir` / `:analysis:analysis-api-standalone` — 后端实现
- `intellij-ide/` — IDE 端实现

## 命令

```bash
./gradlew :analysis:analysis-api-platform-interface:assemble
./gradlew :analysis:analysis-api-platform-interface:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../docs/k2-module-alignment.md` — Kotlin K2 对照
