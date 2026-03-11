## 为什么

当前仓库虽然已经有 `CfirResolvePhase` 的完整阶段枚举和可运行的 resolve 骨架，但实现主体仍以 `CfirMinimal*` 处理器与合成测试入口为主，尚未形成“可对齐官方仓颉语义”的完整 CFIR_RESOLVE 阶段。  
这使得 `CFIR_BUILD -> CFIR_RESOLVE -> Analysis API` 的关键语义链路长期处于半成品状态，无法支撑后续 FINALIZE/CHIR 与 IDE 语义能力稳定推进，因此现在必须进行完整实现而不是继续最小化补丁。

## 变更内容

- 将 `CfirResolvePhase` 全阶段（`IMPORTS/SUPER_TYPES/TYPES/STATUS/EXTENSIONS/IMPLICIT_TYPES/BODY_RESOLVE/CHECKERS`）从“可推进骨架”升级为“完整语义实现”，覆盖声明头、类型解析、隐式推断、函数体解析与最终检查。
- 保留 Kotlin K2/FIR 风格框架（phase pipeline、session component、provider/symbol provider、lazy/total resolve 入口），但每个阶段的规则、诊断与边界行为以仓颉官方编译器 `external/cangjie_compiler/src/Sema` 为语义基准。
- 将当前 `CfirMinimalResolveProcessors`/`MinimalResolveDiagnosticsPipelineTest` 的定位降级为历史兼容或迁移层，主路径切换到正式 resolve 处理器与正式测试矩阵。
- 建立覆盖完整语义的测试体系：从合成 case 扩展到真实 `.cj` 输入、阶段断言、诊断 golden、跨模块/导入/extend/泛型/表达式语义用例，并对 Analysis API 的 `CaCfirResolveFacade` 行为做端到端验收。
- 对 `CfirResolveComponentsRegistrar`、provider 体系与诊断管线进行正式化收敛，避免 “空 provider + 仅推进 phase” 被误用为完成态。

## 功能 (Capabilities)

### 新增功能
- `cfir-resolve-complete-pipeline`: 定义并交付 CFIR_RESOLVE 全阶段完整实现（非 mini 版本），包含阶段间依赖、状态推进、懒加载与全量模式一致性契约。
- `cfir-resolve-cangjie-semantic-parity`: 定义与官方仓颉编译器语义对齐规则，覆盖 extend、继承、类型检查、推断与函数体语义的关键行为一致性。
- `cfir-resolve-full-diagnostics-and-tests`: 定义完整诊断与测试能力，包括阶段行为测试、语义诊断 golden、回归与稳定性要求。
- `cfir-resolve-imports-phase`: 定义 IMPORTS 阶段的导入绑定、冲突处理与可见性行为。
- `cfir-resolve-super-types-phase`: 定义 SUPER_TYPES 阶段的继承图构建与继承合法性检查。
- `cfir-resolve-types-phase`: 定义 TYPES 阶段的显式类型引用解析与错误恢复。
- `cfir-resolve-status-phase`: 定义 STATUS 阶段的修饰符、可见性与声明状态规范化。
- `cfir-resolve-extensions-phase`: 定义 EXTENSIONS 阶段的 extend 语义全量规则（含孤儿规则与特化冲突）。
- `cfir-resolve-implicit-types-phase`: 定义 IMPLICIT_TYPES 阶段的声明边界类型推断与循环推断诊断。
- `cfir-resolve-body-resolve-phase`: 定义 BODY_RESOLVE 阶段的表达式类型、调用绑定与控制流语义。
- `cfir-resolve-checkers-phase`: 定义 CHECKERS 阶段的最终规则检查与稳定诊断输出。
- `cfir-resolve-lazy-jumping-contract`: 定义 jumping phase / same-phase lazy resolve 的合法调用合同与防重入规则。

### 修改功能
- `compiler-architecture`: 将“CFIR_RESOLVE 规划中”升级为“完整语义实现进行中/可验收能力”，并补充该阶段在 12 阶段流水线中的正式输入输出与验收边界。
- `raw-cfir-implementation`: 补充 Raw CFIR 与 CFIR_RESOLVE 的阶段边界契约（RAW 保留语法形态，RESOLVE 完成语义绑定），防止职责漂移或重复实现。

## 影响

- 受影响模块：
  - `cfir:cfir-tree`（resolve processors、phase registry、providers、diagnostics、session wiring、核心测试）
  - `analysis:analysis-api-cfir`（`CaCfirResolveFacade` 与 resolve 生命周期协同）
  - `tests:test-infrastructure`（resolve 端到端测试装配、handler/facade 规范化）
- 受影响实现面：
  - `org.cangjie.cfir.resolve` 的 processor 与注册体系
  - `org.cangjie.cfir.providers` 的查询契约与真实实现接入方式
  - 诊断工厂/报告路径与 golden 数据组织
- 风险与兼容：
  - 现有 minimal 测试与调用点可能需要迁移，存在短期破坏性调整（命名、入口、断言基线）；
  - 但该调整属于必要收敛，用于确保 CFIR_RESOLVE 达到“完整实现”而非“最小可运行”状态。
