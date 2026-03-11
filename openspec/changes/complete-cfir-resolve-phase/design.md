## 上下文

当前代码库已经具备以下基础：
- `CfirResolvePhase` 定义了完整阶段序列与 lazy resolve 约束；
- `CfirTotalResolveProcessor`、`CfirPhaseResolverRegistry`、`CfirResolveComponentsRegistrar` 已可运行；
- `CfirMinimalResolveProcessors` 实现了小范围规则（主要是 super/extend 的部分检查）；
- `analysis-api-cfir` 已提供 `CaCfirResolveFacade`。

主要问题：
- 现有实现仍偏向“最小可运行”，并未覆盖完整语义；
- `IMPLICIT_TYPES`、`BODY_RESOLVE` 仍是空推进，不满足阶段职责；
- provider 与 diagnostics 体系尚未形成完整、可扩展的正式架构；
- 测试覆盖以 synthetic case 为主，无法证明与官方仓颉编译器语义的对齐程度。

约束与利益相关方：
- 架构约束：继续沿用 Kotlin K2/FIR 风格（phase pipeline、session component、provider 抽象）；
- 语义约束：功能与诊断以官方仓颉编译器 `external/cangjie_compiler/src/Sema` 为基准；
- 模块约束：核心改动集中在 `cfir:cfir-tree`，并联动 `analysis:analysis-api-cfir` 与 `tests:test-infrastructure`。

## 目标 / 非目标

**目标：**
- 将 CFIR_RESOLVE 从 minimal 骨架升级为完整语义阶段，实现全部子阶段职责。
- 建立“框架对齐 K2、语义对齐仓颉官方实现”的双重约束落地机制。
- 形成可持续扩展的 resolve 组件架构（processor/service/provider/diagnostic）。
- 建立完整测试矩阵，覆盖阶段行为、语义诊断、Analysis API 端到端一致性。

**非目标：**
- 本变更不实现 FINALIZE、MANGLING、CFIR2CHIR 等后续阶段。
- 本变更不引入与 resolve 无关的新语言特性。
- 本变更不重构 Raw CFIR builder 的整体架构，仅补充与 resolve 的边界契约。

## 决策

### 决策 1：按阶段建立“正式处理器 + 共享语义服务”架构
- 方案：
  - 保留 `CfirResolveProcessor` 与 `CfirPhaseResolverRegistry`；
  - 为每个 phase 建立正式处理器（替代 minimal 空推进）；
  - 将共用语义逻辑下沉到 `resolve/services`（类型引用解析、作用域查询、调用解析、推断、诊断装配）。
- 原因：
  - 保持与 K2 分阶段处理模型一致；
  - 避免将规则散落在单个巨型处理器中，便于语义对齐与回归。
- 备选方案：
  - 单一 mega-processor（按阶段分支）：实现快但可维护性差，诊断归因困难；
  - 继续 minimal processor 增量打补丁：无法收敛到完整阶段语义。

### 决策 2：建立官方语义映射目录（Rule Catalog）
- 方案：
  - 为 resolve 规则建立稳定 ID（如 `RULE_EXTEND_NOT_INTERFACE`）；
  - 记录对应官方实现文件/函数（`Sema/*.cpp`）；
  - 诊断工厂与测试基线统一引用该目录。
- 原因：
  - 将“语义对齐”从口头要求变成可追溯工件；
  - 便于后续差异排查与升级。
- 备选方案：
  - 仅在注释中零散记录来源：不可审计，易漂移。

### 决策 3：provider 体系改为组合式正式提供者
- 方案：
  - 由 `CfirResolveComponentsRegistrar` 注册可组合 provider 链（source/import/builtin/extend）；
  - 空 provider 仅作为测试或过渡配置，不作为默认生产路径；
  - resolve 阶段统一通过 provider 抽象取符号与声明，不直接绕过访问内部结构。
- 原因：
  - 符合 interface-first 与模块边界约束；
  - 支撑跨模块与 Analysis API 一致性。
- 备选方案：
  - 处理器直接访问具体实现对象：耦合过高，不利于替换和测试隔离。

### 决策 4：分层完成 `IMPLICIT_TYPES` 与 `BODY_RESOLVE`
- 方案：
  - `IMPLICIT_TYPES` 仅负责声明边界推断（变量/返回类型等）；
  - `BODY_RESOLVE` 负责表达式类型、调用绑定、控制流相关语义；
  - `CHECKERS` 只做最终规则检查和诊断整合，不重复执行类型求值。
- 原因：
  - 与现有 phase 设计契约一致；
  - 降低循环依赖与错误恢复复杂度。
- 备选方案：
  - 把推断与体解析混在同一阶段：短期可行，长期难维护且不利 lazy resolve。

### 决策 5：测试从 synthetic 驱动升级为“真实输入 + 分层断言”
- 方案：
  - 保留现有 synthetic 测试作为回归护栏；
  - 新增真实 `.cj` 用例，覆盖 imports/super/extend/implicit/body/checkers；
  - 对 `CaCfirResolveFacade` 增加端到端一致性测试（与 direct processor 对齐）；
  - 诊断输出采用稳定排序与稳定文本结构。
- 原因：
  - 真实语义覆盖必须来自真实输入；
  - 可直接支撑后续分析 API 与 IDE 场景。
- 备选方案：
  - 仅扩展 synthetic marker：维护成本低，但语义代表性不足。

## 风险 / 权衡

- [规则覆盖面大，迭代周期长] → 分阶段里程碑交付（先 SUPER_TYPES/EXTENSIONS，再 IMPLICIT/BODY），每阶段都有可验收测试门槛。  
- [与现有 minimal 测试基线冲突] → 保留 legacy 测试集并增加迁移断言，逐步将主路径切换到正式实现。  
- [官方语义实现复杂，可能出现偏差] → 维护规则映射目录与差异清单，要求每条新增规则都附带来源与回归用例。  
- [lazy resolve 引入同阶段重入风险] → 严格使用 phase 契约与可重入防护（同阶段允许范围仅限定义规则）。  
- [性能回归风险] → 在关键阶段添加基准用例（大文件/深继承/高泛型密度），并监控 diagnostics 与解析耗时。

## Migration Plan

1. 建立迁移支架：
   - 引入正式处理器命名与目录结构；
   - 保留 `CfirMinimal*` 为 legacy 兼容层并标记迁移状态。
2. 分阶段替换实现：
   - 先替换 `IMPORTS/SUPER_TYPES/TYPES/STATUS/EXTENSIONS`；
   - 再替换 `IMPLICIT_TYPES/BODY_RESOLVE/CHECKERS`；
   - 每次替换都要求新增阶段测试与诊断用例通过。
3. 切换默认注册路径：
   - `CfirResolveComponentsRegistrar` 默认注册正式 provider + 正式处理器；
   - minimal 路径仅用于过渡测试或兼容入口。
4. 收敛 Analysis API：
   - 确认 `CaCfirResolveFacade` 默认走正式路径；
   - 补齐端到端一致性测试。
5. 清理与收尾：
   - 完成迁移后移除过时 minimal 实现或将其隔离到 legacy 包；
   - 更新架构文档与阶段状态说明。

回滚策略：
- 若某阶段替换导致主线回归，可按阶段粒度回退到上一稳定处理器实现，而不回退整个 resolve 子系统；
- 保留 legacy 测试可用于快速验证回滚有效性。

## Open Questions

- 是否在本次变更中同时引入更细粒度的阶段性能监控（例如每 phase 耗时采样），还是作为后续优化任务单独推进？  
- `BODY_RESOLVE` 的首批语义覆盖范围是否包含全部仓颉特性表达式（如 effect/interop 相关），还是先定义核心子集并分批并入？  
- 官方语义映射目录采用代码内常量、独立 markdown，还是两者并存（推荐两者并存：代码用于执行，文档用于评审）？  
- legacy minimal 入口保留多久：一个发布周期还是直到 FINALIZE 阶段联调完成？
