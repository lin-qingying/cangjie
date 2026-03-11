## 为什么

当前 CFIR_RESOLVE 只有阶段枚举与调度框架，缺少真正的阶段处理器与测试接入，导致语义解析与诊断无法被验证。
为了让解析链路可运行、可测试，需要尽快建立最小可用的 resolve 流水线与诊断测试基础。

## 变更内容

- 新增最小 CFIR_RESOLVE 处理器链（IMPORTS → TYPES → STATUS → CHECKERS），并在 session 中注册。
- 引入最小的 provider/symbol provider 实现以支撑解析阶段运行。
- 将 CfirResolveFacade + DiagnosticsHandler 接入现有 test-infrastructure，新增诊断测试用例与 golden 文件。

## 功能 (Capabilities)

### 新增功能
- `cfir-resolve-minimal-pipeline`: 提供可运行的最小 CFIR_RESOLVE 阶段处理与解析推进能力。
- `cfir-diagnostics-test-integration`: 在测试基础设施中接入 resolve 诊断处理与 golden 校验。

### 修改功能
-

## 影响

- `cfir/cfir-tree`：resolve 处理器、诊断管线与 session 组件注册。
- `analysis/analysis-api-cfir`：resolve facade 可能需要新的 session 初始化入口。
- `tests/test-infrastructure`：新增 DiagnosticsHandler 与 TestFacade 接线。
- `cfir/cfir-tree/testData`：新增/扩展诊断测试数据与 golden 文件。
