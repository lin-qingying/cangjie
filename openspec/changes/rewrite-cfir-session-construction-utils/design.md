## 上下文

当前 CfirSessionConstructionUtils 仅支持单模块构建流程，并且未使用外部注入的 session 生产回调，导致会话构建缺少可扩展性。参照 Kotlin SessionConstructionUtils 的核心分层（shared library / library / source），需要在仓颉前端中引入多模块与 metadata 语义下的会话构建逻辑，但不引入多平台分支。利益相关者包括 CFIR entrypoint 与 CLI pipeline 的调用方，以及后续基于多模块/metadata 的前端阶段。

## 目标 / 非目标

**目标：**
- 提供与 Kotlin 结构对齐的多模块会话构建路径，支持 metadata 会话构建与模块级 SessionWithSources 输出。
- 明确使用注入的 createSharedLibrarySession/createLibrarySession/createSourceSession 回调，保持可插拔性。
- 在不引入多平台分支的前提下，抽取可重用的会话构建流程与模块数据构建方式。

**非目标：**
- 不实现 Kotlin 中 JS/Native/Wasm 等多平台 session 构建分支。
- 不引入脚本会话（script session）与脚本编译路径。
- 不改变现有编译阶段或新增新的外部依赖。

## 决策

- **采用 Kotlin 的“分层会话”结构（shared library → library → source）并保持单平台化。**
  - 原因：保持结构可扩展，同时避免多平台带来的复杂度。
  - 备选：继续维持单一 source session。缺点是无法支持多模块与 metadata 语义。

- **以“模块为单位”输出 SessionWithSources，并显式构建 CfirSourceModuleData 图。**
  - 原因：多模块与 metadata 行为需要模块级数据建模。
  - 备选：按文件分组返回单一 session，会限制后续模块级解析与诊断。

- **注入式 session 生产回调作为唯一构建入口，CfirDefaultSessionFactory 仅作为默认实现。**
  - 原因：对齐 Kotlin 的可插拔构建风格并消除当前参数闲置问题。
  - 备选：内部强制使用默认工厂，扩展性差且与现有签名不一致。

## 风险 / 权衡

- [多模块模型引入后调用方需要调整] → 在 CLI pipeline 中提供向后兼容的单模块调用路径。
- [metadata 会话构建细节尚不完整] → 先聚焦 metadata 会话的最小可用路径并在 specs 中明确行为边界。
- [模块数据依赖图容易不一致] → 规范化 moduleData 构建与依赖传入方式，集中在 CfirSessionConstructionUtils 内部完成。
