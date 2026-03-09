## 上下文

当前 rawBuilder 相关测试存在三类相互耦合的问题：
1) golden 断言在不匹配时可能改写期望文件，导致错误输出被固化；
2) `CjBasicType` 未被完整转换为可用的 Raw CFIR 类型引用，导致大量 `Unsupported type element`；
3) lazyBodies by-stub 场景对临时物理文件与 file type/stub 构建链路敏感，出现 `UNKNOWN` 文件类型后会放大不稳定性。

这些问题跨越 `psi`、`raw-cfir` 与测试基础设施，若不一起收敛，将持续产生“测试通过但语义错误”或“测试不稳定难复现”的双重风险。

## 目标 / 非目标

**目标：**
- 建立默认“只比较不改写”的 golden 校验策略，仅在显式更新模式下改写期望文件。
- 在 PSI → Raw CFIR 转换中完整支持 `CjBasicType`，并将“基础类型在 RAW 阶段可确定”作为硬约束，使输出稳定为可解析类型引用。
- 稳定 by-stub lazyBodies 测试输入链路，确保 `.cj` 文件被一致识别并可构造 stub。
- 补足 rawBuilder 测试覆盖护栏，减少 testData 漏测与隐性回归。

**非目标：**
- 不在本变更中扩展 cfir-resolve/类型推断语义。
- 不重写整个 test-infrastructure 框架。
- 不引入与本问题无关的大规模重构（例如全面替换 tests-gen 机制）。

## 决策

### 决策 1：将 golden 更新改为显式开关驱动（默认严格）
- 方案：在 rawBuilder 断言逻辑中读取统一更新标志（与仓库文档约定一致），仅当开关开启时写回期望文件；否则仅失败并输出差异。
- 备选方案 A：保持自动写回并依赖失败提示人工回滚。拒绝原因：会持续污染 testData，回归信号失真。
- 备选方案 B：每个测试类各自实现更新逻辑。拒绝原因：策略分散，容易再次漂移。

### 决策 2：新增 `CfirBasicTypeRef` 表达基础类型（Raw 阶段）
- 方案：在类型系统中新增 `CfirBasicTypeRef`，在 `PsiConversionUtils` 的类型分派中为 `CjBasicType` 输出该节点（名称来自 `CjBasicType.getName()`）。
- 约束：`CjBasicType` 的识别与映射必须在 RAW 构建阶段完成，不得将“是否为基础类型”的判断延后到 resolve 阶段。
- 备选方案 A：仍输出 `CfirErrorTypeRef`，待 resolve 修复。拒绝原因：rawBuilder golden 在构建阶段即失真，不利于定位问题。
- 备选方案 B：复用 `CfirUserTypeRef`。拒绝原因：会模糊基础类型与普通用户类型的语义边界，不利于后续类型系统扩展与诊断精准性。

### 决策 3：by-stub 基础设施以“真实 file type + physical provider + stub 可用”作为硬约束
- 方案：复用 Kotlin 同类测试思路，确保替换后的 provider 仍使用正确 file type，并对 `isPhysical`、`stub != null` 与禁止 tree access 进行断言。
- 备选方案 A：绕过 by-stub，仅保留 by-ast。拒绝原因：丢失关键懒构建验证维度。
- 备选方案 B：放宽断言容忍 UNKNOWN。拒绝原因：掩盖基础设施缺陷。

### 决策 4：为 tests-gen 增加全量覆盖校验护栏
- 方案：在生成测试中补充 all-files-present 风格校验或等效机制，保证新增 `.cj` 必须被执行。
- 备选方案：仅靠 `testSmoke`。拒绝原因：无法防漏测。

## 风险 / 权衡

- [风险] 开启严格模式后短期会暴露大量历史污染的 golden 差异 → **缓解**：分批更新并在 PR 描述中区分“机制修复”与“语义变化”。
- [风险] `CjBasicType` 修复会引发大批期望文本变化 → **缓解**：先做小样本验证（如 `topLevelFunction`、`controlFlow`），确认模式后批量更新。
- [风险] by-stub 稳定化可能受 IntelliJ 平台行为差异影响 → **缓解**：保留基础设施专测，覆盖 Windows/CI 场景。
- [权衡] 新增 `CfirBasicTypeRef` 会带来渲染器、访问者与测试基线的同步改动成本，但能保留基础类型语义边界并减少后续返工。

## 迁移计划

1. 先改测试更新策略，确保后续失败不会继续污染 golden。
2. 再引入 `CfirBasicTypeRef` 并修复 `CjBasicType` 转换，随后更新受影响 testData（rawBuilder + lazyBodies）。
3. 稳定 by-stub 基础设施并通过专测验证。
4. 最后加覆盖护栏并执行模块级测试回归。

回滚策略：
- 若策略改动导致测试流程不可用，可临时回滚到旧断言实现（仅回滚机制改动，不回滚语义修复），保持最小可运行。

## 开放问题

- 仓库最终统一采用 `-Dupdate.test.data=true` 还是 `-Pkotlin.test.update.test.data=true` 作为标准入口，需要在文档与实现层明确单一真源。
- 是否需要在 `raw-cfir-implementation` 规范中引入“测试结果可信性”独立 requirement，以约束未来测试基础设施变更。
