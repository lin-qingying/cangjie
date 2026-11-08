# Proposal: 仓颉 Inlay Hints

## 为什么

LSP 协议管线（`textDocument/inlayHint`）已就绪但返回空桩，IDE 插件侧无任何 provider。类型提示、参数名提示是现代语言服务的基础体验，仓颉应与 Kotlin K2 同等对齐：Analysis API（`CaSession`）已具备全部查询能力（类型、解析、渲染），只缺一层与宿主无关的 hint 收集实现。

## 变更内容

- 新建 `:code-insight:hints` 共享模块：宿主无关的 hint 数据模型与收集器（类型 hint、参数名 hint），只产出纯文本 presentation，IDE 与 LSP 各自转换协议对象——与 `code-insight:folding` 同构，符合 completion 提案确立的"IDE 与 LSP 是同一实现的两个消费端"原则。
- `:lsp` 实装 `textDocument/inlayHint`（P0）与 `inlayHint/resolve` 兜底（原样返回），开启 capabilities；P3 实装 resolve 会话存储与懒导航。
- intellij-ide 插件（P2）：声明式 `InlayHintsProvider` 复用同一收集器，providerId/optionId 对齐已预置的 `inlayProviders/` 资源与 `CangJieBundle` 文案。
- deveco 宿主暂不接线，后续单独开任务（见 design.md §8）。

## 影响

- 主仓库：新增 `code-insight/hints` 模块与 2 个 prepare 制品；`lsp` 4 处编辑（facade 接口/Abstract/实现/capabilities + 枚举 + 覆盖测试白名单）。
- intellij-ide：settings substitution、Version Catalog、ide/base 依赖与 provider 注册（P2）。
- 详细设计、分期与验收标准见 `design.md`（v7，经六轮审查）。
