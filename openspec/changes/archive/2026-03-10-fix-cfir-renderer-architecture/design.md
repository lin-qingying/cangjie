## 上下文

当前 `cfir/cfir-tree/src/org/cangjie/cfir/renderer/CfirRenderer.kt` 采用单体实现：私有构造 + `companion object render(element)` 静态入口，内部把声明、类型、引用、表达式等渲染逻辑耦合在一个 Visitor 中。该实现在仓库中的直接调用主要位于 `cfir/raw-cfir/psi2cfir/testFixtures` 的 golden file 测试夹具，因此其默认语义被“锁定”为测试对比工具，而不是可复用的编译器基础设施。

与此同时，Kotlin `FirRenderer` 已采用组件化架构：声明/注解/类型/phase/符号等能力解耦为可组合组件，并提供多个预设工厂（例如 `withResolvePhase()`、`forReadability()`）。这使同一渲染器可以服务测试、调试、分析 API 输出等场景。

本变更的约束是：
- 不能破坏现有 raw-cfir golden 文件稳定性；
- 需要为后续 `CFIR_RESOLVE` 与诊断开发提供可扩展输出能力；
- 变更应遵循仓库的 interface-first 与模块边界约定。
- 当前测试基础设施虽声明了 `DUMP_CFIR`/`DUMP_CHIR` 指令，但现状以 raw-cfir 测试夹具直接调用 `CfirRenderer` 为主，尚未形成统一 handler 管线；本变更应先解决核心渲染能力，再避免一次性引入过重注册机制。

## 目标 / 非目标

**目标：**
- 将 `CfirRenderer` 升级为可配置、可组合的渲染框架，而非单用途测试工具。
- 引入渲染配置（profile）与组件接口，支持至少三类场景：golden-compat、debug、readability。
- 保持现有测试入口兼容：`CfirRenderer.render(element)` 仍可工作，并默认产生与历史字节级一致的 golden 输出。
- 为阶段信息（如 resolve phase）与语义信息（resolved reference/type）预留可扩展渲染点。

**非目标：**
- 不在本变更中重写全部 CFIR 节点的文案风格。
- 不引入跨模块的诊断协议改造。
- 不修改 `external/` 参考实现，也不要求 1:1 复制 Kotlin FIR 全部组件类型。

## 决策

### 决策 1：采用“核心渲染器 + 组件接口 + 预置 profile”架构

**选择**：
- 新增 `CfirRendererComponents`（组件集合）与若干细分接口（例如 declaration/type/reference/expression renderer）；
- `CfirRenderer` 负责访问流程与打印器编排，具体渲染由组件完成；
- 提供统一 `withXxx()` 风格工厂：`withGoldenCompat()`、`withDebug()`、`withReadability()`。
- 引入轻量 `CfirPrinter` 抽象封装缩进/换行，替代在主类内直接散布 `StringBuilder` 细节。
- 提供实例渲染入口（例如 `renderElementAsString(element)`），并保持静态兼容入口委托到实例实现。

**原因**：
- 对齐 Kotlin `FirRenderer` 的可组合思路，降低“一个类承载全部策略”的演进成本；
- 可在不破坏默认输出的前提下扩展非测试场景。

**备选方案**：
- 方案 A：保留单体类，仅增加参数开关。问题是开关数量会持续膨胀，职责仍然耦合。
- 方案 B：拆分多个独立 renderer 类（TestRenderer/DebugRenderer/ReadableRenderer）。问题是重复逻辑多，难以复用。

### 决策 2：保留 `CfirRenderer.render(element)` 作为兼容入口

**选择**：
- 旧入口内部改为委托到 `withGoldenCompat()` profile。

**原因**：
- 最小化调用方改动，避免一次性修改全部测试夹具；
- 使迁移可分阶段进行。

**备选方案**：
- 直接删除静态入口并强制调用方显式构造 profile，短期迁移成本高且风险大。

### 决策 3：将测试场景的策略显式化

**选择**：
- 在 `raw-cfir` 测试夹具中引入明确命名的渲染 profile 调用（例如 `CfirRenderer.goldenCompat().renderElementAsString(...)` 或等价 API）。

**原因**：
- 将“测试需求”与“通用能力”解耦，避免默认语义继续被测试场景绑架。

**备选方案**：
- 继续沿用默认 `render()`，仅在文档中说明。无法从代码层面表达边界，不利于长期维护。

### 决策 4：渲染配置采用“preset 优先，options 兜底”而非 registry-first

**选择**：
- 第一阶段以 preset profile（golden/debug/readability）为主；
- 本次不引入 `CfirRendererOptions`，避免在需求尚未稳定时提前扩展配置面；
- 若后续出现明确且稳定的多开关需求，再补充 `CfirRendererOptions`（可参考 `withOptions {}` + lock/copy 模式）；
- 本变更不引入诊断系统那类 registry/provider 级扩展框架。

**原因**：
- 与 Kotlin `FirRenderer` 的演进路径一致，先组件化再细化配置；
- 与仓库既有 `DescriptorRenderer` 经验一致，避免在需求尚不复杂时过度架构。

**备选方案**：
- 直接做可注册渲染器平台。问题是复杂度高、落地慢，且当前场景主要在编译器内部与测试流程，收益不足。

## 风险 / 权衡

- **风险：golden 输出回归** → 通过 golden-compat profile + 回归测试确保输出字节级稳定（必要时新增过渡比较工具）。
- **风险：组件拆分初期增加复杂度** → 先拆分最核心能力（declaration/type/reference），其他能力保留在默认实现并逐步迁移。
- **风险：API 过度设计** → 仅暴露当前明确需要的组件接口，避免一次性复制 Kotlin 的全部 renderer 组件矩阵。
- **风险：把测试指令接线与 renderer 重构耦合在同一变更** → 本次仅保证测试夹具迁移与输出稳定；统一 handler 接线作为后续独立优化项。

## 迁移计划

1. 引入组件接口与 profile API，保持旧静态入口可用。
2. 将现有单体逻辑迁移到默认组件实现，确保 `render()` 输出不变。
3. 在 `raw-cfir` 测试夹具切换到显式测试 profile。
4. 增加非 golden 的渲染测试（debug/readability）验证可复用性。
5. 若发现输出不兼容，回滚到旧入口实现并仅保留 profile 框架骨架（不影响现有测试）。

## 待定问题

- 无（关键决策已确认：resolve phase 仅做框架预留；profile 统一 `withXxx()`；`CfirRendererOptions` 本次不落地；实例渲染 API 与 `CfirPrinter` 本次一并落地）。
