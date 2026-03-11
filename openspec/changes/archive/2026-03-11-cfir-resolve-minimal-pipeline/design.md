## 上下文

当前仓库已具备 `CfirResolvePhase` 枚举、`CfirResolveProcessor` 接口与 `CfirTotalResolveProcessor` 调度器，但没有实际阶段处理器注册与诊断接入。现有 `MinimalResolveDiagnosticsPipelineTest` 为最小 smoke test，未接入 test-infrastructure 的标准配置管线。

## 目标 / 非目标

**目标：**
- 提供可运行的最小 CFIR_RESOLVE 处理器链（IMPORTS → TYPES → STATUS → CHECKERS）。
- 引入最小可用 provider/symbol provider 以保证解析流程能推进阶段。
- 将 resolve 诊断处理接入 test-infrastructure，形成可复用的 DiagnosticsHandler，并新增诊断用例。

**非目标：**
- 不实现完整类型推断、重载解析或所有诊断规则。
- 不引入新的外部依赖或大规模重构 resolve 架构。

## 决策

- **使用现有 `CfirTotalResolveProcessor` 作为唯一调度入口。**
  - 备选方案：新增独立的解析入口类。
  - 取舍：复用已有调度逻辑，减少架构扩展面。
- **CHECKERS 阶段产出最小诊断集合。**
  - 备选方案：在各阶段直接报告诊断。
  - 取舍：与 Kotlin FIR 模式一致，保持诊断集中在检查阶段。
- **提供最小 provider 实现以支持 resolve 流程。**
  - 备选方案：resolve 直接依赖未来完整 provider 实现。
  - 取舍：降低最小链路的耦合，保证阶段推进不被阻塞。
- **测试接入复用 test-infrastructure 的 `TestFacade` + `AnalysisHandler`。**
  - 备选方案：保留单独的手写测试流程。
  - 取舍：统一测试管线，便于扩展诊断测试矩阵。

## 风险 / 权衡

- 最小 provider 可能掩盖真实语义缺失 → 在设计中明确仅用于最小链路，后续替换为完整 provider。
- 诊断规则过少导致测试价值有限 → 先定义稳定输出格式，后续可逐步扩展规则。
