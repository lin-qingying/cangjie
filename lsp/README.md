# lsp/ — 仓颉 Language Server 框架

基于 [`lsp4j`](https://github.com/eclipse-lsp4j/lsp4j) 的仓颉 Language Server **框架模块**。提供 LSP 协议的完整脚手架：能力协商、文档同步、工作区状态、与真实分析模块解耦的 `AnalysisFacade` 接缝。

当前**框架层完整**，但真实语义能力通过 `TODO(...)` 占位，便于后续对接 CFIR / Analysis API。

## 关键包

| 包 | 职责 |
|---|---|
| `cangjie.lsp.server` | `CangjieLanguageServer` 主实现，`TextDocumentService` / `WorkspaceService` / `NotebookDocumentService` |
| `cangjie.lsp.capabilities` | 服务器能力描述与协商 |
| `cangjie.lsp.state` | 文档状态仓库、工作区状态管理 |
| `cangjie.lsp.protocol` | 请求执行器、协议适配 |
| `cangjie.lsp.analysis` | `AnalysisFacade` 接缝（对外抽象，未来对接 `:analysis:analysis-api`） |

## testFixtures

提供 LSP 测试共用的辅助。

## 与 IDE 的关系

`:lsp` 是**仓颉端**的 Language Server 实现（如果未来需要把仓颉前端作为 LSP server 暴露给其它编辑器）；
而 `intellij-ide/` 子项目里的 `:modules:ide:lsp` 是 **IDE 客户端**（基于 RedHat LSP4IJ 接入外部 LSP server）。两者方向相反，不冲突。

## 依赖

- `:util`、`:common`
- `lsp4j` 第三方库

## 命令

```bash
./gradlew :lsp:compileKotlin
./gradlew :lsp:test
```

## 相关文档

- `../docs/current-module-organization.md` — LSP 层定位
