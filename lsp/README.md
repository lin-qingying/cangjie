# lsp/ — 仓颉 Language Server 框架

基于 [`lsp4j`](https://github.com/eclipse-lsp4j/lsp4j) 的仓颉 Language Server **框架模块**。提供 LSP 协议的完整脚手架：能力协商、文档同步、工作区状态、与真实分析模块解耦的 `AnalysisFacade` 接缝。

框架层之外，通过 `AnalysisFacade` 接缝已对接真实 Analysis API 与共享 code-insight 能力（补全、inlay hints、semantic tokens、重命名等），而非 `TODO` 占位。

## 关键包

| 包 | 职责 |
|---|---|
| `cangjie.lsp.server` | `CangjieLanguageServer` 主实现、`TextDocumentService` / `WorkspaceService` / `NotebookDocumentService`、请求执行器（`CangjieRequestExecutor`） |
| `cangjie.lsp.capabilities` | 服务器能力描述与协商 |
| `cangjie.lsp.state` | 文档状态仓库、工作区状态管理 |
| `cangjie.lsp.analysis` | `AnalysisFacade` 接缝（`AnalysisApiCangjieAnalysisFacade` 对接 `:analysis:analysis-api`） |
| `cangjie.lsp.completion` | LSP 补全接入（item 转换、编辑计算、import 编辑、排序） |
| `cangjie.lsp.hints` | inlay hints 转换 |
| `cangjie.lsp.semantic` | semantic tokens 生成 |

## testFixtures

提供 LSP 测试共用的辅助。

## 与 IDE 的关系

`:lsp` 是**仓颉端**的 Language Server 实现（如果未来需要把仓颉前端作为 LSP server 暴露给其它编辑器）；
而 `intellij-ide/` 子项目里的 `:modules:ide:lsp` 是 **IDE 客户端**（基于 RedHat LSP4IJ 接入外部 LSP server）。两者方向相反，不冲突。

## 依赖

- Analysis：`:analysis:analysis-api` / `:analysis:analysis-api-cfir` / `:analysis:analysis-api-impl-base` / `:analysis:analysis-api-standalone` / `:analysis:analysis-api-platform-interface` / `:analysis:macro-expansion` / `:analysis:cj-references`
- Code Insight：`:code-insight:api` / `:code-insight:completion` / `:code-insight:editor` / `:code-insight:code-actions` / `:code-insight:hints` / `:code-insight:refactoring` / `:code-insight:highlighting`（与 IDE 共用的编辑器能力）
- 编译器与宏：`:compiler:config`、`:compiler:frontend`、`:macro:macro-process`（LSP 进程内宏展开）
- 第三方 / 平台：`lsp4j`（+ `jsonrpc`）、`intellijCore()`、IntelliJ `analysis` / `code-style` / `indexing` / `refactoring` / `usage-view`

## 命令

```bash
./gradlew :lsp:compileKotlin
./gradlew :lsp:test
```

