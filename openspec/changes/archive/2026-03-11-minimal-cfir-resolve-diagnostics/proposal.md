## 为什么

当前仓库已具备 CFIR Resolve 的阶段枚举与处理器骨架，但缺少可执行的解析入口与诊断测试链路，导致后续语义阶段无法被持续验证。本变更以“仅框架/空诊断”为最小范围，先打通可运行的阶段与测试入口。

## 变更内容

- 新增最小的 CFIR_RESOLVE 流程入口（Resolve Facade + 处理器驱动），允许在不做语义解析的情况下执行到 CHECKERS。
- 新增最小 DiagnosticsHandler 测试入口，支持空诊断的稳定输出与校验。

## 功能 (Capabilities)

### 新增功能
- `cfir-resolve-diagnostics`: 定义最小 CFIR_RESOLVE 诊断入口的行为规范（允许空诊断，但必须提供可运行的测试与输出）。

### 修改功能
- `compiler-architecture`: 补充/澄清 CFIR_RESOLVE 的最小可运行入口与诊断测试链路要求。

## 影响

- 受影响模块：`cfir`（resolve 入口对接）、`analysis-api-cfir`（resolve facade 接入点）、`tests:test-infrastructure`（新增 DiagnosticsHandler 测试入口）。
- 受影响测试：新增最小诊断测试样例与输出规范（空诊断允许）。
