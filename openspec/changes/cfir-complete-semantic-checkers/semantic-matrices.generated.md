# CFIR V5 semantic matrices

本文件由 `validate-v5-matrices.ps1 -GenerateMarkdown` 从同目录五个 JSON 矩阵生成。JSON 是唯一事实源；本 Markdown 只用于审查和缺口说明。

## 机器校验规则

- `annotation-catalog.json` 必须覆盖 24 个官方 `CangjieAnnotationKind`，并把 `CUSTOM`、compile-time-visible custom、system macro、special expression、platform-derived、unknown 保留为 identity state，而不是 builtin enum kind。
- `ffi-semantics-matrix.json` 必须覆盖 foreign、`@C`、calling convention、CType、CPointer/CString/CFunc、C struct、conversion、unsafe、inout、Java、ObjC、CJMP、ABI。
- `cfir-phase-state-matrix.json` 必须为每个 phase 指定唯一 owner、输入输出、最低读取 phase、lazy/reentrancy、失败终态、invalidation 和重复诊断策略。
- `path-capability-matrix.json` 必须逐条覆盖 source、PSI、LightTree、CJO、stub、decompiled、Analysis API、CHIR、backend adapter。
- `producer-consumer-matrix.json` 必须为每个跨层事实指定唯一 producer、结构化表示、consumer 和禁止的反向推断路径。

## 当前实现状态

矩阵只证明“范围已登记且字段完整”，不把登记当成语义完成。每个条目的 `officialOwner`、`requiredPaths`、`phase` 和 `consumer` 都是后续实现/验证的硬契约；未完成的条目仍必须保留在矩阵中，不能删除来制造覆盖率。

## 缺口说明

1. annotation catalog 已建立公共身份模型，但 Overflow 的表达式级传播、When 的条件编译状态、Java/ObjC/CJMP 生成图、CJO/stub/Analysis API 全路径仍需按矩阵逐项实现和验证。
2. FFI 当前已接通 CFunc 构造、CType 基础判断和部分调用检查；CPointer 泛型反向推断、foreign block 收集、C struct 全约束、CFunc CHIR typed representation、backend link/load/run 仍是独立缺口。
3. IfAvailable 已拥有专用 PSI/CFIR 节点、`() -> Unit` 分支入口和 API/syscap scope seam；desugar/weak-link metadata/CHIR/backend consumer 仍需完成端到端验收。
4. phase matrix 要求失败状态可观察、每个状态唯一 producer、重复 resolve 可重入且可 invalidation；现有树上部分声明 phase 已具备 `whileAnalysing`，annotation call 自身状态和并发测试仍需补齐。
5. path matrix 中 CJO、stub、decompiled、Analysis API、CHIR 和 backend adapter 不能用 source-only round-trip 替代；没有 source writer 的路径只能做明确的 lossy projection/metadata validation。

## 验证命令

```powershell
pwsh -NoLogo -NoProfile -File .\openspec\changes\cfir-complete-semantic-checkers\validate-v5-matrices.ps1
pwsh -NoLogo -NoProfile -File .\openspec\changes\cfir-complete-semantic-checkers\validate-v5-matrices.ps1 -GenerateMarkdown
```
