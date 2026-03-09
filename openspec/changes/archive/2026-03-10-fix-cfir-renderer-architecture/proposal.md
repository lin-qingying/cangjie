## 为什么

当前 `CfirRenderer` 的实现形态与职责边界存在偏差：实现采用单体 Visitor + 静态 `render()` 入口，仓库内实际调用几乎仅在 Raw CFIR 测试夹具中，导致其被固化为 golden file 对比工具，而不是可复用的 CFIR 可视化基础设施。随着 `CFIR_RESOLVE`、诊断与调试能力推进，需要一个可配置、可组合、可在测试与非测试场景共享的渲染框架。

## 变更内容

- 重新定义 `CfirRenderer` 的定位：从“测试导向的文本 dump”升级为“通用 CFIR 渲染基础设施”。
- 对齐 Kotlin `FirRenderer` 的关键设计思想，引入组件化渲染能力（按声明、类型、引用、阶段信息等职责拆分），支持通过配置组合不同输出策略。
- 保留并兼容现有 golden file 输出能力，新增“可读性导向”和“调试导向”等预置渲染配置；`CfirRenderer.render(element)` 必须继续可用并委托到 golden 兼容 profile，确保现有 golden 输出基线保持不变。
- 将测试中的渲染调用迁移到显式的测试配置入口，避免“默认实现 == 仅供 golden 对比”的语义耦合。
- 补充覆盖测试，验证：
  - 现有 golden 输出不回归；
  - 新配置在非 golden 场景可复用；
  - 组件组合行为符合预期。

## 功能 (Capabilities)

### 新增功能
- `cfir-renderer-architecture`: 建立可组合、可配置、可复用的 CFIR 渲染架构，支持测试、调试和诊断等多场景输出。

### 修改功能
- `raw-cfir-implementation`: 将该能力中的“CFIR 渲染”要求从“仅 Golden File 比对输出”扩展为“在保证 golden 稳定的前提下支持多场景复用的渲染能力”。

## 影响

- 受影响代码：`cfir/cfir-tree`（renderer 实现）、`cfir/raw-cfir/psi2cfir`（测试夹具与 golden 流程）及相关测试文件。
- 受影响规范：`openspec/specs/raw-cfir-implementation/spec.md`（需求语义扩展）；新增 `openspec/changes/fix-cfir-renderer-architecture/specs/cfir-renderer-architecture/spec.md`。
- 兼容性：默认 golden 输出需保持字节级兼容；对外不引入破坏性 API 变更。
