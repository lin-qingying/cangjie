## 为什么

当前 `cfir/raw-cfir/psi2cfir` 的 rawBuilder 测试结果存在可置信性问题：错误输出可能被自动写回 golden，且 `CjBasicType` 在类型转换链路中被系统性降级为 `Unsupported type element`。这导致测试数据不能稳定反映真实语义回归，必须优先修复。

## 变更内容

- 修复 rawBuilder 测试 golden 更新策略：默认严格对比，不允许未显式开启更新模式时改写期望文件。
- 补齐 PSI → Raw CFIR 的 `CjBasicType` 类型引用转换：新增 `CfirBasicTypeRef` 表达基础类型，并明确基础类型在 RAW 阶段即可确定，禁止依赖 resolve 阶段再确定基础类型语义。
- 稳定 lazyBodies by-stub 测试基础设施，避免临时 `.cj` 文件被识别为 `UNKNOWN` 而污染结果。
- 增强 rawBuilder 测试覆盖护栏，降低 testData 漏测风险。

## 功能 (Capabilities)

### 新增功能
- `rawbuilder-test-reliability`: 提供可控的 golden 更新、稳定的类型转换输出与更可靠的 rawBuilder/lazyBodies 结果校验。

### 修改功能
- `raw-cfir-implementation`: 调整 rawBuilder 测试与类型转换相关需求，确保测试结果可复现、可验证且不被误回写掩盖。

## 影响

- 模块：`cfir/raw-cfir/psi2cfir`（testFixtures、tests-gen、testData 断言逻辑）、`cfir/raw-cfir/raw-cfir-common`（如需共享更新策略）、`psi`（`CjBasicType` 与 stub/file type 相关链路）。
- 测试：rawBuilder、lazyBodies(by-ast/by-stub)、基础设施测试将出现可预期差异，需要更新期望文件与稳定性断言。
- 文档：`README.md` 与 OpenSpec `raw-cfir-implementation` 规范需与更新策略一致。
