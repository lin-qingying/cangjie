# compiler/frontend/ — 前端基础设施与管线协调

前端的"驱动层"。组装 `:psi`、`:cfir:*`、`:macro:*` 等模块成完整的编译管线，对外暴露统一的环境与入口。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.frontend.environment` | 编译环境装配（继承 / 复用 IntelliJ Application、Project、PSI Factory） |
| `org.cangnova.cangjie.frontend.pipeline` | 编译管线编排（阶段串联、Phase 推进） |
| `org.cangnova.cangjie.frontend.arguments` | 前端参数处理 |
| `org.cangnova.cangjie.frontend.sources` | 源码输入抽象（`CONTENT_ROOTS` 模型，对齐 Kotlin 的 Content Roots） |
| `org.cangnova.cangjie.extensions` | 前端扩展点 |

## 源码输入

统一使用 `CONTENT_ROOTS` 作为源码输入入口（对齐 Kotlin Content Roots）。`CLI_SOURCE_FILE_PATHS` 已进入弃用周期，仅用于兼容历史脚本。

## 依赖

- `:compiler:config`、`:compiler:phaser`、`:compiler:arguments`
- `:common`、`:common:diagnostics`、`:util`
- `:psi`、`:cfir:cfir-common`、`:cfir:entrypoint`（按需）

## 命令

```bash
./gradlew :compiler:frontend:assemble
./gradlew :compiler:frontend:test
```

## 测试

- 接入 `:tests:test-infrastructure`
- 测试入口基类 `AbstractCangjieCompilerTest`，支持通过 `-Dcangjie.slow.assertions=true` 启用 `resolution.common` 的 slow assertions

## 上游接入

本模块由 `:prepare:frontend` / `:prepare:frontend-embeddable` 聚合为发布工件 `cangjie-frontend` / `cangjie-frontend-embeddable`。

## 相关文档

- `../../cjfir-compiler-stages.md` — 完整阶段设计
- `../../docs/current-module-organization.md` — 当前实装模块全景
- `../../TESTING_CONVENTIONS.md` — 全项目测试约定
