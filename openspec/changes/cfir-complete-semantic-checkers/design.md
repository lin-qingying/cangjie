## 上下文

当前变更已经完成了一批 checker 落地，但 OpenSpec 设计仍停留在“按 checker 分组推进”的描述层，缺少两个框架级约束：

1. **缺少全诊断定义覆盖率模型**：没有以 `CfirDiagnosticsList` 为唯一基线回答“总量、已覆盖量、剩余量、责任归属、验证状态”。
2. **缺少 resolve / checker 职责分层**：容易把本应由 resolve 负责的语义错误，误放到 checker 中补洞。

结合 `CfirDiagnosticsList.kt` 当前结构，可以看到诊断并不天然等价于 checker：

- 一部分天然属于 checker，如修饰符合法性、声明/表达式语义、互操作约束、平台注解约束、mock/common-specific 等；
- 一部分天然属于 resolve，如参数绑定、候选选择、约束求解、未解析引用、深层泛型推断；
- 还有一部分当前虽已有实现，但尚未纳入统一台账，无法形成可信覆盖率说明。

因此，本设计将原有“checker 完成计划”提升为“**全诊断定义覆盖治理 + 分层实施设计**”。

## 目标 / 非目标

**目标：**

- 以 `CfirDiagnosticsList` 中的全部诊断定义作为唯一覆盖对象。
- 建立全量诊断覆盖台账，确保无遗漏、无游离项。
- 以 `openspec/changes/cfir-complete-semantic-checkers/diagnostic-coverage-ledger.md` 作为本变更的覆盖台账主入口。
- 对每个诊断定义明确归属：
  - `existing`
  - `resolve`
  - `checker`
- 明确约 80-100 个剩余诊断属于 resolve 管线职责，约 80-90 个剩余诊断属于 checker 管线职责，并以实际台账统计结果为最终准。
- 将任务拆分到“不漏一个诊断子项”的粒度，至少做到每个诊断都有映射关系、责任人、实现入口、测试入口和阻塞说明。

**非目标：**

- 不修改 `CfirDiagnosticsList` 的诊断定义语义。
- 不用 checker 为 resolve 缺口做兜底实现。
- 不以“测试通过”代替“职责正确”。
- 不把未确认语义的诊断混入已完成统计。

## 核心设计

### 决策 1：以诊断定义而非 checker 文件作为一级规划对象

**选择**：将 `CfirDiagnosticsList` 中的每个诊断定义作为一级治理单元；checker 文件、resolve 模块、测试目录都只是其承载位置。

**理由**：

- checker 分组只是实现组织方式，不是覆盖对象本身；
- 同一诊断组中的不同诊断可能分属不同实现层；
- 只有按诊断定义建账，才能真正计算覆盖率并防止遗漏。

### 决策 2：建立三态责任模型

**选择**：每个诊断定义必须被归入三类之一：

- `existing`：现有实现已完整覆盖，且能定位到实现入口与测试入口；
- `resolve`：应由 resolve 管线负责；
- `checker`：应由 checker 管线负责。

**补充规则**：

- `resolve` 与 `checker` 之间必须互斥；
- 不允许使用“暂不确定”作为长期状态；
- 若短期无法确认，只能作为临时阻塞状态，并必须附带证据链与后续任务。

### 决策 3：以责任子域继续细分

**选择**：在三态责任模型之下，再按子域细分，保证后续任务可执行。

**resolve 子域**：

- `CallResolution`
- `Constraint`
- `TypeCheck`
- `Unresolved`
- `GenericDeep-Inference`

**checker 子域**：

- `General`
- `Function`
- `Expression`
- `InheritanceDeep`
- `ClassStruct`
- `Property`
- `ConstDeclaration`
- `AnnotationExtra`
- `Inout`
- `VArrayExtra`
- `EffectsExtra`
- `Deprecated`
- `CommonSpecific`
- `ExtendExtra`
- `Spawn`
- `Interface`
- `JavaInterop`
- `JavaMirror`
- `CJMapping`
- `ObjCInterop`
- `ObjCCJMapping`
- `ForeignName`
- `IfAvailable`
- `APILevel`
- `Hide`
- `Mock`
- `Unused`
- `DeclarationStatusExtra`

**理由**：只有先切到责任子域，后续任务才能真正“一个不漏”地展开，而不是继续停留在宽泛批次。

### 决策 4：覆盖率说明必须可审计

**选择**：本变更必须产出可审计的覆盖率说明，而不是口头估算。

覆盖率说明至少回答：

- `CfirDiagnosticsList` 诊断总数；
- 已覆盖数；
- 已覆盖但测试不足数；
- resolve 负责数；
- checker 负责数；
- 各子域剩余缺口数；
- 每个缺口对应的实现任务与验证任务。

**理由**：没有覆盖率说明，就无法证明“提案是否完成”，也无法知道任务拆分是否真实闭环。

### 决策 5：禁止 checker 兜底 resolve 缺口

**选择**：对 `CallResolution`、`Constraint`、`TypeCheck`、`Unresolved`、`GenericDeep` 深层推断部分，若语义本质依赖候选选择、约束求解、未解析恢复或推断收敛，则必须在 resolve 管线中实现。

**明确禁止**：

- 在 checker 中重新做一套不完整的参数绑定；
- 在 checker 中重复实现候选筛选；
- 在 checker 中根据错误形状做事后猜测式诊断；
- 为了“先补覆盖率”而将 resolve 语义错误错误地下沉为 checker。

**理由**：这会直接破坏编译器分层，导致诊断重复、时序错误、信息不完整和后续不可维护。

### 决策 6：任务必须分成“实现任务 + 验证任务”

**选择**：每一类诊断缺口都必须同时有：

- 实现任务
- 测试/验证任务
- 覆盖率回填任务

**理由**：如果只有实现没有验证，覆盖率台账很快会失真；如果只有测试没有责任归属，项目会再次退化回“症状驱动补洞”。

## 架构影响

### 1. 对 checker 体系的影响

- checker 仍是 declaration / expression / type 三大体系；
- 但其职责边界将被重新校正，只承接真正属于 checker 的诊断；
- 原 proposal 中把 `GenericDeep` 整体视为 checker 的写法需要修正为：
  - 一部分属于 resolve 深层推断；
  - 一部分属于 checker 语义约束。

### 2. 对 resolve 体系的影响

- resolve 不再被视为“checker 的前置条件”，而是诊断覆盖的主要承载层之一；
- 本变更要求把 resolve 诊断缺口显式纳入提案，不再默认排除。

### 3. 对测试体系的影响

- 测试需要按责任层组织验证：
  - resolve 诊断验证；
  - checker 诊断验证；
  - 端到端一致性验证；
- `diagnostics2` 可作为新增矩阵的承载区，但不能替代对现有测试入口的责任归位。

## 风险 / 权衡

**[风险] 现有“已完成”任务统计失真**
→ 旧任务以 checker 分组为中心，未反映未归位诊断。缓解：重写任务树，以覆盖台账重新计算进度。

**[风险] 诊断存在跨层边界争议**
→ 某些 `GenericDeep` 或调用语义诊断可能同时依赖推断与后置语义。缓解：以“诊断产生所需的主信息源”作为归属原则，优先归入 resolve。

**[风险] 覆盖率统计与真实实现脱节**
→ 如果覆盖率只记“声称已支持”，会再次失真。缓解：每条诊断必须绑定实现位置和测试位置。

**[权衡] 一次性把所有诊断纳入提案会显著扩大任务规模**
→ 这是必要扩大。相比继续保留模糊边界，这种扩大能换来真实可收敛的工程计划。

**[权衡] 先修 proposal/design/tasks 再继续编码会延缓短期提交**
→ 这是正确顺序。当前问题首先是提案不准确，不应在错误任务树上继续追加实现。
