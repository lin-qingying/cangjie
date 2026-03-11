## 上下文

当前 CFIR Resolve 相关类型已存在（`CfirResolvePhase`、`CfirTotalResolveProcessor`、`CfirDiagnostic*`），但缺少可运行的 resolve facade 与诊断测试入口，导致“阶段可执行性”无法被持续验证。本变更限定为“仅框架/空诊断”，目标是打通最小链路而不引入语义复杂度。

## 目标 / 非目标

**目标：**
- 提供最小 CFIR_RESOLVE 流程入口与诊断收集链路。
- 为 test-infrastructure 增加最小 DiagnosticsHandler 测试入口，允许空诊断输出并可稳定对比。

**非目标：**
- 不实现 IMPORTS/TYPES/BODY_RESOLVE 等真实语义解析。
- 不新增具体诊断规则，仅验证流程。
- 不修改 Raw CFIR 构建或 PSI 层行为。

## 决策

- 选择“空诊断可验证”的最小路径，以确保测试链路在不引入语义复杂度的情况下先行落地。
- 复用现有 `TestFacade`/`AnalysisHandler` 管线作为诊断入口，避免引入并行测试体系。
- 以 change-scoped spec 描述新增能力，并在 `compiler-architecture` 变更能力中补足最小入口要求，保证架构层与实现层边界清晰。

## 风险 / 权衡

- [风险] 空诊断可能被误解为“没有进展” → **缓解**：规范与测试明确“空诊断即成功”。
- [风险] 后续引入真实语义时接口不匹配 → **缓解**：保持 facade/processor 接口薄且与 `CfirResolvePhase` 对齐。
- [风险] 诊断输出格式未来变更 → **缓解**：先定义稳定的最小输出格式作为基线。
