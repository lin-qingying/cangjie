## 为什么

当前 CfirSessionConstructionUtils 仅支持单模块路径，并且未使用可注入的 session 构建回调，导致多模块与 metadata 场景无法对齐 Kotlin 的 session 构建模型。随着 CFIR 构建与解析推进，需要一个可扩展的多模块/metadata 会话构建入口以支撑后续阶段。

## 变更内容

- 重写 CfirSessionConstructionUtils.prepareSessions，支持多模块与 metadata 会话构建，并按模块返回 SessionWithSources。
- 对齐 Kotlin SessionConstructionUtils 的核心构建路径（共享库会话、库会话、源会话的分层），但不引入多平台分支。
- 明确使用注入的 session 生产回调与扩展注册器，实现可插拔会话构建。

## 功能 (Capabilities)

### 新增功能
- `cfir-session-construction-multimodule-metadata`: 提供多模块与 metadata 语义下的 CFIR 会话构建行为规范。

### 修改功能

<!-- 无现有规范需要修改 -->

## 影响

- `cfir/entrypoint` 的会话构建入口及其与 `CfirDefaultSessionFactory` 的协作方式。
- CLI 前端流水线的会话构建与后续 CFIR 构建/解析路径。
- 会话构建相关的扩展注册器与 .cjo 管理器注入点。
