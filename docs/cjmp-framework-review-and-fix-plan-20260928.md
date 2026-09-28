# CJMP 框架级审查与修复计划（2026-09-28）

本文件是 [原实施计划](cjmp-implementation-plan-20260923.md) 的补充。原计划 §11.5 记录了 v10 全量快照 0 失败、KotlinParityGatekeeper 终审通过。本文件按"框架级实现"标准对同一批实现做了复核：测试全绿，但仍有 22 条高严重度的非框架级实现。

- 第一部分（§0–§6）是完整审查结果。
- 第二部分（§7–§12）是修复计划。

说明：

- 行号以 2026-09-28 审查时的工作树为准（HEAD `b5aba9360` 加未提交修改）。审查期间另一个 agent 仍在修改工作树，行号可能漂移，定位时以函数名为准。
- 审查全程只读，没有改动任何代码。

---

## 第一部分：审查结果

### 0. 审查方法与标准

**范围**

- 提交：`b6bbb19f6`、`f964f41bb`、`1dfa758f8`、`f06020203`、`08b8df4b5`、`c7731df8c`、`d82a4d1b2`、`6e58cab6b`、`03b84abef`、`78c1bc351`、`f781a7a88`，再加上当前的未提交修改。
- 规模：生产代码约 1 万行。其中 cjmp 核心文件约 5,000 行，另有约 60 个文件含散点改动。
- 顺带审查了工作区内与 cjmp 无关的 analysis-api 未提交修改。

**"框架级"的定义**：采用项目 skill `cangjie-cfir-llt-repair` 的定义，即修复必须改变问题类型的共享 owner（所有调用点都经过的那一层），而不是让暴露问题的某个 fixture 通过。下列 8 类都属于"非框架级"：

1. 兼容层：掩盖了哪条代码路径才是正确的。
2. 回退或兜底：静默掩盖失败，而不是暴露真实诊断。
3. 硬编码 fixture 逻辑：硬编码名称、mangled id、特定类型或包名、特定用例形状。
4. 只为某一个文件或某一个测试加的特判。
5. 绕开真正 owner 的桥接式实现：修复到不了其他调用点使用的代码。
6. 只在测试中生效的行为：运行时与测试的行为分叉。
7. 为满足当前期望输出而做的语义降级。
8. 引入官方 cjc 或 `external/cangjie_compiler` 没有许可的 Kotlin 语义。

此外，项目 `CLAUDE.md` 还要求：resolve 框架在目录层级、处理器分层、类名与公共方法名、处理流程上镜像 Kotlin K2；如有偏离必须写明原因；模块之间通过接口暴露抽象。

**依据优先级**（本仓库现状不作为依据）

1. cjc 1.1.3 实测。
2. `external/cangjie_compiler` 在 git tag `v1.1.3` 下的源码。
3. `external/kotlin`，只作架构参考。

注：审查时以 v1.1.3 作为主要对照版本。按 §12 决策 1，修复时的语义依据扩展到全部官方版本，版本之间有差异的行为用语言版本门禁控制（见 §7 原则 2）。

**分组**：按子系统分 6 组并行审查，每组都对照官方源码和 Kotlin 对应物。

| 组 | 范围 |
|---|---|
| 配对引擎 | `resolution.common` mpp 层、`cfir/resolve/.../cjmp/`、`CfirCjmpMatchingProcessor`、配对存储 |
| LL 与分析侧 | LL 惰性配对、LL 模块图与 provider、测试基建、与 cjmp 无关的 analysis-api 修改 |
| cjmp 检查器 | `CfirCommonSpecificChecker` 及修饰符、冲突、注解相关检查 |
| 泛型实例化 | `CfirGenericInstantiationChecker`、use-site scope、extend 规则服务 |
| 序列化与驱动 | CJO 读写、common part 加载门、编译管线、CLI、session 装配 |
| resolve 消费侧 | 调用解析遮蔽、默认值读穿、raw 构建与 PSI、推断返回类型、`c7731df8c` 的解析修复 |

**判定口径**

- 官方语义本身规定的特例，只要落在对应的共享 owner 上、且有官方出处，就不算问题（见 §5）。
- 在计划里登记过，不等于就是框架级。每条发现都注明：计划是否登记、代码已提交还是未提交。
- 严重度：
  - **高**：在未测试的形状上会给出错误结果，或掩盖真实错误。
  - **中**：结构性偏离，暂时正确，但行为会分叉。
  - **低**：命名、分层或可维护性问题。
- 置信度：
  - **确定**：代码路径已逐行核实。
  - **可能**：需要 cjc 探针或 fixture 复现。

**核实**：主审查者亲自核对了 11 条高严重度发现和 1 条低严重度发现，全部成立，明细见 §6。

### 1. 结论摘要

去重后共 70 条非框架级实现：高严重度 22 条，中严重度 29 条，低严重度 19 条。

它们的共同根因是：几个本该承担问题的共享 owner 不存在，下游只好各自打补丁。按缺失的 owner 归并如下。

| 组 | 缺失的共享 owner | 高 | 中 | 低 |
|---|---|---|---|---|
| A | 配对结果的产出时机与读取入口：阶段位置、带惰性解析的访问器 | 2 | 1 | 0 |
| B | 反序列化声明的 common 语义：`COMMON` 与 `FROM_COMMON_PART` 分离 | 1 | 0 | 0 |
| C | common 并入 specific 的合并视图：官方 `MergeCommonIntoSpecific`，Kotlin `FirActualizingScope` | 5 | 0 | 0 |
| D | 声明级的实现关系判定：泛型实例化 owner 组、use-site scope | 4 | 3 | 2 |
| E | 检查器按官方条件实现，并按 Kotlin checker 分层 | 4 | 9 | 1 |
| F | 匹配器内部的共享判据：类型等价、first-fit、enum 构造器 | 2 | 1 | 5 |
| G | 生产 LL/IDE 入口：会话 CJMP 模式、dependsOn、阶段门 | 1 | 6 | 2 |
| H | 序列化与编译驱动：CJO 产出、common part 加载、FullId 键约定、诊断通道 | 3 | 6 | 5 |
| I | 本批顺带的 resolve 修改 | 0 | 2 | 4 |
| J | 静默兜底（`runCatching`） | 0 | 1 | 0 |

另有一个横跨多组的问题：官方各版本之间有行为差异，本仓库却只实现了其中一个版本（主要是 origin/main）的行为，也没有用语言版本门禁加以区分。因此在 1.1.x 语言版本下，这些位置的行为是错的：

- 加载门、CLI 形态与模式判据（H6）
- 注解分类表与禁止集（E2、E7）
- 多 common parent 的配对语义与 DAG 过滤（G6、F4）

按 §12 决策 1，这类差异一律按"确定引入版本 → 定义 `LanguageFeature` → 用语言版本门禁选择行为"来处理（见 P0 的跨版本差异清单）。

### 2. 高严重度问题（22 条）

每条的来源审查组写在末尾。"主审查者核实"表示主审查者本人对照代码和官方源码确认过。

#### A1　配对阶段排在 IMPLICIT_TYPES 之后

- **位置**
  - 阶段顺序：`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirResolvePhase.kt:206-223`，当前为 IMPORTS → SUPER_TYPES → TYPES → STATUS → EXTENSIONS → IMPLICIT_TYPES → CJMP_MATCHING → BODY_RESOLVE。
  - 存储清空与写入：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt:77-82`，到本阶段开始时才进行。
  - 函数体跳过：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt:331-357`，`transformFunctionContent` 在 `bodyResolved` 为真时不再解析函数体。
  - 依赖推断结果的匹配分支：`resolution.common/src/org/cangnova/cangjie/resolve/calls/mpp/AbstractCjmpMatcher.kt:78-92`，以及 `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchingContext.kt:87-92` 的 `hasInferredReturnType`（未提交）。
- **现状**
  - 省略返回类型的函数体、省略类型的变量初始化器，都在 IMPLICIT_TYPES 阶段完整解析。
  - 到 BODY_RESOLVE 时，因为 `function.body?.coneTypeOrNull != null`，这些函数体不再重新解析。
  - 而 IMPLICIT_TYPES 阶段时配对存储还是空的：`CfirCallResolver.reduceCjmpShadowedCommonCandidates` 遇到 `storage.isEmpty` 直接返回（`CfirCallResolver.kt:1669`），`cjmpHasDefaultValue()` 也查不到 `commonFor`。
- **后果**
  - specific 模块里的 `public func use4() { platform() }`：common 与 specific 两个同签名候选同时存活，报歧义。
  - `public func use3() { a() }`（对应 `cjmpDefaultReadThrough.cj`）：specific 形参被判为缺实参，调用静默绑定到 common 那个没有函数体的 `a`。
  - `specific func b() { a() }`（有函数体时可省略返回类型）：推断出错误类型，配对记为 TYPE_NOT_RESOLVED，连带引发 NOT_MATCHED 级联。官方在配对时对这类返回类型按 Quest 照常接受。
  - 现有 fixture 里的调用都写在显式声明了返回类型的函数中（如 `use2(): Int64`），所以没有覆盖到这些情况。
  - 同时违反本仓库的阶段契约第 4 条"不得读取更高阶段的信息"（`CfirResolvePhase.kt:51-53`）。
- **证据**
  - 官方：
    - `TypeChecker.cpp:2042-2047` 在 PreTypeCheck 中、检查任何函数体之前，就执行 `MatchSpecificWithCommon`；
    - 隐式返回类型此时是 Quest，一律先接受，由 PostTypeCheck（`CheckCJMP.cpp:545-569`）复核；
    - 调用解析在 `TypeCheckCall.cpp:2301` 使用配对结果。
  - Kotlin：`FirResolvePhase.kt:150-183` 的顺序是 STATUS → EXPECT_ACTUAL_MATCHING → CONTRACTS → IMPLICIT_TYPES_BODY_RESOLVE。
  - 计划：§10 Phase 2 写的是"IMPLICIT_TYPES 后，对齐 Kotlin"，§2 矩阵写的却是"TYPES/STATUS 后"，前后矛盾；消费侧的后果没有登记。
  - 测试：
    - 未提交的单测 `AbstractCjmpMatcherTest.kt:205-220` 把这个偏离固化成了期望；
    - 测试辅助 `CfirMacroAnnotationSourceModuleTest.kt:307-329` 的 `resolveThroughBody` 先跑 CJMP_MATCHING 再跑 IMPLICIT_TYPES，与生产顺序相反，恰好掩盖了本问题（见 I6）。
- **框架级替代**
  - 把 CJMP_MATCHING 前移到 EXTENSIONS 与 IMPLICIT_TYPES 之间。
  - matcher 按"是否为隐式返回类型"（对应官方的 Quest）延后返回类型的兼容判断，协变复核留在 checker。
  - 所有配对结果的读取都走 A2 的访问器。
- **置信度与状态**
  - 机制确定（主审查者核实），具体诊断形态有待 fixture 复现。
  - 阶段位置已提交；`hasInferredReturnType` 分支和单测未提交。
  - 来源：配对引擎组 #4、resolve 组 #1。

#### A2　读取配对存储时不先推进阶段，并依赖反向查表

- **位置**
  - `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpMappingStorage.kt:171-235`（`commonFor`、`specificBindingsFor` 等）、`:350-359`（`cjmpHasDefaultValue`）
  - `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpSpecificCompilation.kt:38-46`（`isCjmpShadowedCommonDeclaration`）
  - `CfirCallResolver.kt:1665-1676`
  - `CfirCommonSpecificChecker.kt:1016-1062`（`CfirCjmpCommonSideChecker`）
- **现状**
  - 生产代码里，没有任何读取方会在读之前执行 `lazyResolveToPhase(CJMP_MATCHING)`。例外只有两处：FileStructure 推进当前文件的声明，`LLCfirBodyLazyResolver:743` 推进 CFG 成员。
  - 调用解析还按 common 去反查 specific。
- **为什么不是框架级**
  - 阶段 KDoc 里"必须早于 BODY_RESOLVE"的保证只在 eager 模式成立，因为 eager 会先对整个模块做配对。
  - LL 只对被请求的那个声明配对。跨文件的调用方做函数体解析时，被调用的 specific 函数可能只推进到了 STATUS 或 IMPLICIT_TYPES。
  - 因此下面这些结果都取决于请求顺序，IDE 与 CLI 的结果不一致：
    - 默认值读穿；
    - common 遮蔽；
    - scope、继承、实例化中的遮蔽判定；
    - common 方向的 NOT_MATCHED 与 MULTIPLE_COMMON_IMPLEMENTATIONS。
  - LL 测试只在显式推进到 CJMP_MATCHING 之后才读存储，没有覆盖这些读取方。
- **证据**
  - Kotlin `compiler/fir/tree/.../ExpectActualAttributes.kt:58-62` 的 `expectForActual` 访问器会先 `lazyResolveToPhase(EXPECT_ACTUAL_MATCHING)` 再读。
  - Kotlin `FirActualizingScope.kt:61-80` 从 actual 出发正向过滤 expect，不使用反向表。
  - 计划 C25 只登记了写入方向，读取侧没有登记。
- **框架级替代**
  - 把正向配对结果作为 specific 声明的属性保存。CFIR 已有 `CfirDeclarationDataKey` 机制（`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/CfirDeclarationDataKey.kt`）。
  - 通过一个"先 `lazyResolveToPhase(CJMP_MATCHING)` 再读"的访问器对外暴露。
  - 调用解析改为正向剔除 common。
  - 反向查询只留给全量解析之后运行的 checker，并且要先经 specific session 的 provider 找到候选、推进阶段后再读。
- **置信度与状态**
  - 机制确定；LL 下的具体诊断差异待 LL 诊断用例验证。
  - 已提交。来源：配对引擎组 #5、LL 组 #2。

#### B1　反序列化把 FROM_COMMON_PART 当成 common

- **位置**
  - 根因：`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt:804`（`buildStatus`）。
  - 消费方：`compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/CjmpDeserializedCommonSideReporter.kt:53-57`、`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpCommonSideFacts.kt:54-95`。
  - 症状补丁：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpResolver.kt:227-235`（`findCommonCandidates`）、`:259-262`（`findCommonMemberCandidates`），以及 `CfirCommonSpecificChecker.kt:126,136`。
- **现状**

```kotlin
status.isCommon = testAttr(decl, AttrBit.COMMON) || testAttr(decl, AttrBit.FROM_COMMON_PART)   // 已提交
(candidate !is CfirConstructor || !candidate.isPrimary) && ...status.isCommon...               // 未提交
```

- **为什么不是框架级**
  - 写侧会给 common part 中的每个声明写上 FROM_COMMON_PART（`CfirCjoPackageMetadataProducer.kt:454`；官方 `ASTWriter.cpp:1575-1577` 也一样）。
  - 结果在 CJO 路径下，所有没标 `common` 的声明都成了 `isCommon`，包括未标记的主构造、common 类里的普通成员、顶层辅助函数、typealias 和变量。
  - 源码路径（LLT/IDE）的 `isCommon` 只来自修饰符。同一个字段在两条路径上含义不同。
  - 本轮只剔除了主构造这一种形状。未覆盖的情况：
    - `specific func g()` 对 common part 中未标记的 `func g()`：CJO 路径直接绑定，不报 NOT_MATCHED；官方应报 NOT_MATCHED 和重定义。源码路径则会报 NOT_MATCHED。
    - CLI 的 common 方向报告器把未标记声明当作必须配对。写侧不给未标记声明写 COMMON_WITH_DEFAULT，反序列化后函数体为 null，于是报 `'common' ... can not find 'specific' match`，并使管线失败。
  - §11.3 里"enum 合成主构造被误当成必须实现的 common constructor"是同一机制的另一个实例，当时的处理是按形状从 CJO 写侧剔除。
  - C45 两段式测试（`CjmpTwoPhaseCompilationTest.kt:551-587`）只断言 `diagnostics`，不断言 `messages`/`completed`，掩盖了上述报告器的误报。
- **证据**
  - 官方 `CheckCJMP.cpp:664-693` 的 `CollectDecl`/`CollectCJMPDecls` 只收集带 `Attribute::COMMON` 的声明，并在结构上跳过 `PRIMARY_CTOR_DECL`（主审查者核实）。
  - 官方 `ASTLoader.cpp:298-331` 只把 common part 的**文件**标为 isCommon；`ASTLoader.cpp:604` 原样 `CopyAttrs`。
  - 官方 `SetCJMPAttrs`（`ParseCJMPDecl.cpp:57-65`）只为 COMMON 声明补 COMMON_WITH_DEFAULT。
  - 计划 §1.1 把读侧位映射列为"已存活，不重做"；§9.7 C45 把"统一过滤 primary 候选"登记为修复方案；根因没有登记。
- **框架级替代**
  - 反序列化分别恢复 COMMON 与 FROM_COMMON_PART，FROM_COMMON_PART 作为独立的来源属性。
  - "能否作为 CJMP 对应物"收敛为一个共享谓词：只看 COMMON/SPECIFIC 位，结构上排除官方的 PRIMARY_CTOR_DECL 形态。resolver、checker 和 CLI 报告器共用这个谓词。
  - 删除所有 `isPrimary` 特判。
- **置信度与状态**
  - 映射与路径分叉确定（主审查者核实）。CLI 多报是推断：在两段式测试的 common 源里加一个 `public func helper(): Int64 { 1 }`，并断言 `messages`/`completed`，即可确认。
  - 读侧映射已提交（`79bee4f32`，早于本计划）；resolver 和 checker 中的过滤未提交。
  - 来源：配对引擎组 #1、序列化组 H1。

#### C1　specific 类的成员 scope 不含 common 成员

- **位置**
  - `cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassUseSiteMemberScope.kt:418`（`declaredScope = CfirClassDeclaredMemberScope(classSymbol)`）。整个文件没有 CJMP 合并逻辑。
  - 隐式主构造的合成：`PsiRawCfirBuilder.kt:803-804,856-857`、`LightTreeRawCfirDeclarationBuilder.kt:425,476`。
- **现状**
  - `CfirCjmpCommonSideFacts.kt:94-95` 允许 specific 类省略带默认实现的 common 成员，并豁免 common 接口成员。
  - 但 specific 类的成员 scope 里只有它自己的声明，由此：
    - `Repo().extra()` 这样的调用、子类继承、接口实现，都看不到 common 的默认成员。
    - specific 接口省略了 common 接口的抽象成员后，实现类不会再被要求实现这些成员。
    - specific 类省略所有构造器时，raw builder 会补一个隐式 `init()`。
  - extend 一侧已经在 scope 层完成合并（`CfirExtendMemberScope` 加 session extend provider），nominal 一侧没有。
- **为什么不是框架级**：合并后的成员面没有 owner，下游只能各自拼凑，调用解析和各检查器看到的成员集合互不一致。C2–C5 以及 D 组的大部分补丁都由此而来。
- **证据**
  - 官方：
    - `CheckCJMP.cpp:219-289` 的 `MergeCommonIntoSpecific` 把全部 common 成员并入 specific 的声明体；
    - `:298-333` 对 class、struct、interface、enum 执行这一合并；
    - `TypeChecker.cpp:2093,2104` 先合并，再 `AddDefaultFunction`；
    - `Utils.cpp:381-394` 不为 specific 类补默认构造。
  - Kotlin 没有直接对应物（actual 必须声明全部成员），最接近的是 `FirActualizingScope`。
  - 计划 §3 D1 登记了"逻辑合并在 transform/resolve 阶段实现"，但实际没有落地；作用域缺口也没有登记。
- **框架级替代**
  - 在 providers 层为已配对的 specific nominal 提供合并后的声明成员视图，内容为 specific 成员加上未被配对替代的 common 成员，构造器同样处理。
  - 这个视图由 `CfirCangJieScopeProvider.getUseSiteMemberScope` 统一产出。
  - specific nominal 的隐式构造按官方 AddDefaultCtor 的条件处理。
  - 这是 Cangjie 相对 Kotlin 的结构偏离，需要写进 KDoc 和计划。
- **置信度与状态**
  - 作用域缺口确定；隐式构造的具体后果待探针确认。
  - 已提交。来源：resolve 组 #2。

#### C2　泛型实例化检查器在 checker 内部自建 common 投影

- **位置**：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGenericInstantiationChecker.kt`
  - `1190-1235`；
  - `1378-1418`，其中 `1394-1405` 用 common session 自建 use-site scope；
  - `2614-2660`（`isImplementedBy` / `isMergedWithCommonDirectMember`）。
- **现状**：只有当被实例化的声明本身满足 `storage.commonFor(this)` 时，才去 common session 建一个 scope（不含 extend 成员），再用自己写的合并/实现谓词把 common 签名并进来。
- **为什么不是框架级**
  - **未配对的子类型会漏报**：把 `cjmpGenericCounterpartInheritedDefaultInstantiation.cj` 里的 C 换成 specific 模块中的普通 `class D<U> <: I<U> { public func f(value: Int64) {} }`。specific 的 `I` 是空的，D 也不在配对存储里，所以 `D<Int64>` 漏报。
  - **CLI 路径失效**：common 来自 CJO 时 origin 是 Library，投影恒为空。该 fixture 的结论只在多模块源码路径下成立。
  - **出处引错**：`isImplementedBy` 注明对齐 `IsImplementationFunc`，但这个函数属于 GenericInstantiationManager（`GenericInstantiationContext.cpp:398`），不是产生本诊断的归并路径。
- **证据**
  - 官方在类型检查之前，就用 `MergeCommonIntoSpecific` 做了物理合并（`CheckCJMP.cpp:219-293,309-330`），之后所有消费方看到的是同一个合并成员面。
  - 计划 §11.1 第 571 行登记了这个 checker 内的投影，但登记不等于框架级。
- **框架级替代**：改为消费 C1 的合并 scope（包括 CJO 来源），删除 checker 内的投影和自造谓词。
- **置信度与状态**
  - 确定。CLI 路径的问题建议补一个两段式用例再确认。
  - 已提交（`d82a4d1b2`）。来源：泛型实例化组 H4、resolve 组 #2。

#### C3　跨 session 成员重载冲突按 fixture 形状打补丁

- **位置**
  - `CfirCommonSpecificChecker.kt:115-206`
  - 配套的反向抑制：`CfirGenericInstantiationChecker.kt:1205-1233,2639-2657`（`isMergedWithCommonDirectMember`）
- **现状**

```kotlin
is CfirNamedFunction -> !function.status.isSpecific                       // 只取未标记 specific 的函数
is CfirConstructor -> function.status.isSpecific && !function.isPrimary   // 只取 specific 次构造
is CfirNamedFunction -> commonFunction is CfirNamedFunction && commonFunction.status.isCommon  // 只比 common 标记函数
is CfirConstructor -> commonFunction is CfirConstructor && commonFunction.isPrimary             // 只比未标记主构造
```

- **为什么不是框架级**
  - 官方的冲突来自合并后成员体上的通用 `PreCheckFuncRedefinition`（`PreCheck.cpp:1820-1835,1631-1669`；合并发生在 `TypeChecker.cpp:2093`）。同名组按 `FilterOutCommonCandidatesIfSpecificExist`（`CheckCJMP.cpp:500-520`）处理。
  - 本补丁只覆盖了 `cjmpUnmarkedDirectMemberSignatureMerge`、`cjmpSpecificInitConflictsWithUnmarkedCommonPrimary` 两个 fixture 的形状。
  - **漏报**：
    - 未标记 common 对未标记 specific 的同签名函数或 init；
    - `specific` 函数或 init 对未标记 common；
    - extend 成员（官方经 `MergeCJMPExtensions` 合并后同样会检查，见 `TypeCheckExtend.cpp:411`）；
    - var/prop 重定义（可能）。
  - **可能多报**：同名组里另有 `specific` 函数时，官方会整组剔除 common，本仓库仍会报 CONFLICTING_OVERLOADS。
  - **note 不一致**：只有构造器附了 note，官方对所有同签名成员都附（`Diags.cpp:193-196`）。
  - **两个检查器的谓词不同**：泛型实例化检查器用另一套谓词（不看标记位）剔除"已合并"成员。两者叠加后，泛型类中未标记对未标记的同签名成员不会产生任何诊断。
  - **与顶层不对称**：
    - 顶层走 collector 的 `shouldCheckForMultiplatformRedeclaration`，是框架级的；
    - 成员走 `collectClassMembers`（`CfirConflictsHelpers.kt:263-380`），只扫描 specific 自己的 use-site scope。
- **证据**：计划 §11.2 和 C45 登记了这个补丁，但没有登记它只覆盖这几种形状。
- **框架级替代**
  - 让 `collectClassMembers`/`collectExtendMembers` 在 C1 的合并视图上统一检测重定义。
  - 在组级别实现 `FilterOutCommonCandidatesIfSpecificExist`。
  - 删除两个检查器中的成对补丁和反向抑制。
- **置信度与状态**
  - 本仓库的覆盖面确定。"未标记对未标记在官方会报冲突"与已测的 fixture 走同一机制，待 cjc 1.1.3 探针确认。
  - 命名函数部分已提交（`d82a4d1b2`）；构造器分支、note、`containsErrorType` 未提交。
  - 来源：检查器组 #4。

#### C4　遮蔽只在调用解析里做后置过滤，且判据有三套

- **位置**
  - 主入口：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt:1657-1692` 的 `reduceCjmpShadowedCommonCandidates`。它只在 `collectCandidates` 里、`reduceCandidates` 之后被调用。
  - 绕过路径 A：`callableReferenceChoices`（`:1406-1423`）。`ArgumentCheckingProcessor.kt:336-351` 会给每个函数实参都创建引用 atom；出现两个候选时走 `:1242-1290`，报歧义。
  - 绕过路径 B：`resolveCallFromPrecollectedCandidates`（`:549-587`）。它用的 discovery 来自 `:1708-1711`，是过滤前的 `candidatesDiscoveredInBestGroup()`。
  - 另外两套判据：`CfirCjmpSpecificCompilation.kt:38-46`、`cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirCompositeSymbolProvider.kt:60-65`。
  - scope 和 checker 里零散的过滤：`CfirExtendMemberScope.kt:198`、`CfirGenericInstantiationChecker.kt:999,1007,1465-1469,1489-1492,1507-1511`、`CfirInheritanceDeepChecker.kt:1783,1862`。
- **现状**

```kotlin
val storage = session.cjmpMappingStorageOrNull ?: return candidates
if (storage.isEmpty) return candidates
return candidates.filterTo(linkedSetOf()) { candidate -> ...
    storage.specificBindingsFor(declaration).isEmpty() }
```

- **为什么不是框架级**
  - 把 CJMP 函数当作函数值实参传递，例如 `apply(platform)`，会同时拿到 common 和 specific 两个候选，结果报歧义。
  - 过滤依赖反向表，无法触发 specific 的按需配对，与 A2 叠加。
  - 读取的是 use-site session 的存储，而不是 specific 声明所在 session 的存储。下游模块同时看得到 common 与 specific 时，过滤不生效（可能，取决于 IDE 的模块图）。
  - 三套判据的门禁各不相同：
    - 调用解析：语言特性开启 + 存储非空 + 反向表非空。被拒绝的第二个绑定也算在内。
    - `isCjmpShadowedCommonDeclaration`：语言特性开启 + SPECIFIC 模式 + 正反两个方向的映射都成立。它的 KDoc 自称是"唯一的 session 判据"。
    - `CfirCompositeSymbolProvider`：只要存在任何 specific，就丢掉全部 common。
  - 其中类型层"specific 优先"这条规则本身有官方依据（见 §5）。D2 里补回的签名漏掉了过滤，就是判据分散的直接后果。
  - KDoc 里引用的官方对应物 `FilterOutCommonCandidatesIfSpecificExist` 引错了：它实际是重定义预检用的函数。
- **证据**
  - Kotlin：
    - `TowerLevels.kt:360-363`：`ScopeBasedTowerLevel` 用 `FirActualizingScope` 包装作用域，从 actual 出发正向查配对（`FirActualizingScope.kt:66-80`）；
    - `ConeEquivalentCallConflictResolver.kt:46-54` 负责兜底，CFIR 却把它注释掉了（`ConeCallConflictResolver.kt:52`）。
  - 官方：
    - `CheckCJMP.cpp:523-533` 的 `RemoveCommonCandidatesIfHasSpecific` 要求配对的 specific 也在候选集中，并在重载决议之前执行；
    - 调用点有两处：`TypeCheckCall.cpp:2301`，以及函数引用的 `TypeCheckExpr/NameReferenceExpr.cpp:344`。
  - 计划：
    - §10 Phase 2 登记了"消费侧遮蔽"，§2 和 §11 第 545 行登记了"多点跳过"；
    - 绕过路径、判据重复、出处引错都没有登记。
- **框架级替代**
  - 在 CFIR 的 `ScopeBasedTowerLevel`（`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/tower/TowerLevelHandler.kt:130`）上，为非类型作用域包一层 actualizing scope。
  - 对同一批出现的 specific 候选，经 A2 的访问器正向查出对应的 common（走 specific 自身的 session，按需推进阶段），再剔除已配对的 common。
  - 删除后置过滤、反向查表，以及 scope 和 checker 里零散的过滤，只保留一个遮蔽判据。
- **置信度与状态**
  - 代码路径确定。
  - 主体已提交；泛型实例化补签名路径的漏过滤属于未提交修改。
  - 来源：resolve 组 #3、泛型实例化组 M4。

#### C5　默认值读穿只接到了调用解析

- **位置**
  - 实现：`CfirCjmpMappingStorage.kt:350-359`（`cjmpHasDefaultValue`）。
  - 已接入：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirMapArguments.kt:573,584,608,754`、`CfirCallResolver.kt:776,2399`。
  - 未接入：
    - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt:388-389`（`requiredParameterCount`，在 `:182` 用于检查隐式 `super()`）；
    - `CfirRecursiveConstructorCallChecker.kt:244`。
- **现状**

```kotlin
val storage = runCatching { function.moduleData.session.cjmpMappingStorageOrNull }.getOrNull() ?: return false
val common = storage.commonFor(function) as? CfirFunction ?: return false
internal fun CfirConstructor.requiredParameterCount(): Int = valueParameters.count { it.defaultValue == null }
```

- **为什么不是框架级**
  - 例如：common 写 `init(x!: Int64 = 1)`，specific 的 open 类写 `specific init(x!: Int64)`。specific 模块里的子类走隐式 `super()` 时，会被误报 `NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS`。
  - `runCatching` 捕获的是 Throwable。在 IDE 里它会吞掉 `ProcessCanceledException`，也会掩盖 session 未绑定之类的不变式错误（见 J1）。
  - 读取前不做按需阶段推进，问题与 A1、A2 叠加。
- **证据**
  - 官方：
    - `CheckCJMP.cpp:958-963` 把默认值克隆到 specific 形参上，并设置 HAS_INITIAL，所以所有消费点都能看到默认值；
    - `TypeChecker.cpp:1523-1583` 的 `HasNonParamCtorForClass` 读的是 `param->assignment`。
  - Kotlin：`providers/.../declarationUtils.kt:312` 的 `itOrExpectHasDefaultParameterValue` 是共用入口，被以下位置共同调用：
    - `FirDefaultParametersResolver.kt:24`（从 `FirArgumentsToParametersMapper.kt:305` 进入）；
    - `FirElementSerializer.kt:886`；
    - fir2ir 的 `CallAndReferenceGenerator.kt:1624`。
  - 计划：D7 只登记了调用解析的读穿，检查器这些消费点没有登记。
- **框架级替代**
  - 在 providers 层提供 `itOrCommonHasDefaultValue`，对位 Kotlin 的 `itOrExpectHasDefaultParameterValue`，内部经 A2 的访问器按需推进阶段。
  - 参数映射、两个构造器检查器和 CJO 写侧都统一调用它。
  - 去掉 `runCatching`。
- **置信度与状态**
  - 代码路径确定。
  - 已提交。来源：resolve 组 #4。

#### D1　owner 组的泛型视图用错了 receiver，靠补回原始声明来修补

- **位置**：均在 `CfirGenericInstantiationChecker.kt`
  - `collectInstantiatedMemberSignatureGroups.collectOwnerSignatures`：`1118-1183`。其中泛型视图在 `1130-1134`，早退守卫在 `1148`，补签名在 `1160-1182`。
  - `collectInstantiatedMemberScopeSignatures`：`1470-1471`，泛型图里查不到的签名直接丢弃。
  - 联动：`CfirClassUseSiteMemberScope.kt:783-796` 的 `isImplementedByLocalFunction`。
- **现状**
  - 泛型视图用的是 nominal 自身的类型 `X<T>`，其中 T 没有约束。
  - 带 where 约束的 owner（`extend<U> X<U> where U <: Bound`）在这个视图里通不过约束检查，它的成员和接口边都不会进入泛型图。于是在第 1470 行、own-member recovery 之前，这些成员就被丢掉了。
  - 现在的补救办法是把 owner 的原始函数再加回来：

```kotlin
val genericOwnerSubstitution = createExtendDeclarationSubstitutionForConstraintDerivation(...)
val ownerDirectSignatures = ownerExtend.declarations.asSequence().filterIsInstance<CfirFunction>()
    .mapNotNull { it.toInstantiatedMemberSignature(..., genericSubstitutor = genericOwnerSubstitution.substitutor, ...) }
    .filterNot { d -> inheritedScopeSignatures.any { it.function === d.function } }
```

  - 这里用到的 `createExtendDeclarationSubstitutionForConstraintDerivation`，其 KDoc（`CfirExtendSubstitution.kt:194-199`）写明只供约束系统使用，此处越出了它的约定。
- **为什么不是框架级**
  - 官方做法：
    - 逐个 extend 调用 `CheckInstMemberSignatures(*extend, instTys)`；
    - 成员类型处在 extend 自身带 where 约束的泛型参数空间里，先在声明级按"未实例化的参数完全相同"归并，再实例化比较；
    - 官方从不拿 nominal 上无约束的 T 去重新检查 extend 的约束。
  - 补签名只恢复了 owner 自己的直接函数，以下形状仍会漏报：
    - **owner 成员是 public**。把 `privateExtendOwnerGroups.cj` 第 49 行的 `private` 改成 `public` 即可复现：
      1. 在具体视图里，`isImplementedByLocalFunction` 发现"来源不是 extend、且本地成员非 private"，判定为已实现，于是删掉了父链上的 default；
      2. owner 成员又因为泛型图里没有而被丢弃；
      3. 补回之后只剩一个稳定签名，于是漏报。官方在这种情况下仍会报一条。
    - **接口来自有界 owner 自身**，例如 `extend<U> X<U> <: J<U> where U <: Bound { public func f(v: Int64) }`。J 的 default 只出现在具体视图里，不会被补回。
    - **同约束的有界 peer**：它的成员同样不会被补回。
- **证据**
  - 官方：
    - `InstantiatedChecker.cpp:60-122`（第 81 行重新加入 private 函数）、`250-256`；
    - `StructInheritanceChecker.cpp:362-412`；
    - `MergeInheritedMemberHelper.cpp:139-177`（第 164 行做声明级的参数比较）。
  - Kotlin：`FirKotlinScopeProvider.kt:327-360` 在 unsubstituted scope 上建立 override 图，实例化时只再套一层 `FirClassSubstitutionScope`。
  - 计划：§11.3 第 605 行登记了 owner 规则；补签名、约束推导替换和早退守卫都没有登记。
- **框架级替代**：二选一，然后删除补签名。
  - owner 组的泛型视图改用 owner extend 自己声明的目标类型，也就是带约束的类型参数；
  - 或者整体改成"在声明级建立实现关系 + 替换 scope"的 K2 结构（见 D5）。
- **置信度与状态**
  - 确定。上面前两种漏报形状可以直接写成 LLT 验证。
  - 补签名和早退守卫未提交；owner 组本体已提交（`d82a4d1b2`）。
  - 来源：泛型实例化组 H1。

#### D2　早退守卫按 fixture 形状添加，掩盖了补签名绕过 CJMP 遮蔽

- **位置**
  - 守卫：`CfirGenericInstantiationChecker.kt:1145-1148`
  - 补签名（没有 CJMP 遮蔽过滤）：`CfirGenericInstantiationChecker.kt:1166-1181`
- **现状**

```kotlin
// re-adding raw owner declarations here would diagnose CJMP matching pairs at `Box<T>` / `Holder<T>`.
if (substitutions.values.any { it.containsUnfixedTypeParameterOrVariable() }) return inheritedScopeSignatures
```

- **为什么不是框架级**
  - 误报的根因：`ownerExtend.declarations` 直接取原始声明，没有经过 `isCjmpShadowedCommonDeclaration`。scope 那边在 `CfirExtendMemberScope.kt:198` 做了这层过滤，这里没有。
  - `instantiatedDirectExtends`（`1264-1287`）不排除已配对的 common extend，所以 common extend 也会成为 owner。
  - 守卫只挡住了"部分泛型实例"，具体实例化照样会误报。复现：在 `cjmpGenericOuterTypeParameterMembers.cj` 的 specific 模块里加 `func use(h: Holder<Int64>) {}`。已被 specific 替代的 common `extendIdentity(T)` 会被补回来，与 specific `extendIdentity(V)` 在 Int64 处冲突。
  - 两条路径行为分叉：CLI（CJO）路径下，common 成员的 origin 是 Library，在第 1675 行就被滤掉了，所以这个误报只出现在 LL/IDE 和多模块源码测试路径上。
  - 官方对部分泛型实例同样做检查（`InstantiatedChecker.cpp:203-261`）。守卫可能会跳过 `P<X, X>` 这类实例里真实存在的冲突。
- **证据**：官方 `CheckCJMP.cpp:387-445`，`MergeCJMPExtensions` 合并之后，common 成员不再单独参与检查。计划没有登记。
- **框架级替代**：随 D1 一起删除补签名；CJMP 遮蔽只在 C4 的统一判据处生效。
- **置信度与状态**
  - 较确定。还需补一个"specific 模块内具体实例化"的 e2e 用例。
  - 未提交。来源：泛型实例化组 H2。

#### D3　`isIndependentInterfaceDefault` 的 private 豁免没有官方依据

- **位置**：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendExtraChecker.kt:455-480`
- **现状**

```kotlin
val currentMemberIsPrivate = (currentMember as? CfirMemberDeclaration)?.status?.visibility == Visibilities.Private
return (implementsInterface || currentMemberIsPrivate) &&
        !context.session.extendRuleQueryService.areExtendsInInheritRelation(currentExtend, sourceExtend)
```

- **为什么不是框架级**
  - 官方的 shadow 判定链是 `DiagnoseForInheritedMember → CheckImplementationRelation → CheckInheritanceAttributes → CheckExtendMemberValid`（`StructInheritanceChecker.cpp:718,1043-1060,1111-1120,1358-1396`）。`CheckAccessVisibility` 报出 WEAK_VISIBILITY 之后仍返回 true，继续做 shadow 判定，中间没有"子成员是 private 就放行"的分支。
  - 这个条件的来历记录在计划 §11.3 第 603→605 行：先为 OwnerBox 普通库的误报放宽条件，导致 `default_implement_19` 回归，再收窄成现在的样子。
  - 反例：`extend B <: I1 {}` 加 `extend B { private func foo(): Unit {} }`。它和 `default_implement_19.cj` 第 32-43 行只差可见性。按官方源码应同时报 WEAK_VISIBILITY 和 EXTEND_MEMBER_CANNOT_SHADOW，现在只报前者。
  - 本批新增的 `privateFunctionsCauseGenericInstantiationAmbiguity.cj` 期望只有 WEAK_VISIBILITY，找不到对应的 cjc 取证记录。
  - OwnerBox 误报的真实根因没有定位，即为什么 `f(Int64)` 会被判定为与 peer 注入的 default 同签名。
- **证据**：计划 §11.3 第 603、605 行登记的是收窄过程，没有官方出处。
- **框架级替代**
  - 先定位 OwnerBox 误报在签名比较上的根因。
  - shadow parent 的判定对齐官方的 peer 收集规则，并在声明级比较参数。
  - 删除 `implementsInterface` 和 private 这两个启发式条件。
- **置信度与状态**
  - 可能。需要用 cjc 1.1.3 跑上面的反例和 `.workbuddy/tmp/cjmp_probe113/private_function_instantiation.cj`。
  - 未提交。来源：泛型实例化组 H5。

#### D4　两段式测试的期望被翻转

- **位置**
  - 测试：`compiler/frontend/test/org/cangnova/cangjie/frontend/pipeline/CjmpTwoPhaseCompilationTest.kt:806-816`（`deserialized generic extend peers honor target substitutions and bounds`）
  - 实现侧：`CfirGenericInstantiationChecker.kt:1598-1603`、`1675`，只接受 Source 和 SubstitutionOverride 两种 origin
- **现状**
  - HEAD 断言"恰好一条 GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS，并带 2 个 CJO 定位的 note"。
  - 工作树把期望改成了"一条都没有"，理由是 "the satisfied private peer remains invisible to a cross-package CJO consumer"（主审查者核实了这处 diff）。
- **为什么不是框架级**
  - 计划 §11.3 第 596 行记录：cjc 1.1.3 探针在 `Int64 <: Bound` 成立时报一条。第 597 行写明下一步是"保留官方报告候选"。改后的期望与这两条记录相反。
  - 按官方源码推导，应当报这一条：
    - 有界 extend 是和 OwnerBox 同包的直接扩展，`ExtendDecl::IsExportedDecl` 为真（`Node.cpp:1180-1225`），因此会进入 `GetVisibleExtendsForInstantiation`；
    - `CheckInstMemberSignatures(ext2)` 会把它自己的 private `f(Int64)` 重新加入（第 81 行），与 peer 的 `f(T)`→`f(U)` 在 Int64 处冲突；
    - "peer private 不可见"只对 public owner 那一组成立，对有界 owner 自己那一组不成立。
  - CFIR 报不出来，原因有两个：
    - CJO 成员的 origin 是 Library，会被过滤掉。这是既有行为；LLT 夹具用同一编译里的源码模拟导入包，所以一直没暴露。
    - D1。
- **证据**
  - 计划 §11.3 第 596-597 行。
  - 官方 `InstantiatedChecker.cpp:81-84,125-141,250-256`、`ImportManager.cpp:1416-1460`。
  - 这次翻转没有登记，而且与计划里已登记的结论矛盾。
- **框架级替代**
  - 先用当前的库源码（已去掉非法的 `public extend`）重跑一次两段式探针，把结论固定下来。
  - 然后恢复原期望，让反序列化成员参与实例化成员图，并配合 D1 一起修。
- **置信度与状态**
  - 较确定。
  - 未提交。来源：泛型实例化组 H3。

#### E1　抽象类成员检查的条件与官方不一致

- **位置**：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt:320-340` 的 `checkCJMPAbstractClassMembers`。这是 2026-04 遗留的实现（`e2c13bfb`）。
- **现状**

```kotlin
if (member !is CfirNamedFunction) continue
if (!member.status.isOpen && !member.status.isAbstract && !member.status.isStatic) {
    if (member.body != null && !member.status.isSpecific) continue
    reporter.reportOn(..., CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER, a = decl.name, b = "function", c = "open/abstract")
```

- **为什么不是框架级**
  - 官方条件是 `inAbstractCJMP && !isCJMP && !isAbstract && !compilerAddedOrConstructor && !hasBody && !FROM_COMMON_PART`（`src/Sema/DeclAttributeChecker.cpp:317-322`，主审查者核实），对函数和属性都适用。
  - 本仓库会误报两种合法写法：
    - `common abstract class A { common func f(): Int64 }`。raw builder 不给这个成员打隐式 abstract（`PsiRawCfirBuilder.kt:4019-4021`），status resolve 又把它定为 FINAL（`CfirStatusResolveProcessor.kt:1186-1201`），于是被报。
    - `specific abstract class A { specific func f(): Int64 { 1 } }`。
  - 与官方的其他差异：
    - 不检查属性；
    - `isOpen`、`isStatic` 豁免是官方没有的；
    - 诊断参数是 `(类名, "function", "open/abstract")`，官方是 `(cjmpKind, memberKind, cjmpKind)`（`DiagnosticSema.def:310`）。
  - 6 个含 CJMP 抽象类的 fixture 都没有覆盖上述形状。
- **证据**：计划 §9.8 把这条列为"已有 fixture 覆盖"，判定条件的偏差没有登记。
- **框架级替代**
  - 把官方 `CheckCJMPAttributesForPropAndFuncDeclInClass` 的完整条件移植到 class 成员属性检查处，包括函数和属性、`isCJMP` 豁免、FROM_COMMON_PART 豁免。
  - 修正诊断参数的形状。
  - 与 E10、E13 一起处理。
- **置信度与状态**
  - 确定（主审查者核实）。
  - 已提交。来源：检查器组 #1、resolve 组 #5。

#### E2　注解匹配是单向的，漏掉 nominal，排除表取自 origin/main

- **位置**：均在 `CfirCommonSpecificChecker.kt`
  - `844-864`：nominal 分支在这里提前 return
  - `927-936`
  - `572-644`：`annotationMatches`、`SPECIAL_HANDLED_*`、`firstUnmatchedSpecificAnnotation`
- **现状**

```kotlin
if (specific is CfirClassLikeDeclaration && common is CfirClassLikeDeclaration) { ...; return }  // nominal 永远不做注解比较
val unmatched = firstUnmatchedSpecificAnnotation(specificAnnotations, commonAnnotations)       // 只报 specific 多出来的第一个
```

- **为什么不是框架级**
  - **方向**：官方 `MatchCJMPDeclAnnotations` 是双向比较，每个失配都报（`CheckCJMPAnnotations.cpp:220-241,268-287`）。
  - **比较内容**：官方 `AnnotationEquals` 连实参一起比较（`:89-110`），本仓库只比 kind/ClassId。
  - **nominal**：官方对 nominal 同样校验（`ValidateMatchedAnnotationsAndModifiers`，`CheckCJMP.cpp:648-664`），本仓库提前 return，完全跳过。
  - **"common 单侧的注解不报诊断"是误读探针得出的规则**：
    - fixture `cjmpAnnotationCommonOnly`（探针 T05）没有诊断，真实原因是官方 CJO 不序列化 `@OverflowWrapping`（`ASTWriter.cpp:1648-1757` 的 default 分支）；
    - CUSTOM 注解在 COMMON/SPECIFIC 声明上会被写出（`:1727-1741`），也会被读回（`ASTLoader.cpp:634-680`）；
    - 本仓库把这一个探针形状泛化成了"common 单侧注解永不报"。
  - **代码注释有误**：注释说"common 注解在匹配后会传播到 specific"。实际上官方只传播 `@Deprecated` 和 `@Attribute`（`CheckCJMPAnnotations.cpp:380-384`）。
  - **排除表没有区分版本**：14+6 项与 origin/main 一致，与 v1.1.3 不同，而且没有语言版本门禁。
    - v1.1.3 的 NON_SERIALIZED 是 {C, JAVA_MIRROR, JAVA_HAS_DEFAULT, OBJ_C_MIRROR}，UNSUPPORTED 是 {JAVA, CALLING_CONV, CONSTSAFE, ENSURE_PREPARED_TO_MOCK}（`:44-53`）；
    - 本仓库多排除了 OBJ_C_INIT、OBJ_C_OPTIONAL、FOREIGN_GETTER_NAME、FOREIGN_SETTER_NAME、NON_PRODUCT；
    - 平台侧又缺了 JAVA_MIRROR。
  - **缺属性位比较**：官方 `PostCheckNonSerializedAnnotations` 按属性位比较 C、JAVA_MIRROR、JAVA_HAS_DEFAULT、OBJ_C_MIRROR（`:171-196`），本仓库没有。
- **证据**
  - 计划 §10 Phase 3 第 2 批和 §9.8 登记了反例，但登记的理由与官方源码不符。
  - 排除表的来源版本没有登记，也没有按语言版本区分。
- **框架级替代**
  - 建立统一的"CJO 可见注解"分类表，与序列化写侧共用。各官方版本的表分别定义，由语言版本门禁选择（§12 决策 1）。
  - 对全部已配对声明（包括 nominal）做双向一一匹配，并比较实参。
  - 分层参照 Kotlin：`AbstractExpectActualAnnotationMatchChecker`，加上独立的 `FirActualAnnotationsMatchExpectChecker`。
  - E7 共用同一张分类表。
- **置信度与状态**
  - 代码级确定。"两侧都有 `@OverflowWrapping` 时官方也会报"是推论，待探针确认。
  - 已提交。来源：检查器组 #2。

#### E3　超类型只比较个数

- **位置**：`CfirCommonSpecificChecker.kt:847-853`
- **现状**

```kotlin
if (specific.superTypeRefs.size != common.superTypeRefs.size) { report(SPECIFIC_HAS_DIFFERENT_SUPER_TYPE); return }
```

- **为什么不是框架级**
  - 官方先按泛型映射做替换，再逐个比较接口类型，并比较超类类型（`CheckCJMP.cpp:818-856`）。Kotlin 的 `AbstractExpectActualChecker.areCompatibleSupertypes` 同样按类型比较（`:164-200`）。
  - 本仓库 matcher 的 class-like 分支只看种类和泛型参数个数（`AbstractCjmpMatcher.kt:23-33`），所以超类型实际上没有任何地方在检查。
  - 结果：`common class S <: I` 对 `specific class S <: J`、`<: Base` 对 `<: Base2`，都会静默通过。
  - fixture `cjmpSuperTypeMismatch` 只测了个数不同的情况（1 个对 0 个）。
- **证据**：计划没有登记。
- **框架级替代**：在验证阶段（即 E12 的 `AbstractCjmpChecker`）按类型比较超类型是否兼容，并使用存储中的类型参数映射。
- **置信度与状态**：确定（主审查者核实）。已提交。来源：检查器组 #3。

#### E4　修饰符冲突表不区分作用域，还在 extend 处打了补丁

- **位置**
  - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/Compatibility.kt:74-77`
  - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifiersCompatibilityUtils.kt:60-67`
- **现状**

```kotlin
// 注释：官方 ParserModifierRules.cpp 各作用域表均含 CR(COMMON, PRIVATE, SPECIFIC)
result += incompatibilityRegister(COMMON_KEYWORD, SPECIFIC_KEYWORD, PRIVATE_KEYWORD)
if (owner is CfirMemberDeclaration && setOf(a, b) == setOf(COMMON_KEYWORD, PRIVATE_KEYWORD)) {
    val containingExtend = context.findClosestDeclaration<CfirExtend>()
    if (containingExtend != null && containingExtend !== owner && containingExtend.status.isCommon) return }
```

- **为什么不是框架级**
  - 注释的前提不成立。v1.1.3 中 COMMON 在各作用域的冲突规则如下：
    - 只有顶层的各张表才有 PRIVATE 与 COMMON/SPECIFIC 的冲突（`ParserModifierRules.cpp:73-78,94-99,104-113,118-126,132-153`）。
    - class/interface/struct/enum 体内的函数、class/struct 体内的变量：COMMON 只与 CONST、SPECIFIC 冲突（`:82-89,167-243`；主审查者核实了 `CLASS_BODY_FUNCDECL_MODIFIERS`）。
    - prop、extend 体、init：COMMON 只与 SPECIFIC 冲突（`:245-319,335-344`）。
  - 本仓库用一张全局表，只为 fixture `commonSpecificExtensionCommonPrivate` 在"common extend 成员"上打了豁免。
  - 误报：
    - class/struct/enum 成员上的 `common private`、`specific private`；
    - specific extend 成员上的 `specific private`。
  - 漏报：`common const`、`common foreign`、`specific const` 等冲突（`:130,136-139,143,149-153`）。
- **证据**
  - 计划 §10 Phase 0 的"冲突表"一行登记了这张表，但前提写错了。
  - extend 处的豁免没有登记。
- **框架级替代**
  - 冲突查询按修饰符目标（声明种类、位置、所在容器）分表，与官方 `SCOPE_MODIFIER_RULES` 对位。
  - 删除 extend 特判。
  - 登记本仓库与 Kotlin 全局冲突表的差异。
- **置信度与状态**
  - 确定。主审查者核对了官方表的内容；建议再对 class 成员上的 `common private` 跑一次探针。
  - 已提交。来源：检查器组 #5。

#### F1　形参类型等价用手写的结构比较，最后退回 `==`

- **位置**
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatcher.kt:86-145`（`typesEquivalent`，兜底在 `:143-144`）
  - 调用方：`CfirCjmpMatchingContext.kt:94-99`（`areTypesEquivalent`）、`CfirCjmpMatcher.kt:60-75`（`matchEnumConstructors`）
- **现状**
  - 只对 TypeParameter、ClassLike、Primitive 三类做了映射感知的比较，其余一律走 `specificType == commonType`。
  - `ConeFunctionType`、`ConeTupleType`、`ConeVArrayType` 都不是 `ConeClassLikeType`，它们的 `equals` 会逐个比较内部类型。
  - `ConeTypeParameterType` 按 lookup tag 判等，而 lookup tag 是按符号比较的 data class。所以 common 的 `T` 永远不等于 specific 的 `T`。
- **为什么不是框架级**
  - 以下形状都会得到 PARAMETER_TYPES，双向报 NOT_MATCHED：
    - `common func apply<T>(f: (T) -> Unit)` 与同形状的 specific；
    - 泛型类成员 `f(p: (T, Int64))`；
    - 泛型 enum 载荷 `Value((T) -> Unit)`。
  - 官方先替换泛型参数（`MapCJMPGenericTypeArgs`/`GetInstantiatedTy`）再比较，这些形状都能配上。
  - 仓库里同一件事有三份实现，只有形参和载荷这一份是残缺的：
    - 返回类型：`CfirTypeSubstitutorByMap` 替换 + `AbstractTypeChecker.isSubtypeOf`（`CfirCjmpMatchingContext.kt:101-121`）；
    - checker 的跨 session 重载比较：替换 + `equalTypes`。
  - 次要风险（可能）：嵌套的 `ConeClassLikeType.equals` 会比较 attributes。CJO 反序列化出的类型与源码类型的 attributes 不同时，连非泛型的 `(String)->Unit` 也可能被误判。
- **证据**
  - 官方：`CheckCJMP.cpp:921-933,1014-1029`、`TypeManager.cpp:1962-1965`。
  - Kotlin：`AbstractExpectActualMatcher.kt:176-194` 先建 substitutor；`FirExpectActualMatchingContextImpl.kt:344-374` 用 `AbstractTypeChecker.equalTypes` 比较。
  - 计划：§10 Phase 2 登记的是"ClassId + 实参结构相等"，没有考虑非 class 类型；语料里也没有函数或元组形参的用例。
- **框架级替代**
  - CFIR context 只保留一个实现：先用映射替换 common 类型，再 `equalTypes`。
  - `areTypesEquivalent`、`matchEnumConstructors`、`haveSameExtendKey` 全部复用这一个实现。
- **置信度与状态**
  - 确定（主审查者核实了兜底代码）。
  - 已提交。本轮只把 `ConeErrorType` 的判断改成了 `containsErrorType`。
  - 来源：配对引擎组 #2。

#### F2　模式变量走一条绕开共享 first-fit 的专用路径

- **位置**
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchRunner.kt:213-250`（`matchPatternVariable`）
  - 后置条件：`CfirCjmpMappingStorage.kt:275-277`（`hasResolutionResult`）、`analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt:56-63`
- **现状**

```kotlin
if (matched.isEmpty() || matched.any { it == null }) { storage.recordUnmatched(declaration); return }
if (bindings.size == 1) storage.bind(declaration, matched.single()!!, parentTypeParameterMapping)
else bindings.zip(matched).forEach { (b, c) -> storage.bind(b, c!!, parentTypeParameterMapping) }
```

- **为什么不是框架级**
  1. **LL 可能抛异常**
     - 元组全部配对成功时，只有各个 binding 变量有记录，`CfirPatternVariable` 本身没有结果。
     - LL 的 `check(hasResolutionResult)` 因此会对合法的 `specific let (a, b) = (1, 2)` 抛 IllegalStateException。
     - 官方只禁止 `common` 模式声明（`ParseCJMPDecl.cpp:251-254`，主审查者核实），specific 的元组模式是合法的。
     - eager 没有这个后置检查，所以静默通过。同一个不变量只在一条路径上执行。
  2. **全有或全无**
     - 官方 `TryMatchVarWithPatternWithVarDecls` 逐个变量配对：成功的逐个绑定；失败的逐个报 NOT_MATCHED，再给整个模式报一次；非元组模式或含 `_` 时 `matchedAll=false`。
     - CFIR 只要有一个变量失败就全部不绑，已经能配上的 common 会被误报 NOT_MATCHED。
  3. **绕开 first-fit**
     - `bind` 的返回值被忽略，也不记录失配，绑定失败后不会继续尝试下一个候选。
     - 另外，官方对全局变量直接赋值，不经过 TrySetSpecificImpl（`CheckCJMP.cpp:1075-1077`），不会产生 MULTIPLE。
- **证据**
  - 官方 `CheckCJMP.cpp:1090-1112,1115-1156`。
  - "全有或全无"只写在 KDoc 里，计划没有登记为偏离；testData 里没有 `specific let (` 的用例。
- **框架级替代**：把每个 binding 作为独立的 specific 送进共享的 first-fit，同时为 `CfirPatternVariable` 本身记录一个模式级的结果。
- **置信度与状态**
  - 语义偏离确定。LL 抛异常属于可能，需要一个 LL fixture 确认：`specific let (a, b) = (1, 2)` 对应两个 `common let`。
  - 已提交。
  - 来源：配对引擎组 #3。LL 组 #10 把此条定为低严重度，前提是"官方在解析期拒绝所有 CJMP 模式声明"；经核实该前提不成立，官方只禁止 `common`。

#### G1　生产 LL 会话拿不到 CJMP 模式和 dependsOn 关系

- **位置**
  - 测试注入点：`analysis/low-level-api-cfir/testFixtures/org/cangnova/cangjie/analysis/low/level/api/cfir/resolve/AbstractCfirCjmpMatchingTest.kt:74-78`（`doTestByMainFile`）
  - 模式默认值：`cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/CfirCjmpSettings.kt:29-31,90-95`
  - LL 会话工厂：`analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/sessions/LLCfirAbstractSessionFactory.kt:719-734`，其中 `registerAllCommonComponents` 不注册 CJMP 设置
  - 只声明、从不写入 dependsOn 字段的地方：
    - `analysis/analysis-api-standalone/src/.../projectStructure/CaStandaloneModules.kt:50`
    - `lsp/src/.../AnalysisApiLspProjectStructure.kt:684,803`
    - 外部 `intellij-ide` 的 `CaIdeMutableModule.kt:26`
- **现状**

```kotlin
val mode = if (mainModule.name == "modeNone") CfirCjmpMode.NONE else CfirCjmpMode.SPECIFIC
specificSession.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = mode))
```

  - 写入 `CfirCjmpSettingsComponent` 的只有三处：eager 工厂（`cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirSessionFactoryContextUtils.kt:64`）、测试门面 `CfirFrontendFacade.kt:302`，以及测试本身（主审查者核实）。
  - LL 会话始终用默认的 `DefaultCfirCjmpSettingsComponent`，也就是 NONE。
- **为什么不是框架级**
  - CJMP 角色是模块的事实，应当由 LL session 工厂从 `CaModule` 推导（D14 写的是"IDE 由模块 kind 决定"）。现在它只由测试在会话建好之后，按模块名字符串注入。
  - 在 LSP/standalone 下的后果：
    - `CfirCjmpFilePartChecker.kt:53-68` 在 mode=NONE 时，会给每个含 `common`/`specific` 的文件报 `PARSE_*_IN_NON_*_FILE`，并且从不配对；
    - LL resolver、同名组预处理、阶段门、depends-on extend 合并在生产中都走不到。
  - §11/§11.5 的 LL 验收只证明了"测试注入"这条路径可用。
  - 测试本身的问题：
    - 忽略了 fixture 里的 `// CJMP_MODE: SPECIFIC` 指令（`firstFitCandidateDiagnostic.cj:15`）；
    - 按 `firstMatch`、`parallelFirstMatch` 等模块名走不同分支，按 `Box`、`shared`、`select` 等名字取声明（`AbstractCfirCjmpMatchingTest.kt:80,103,154,169`）。
- **证据**
  - 官方 v1.1.3 `include/cangjie/Option/Option.h:473,1145-1152`：模式是编译调用本身的事实。
  - Kotlin `LLFirExpectActualMatcherLazyResolver.kt:46`：在 resolver 构造时就固定 `enabled`。
  - 计划只部分登记：
    - D14 登记了设计；
    - `CfirCjmpSettings.kt` 的 KDoc 写了"未落地前为 NONE"；
    - §6 把 LSP 列为非目标；
    - 但 §11 开头写的"mode 在测试/IDE session 创建后注入"与代码不符，IDE 这条路径并不存在。
- **框架级替代**
  - LL session 工厂在建 session 时，按 `CaModule` 暴露的 CJMP 角色注册该组件，例如：
    - `directDependsOnDependencies` 非空 → SPECIFIC；
    - 有实现模块或 `isCommon()` → COMMON；
    - dangling 模块继承 context 模块的角色。
  - standalone、LSP、IDE 各平台的 provider 负责写入 dependsOn 边。
  - 测试改为读 `// CJMP_MODE` 指令，走同一个入口；断言改为存储 dump 加 golden 文件。
- **置信度与状态**
  - 确定（主审查者核实）。
  - 已提交；未提交的测试修改保留并扩展了按模块名分支的写法。
  - 来源：LL 组 #1、#9。

#### H1　`_CNat3AnyE` 硬编码：为了让合成的默认上界能往返而打的点补丁

- **位置**
  - 读侧：`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CjoFullIdResolver.kt:107-130`（未提交）
  - 写侧 Any 分支：`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt:676-696`（未提交）
  - 根因，均已提交：
    - `CfirCjoPackageMetadataProducer.kt:262-280`：`genericMetadata` 给每个类型参数都写 constraint（第 276 行 `upperBounds = parameter.bounds...`）；
    - `:628-638`：Generic 类型写出 `declaration.bounds`。
- **现状**

```kotlin
val standardLibraryExportIds: Map<String, ClassId> = mapOf("_CNat3AnyE" to StdlibClassIds.Any)
// 写侧：declarationKey = if (classId == StdlibClassIds.Any) "_CNat3AnyE" else classId.relativeClassName.asString()
```

- **为什么不是框架级**
  - **Any 引用只来自本仓库自己的写法**
    - `addDefaultBoundIfNecessary`（`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/utils/CfirDeclarationBuildingUtils.kt:17-22`）会给无约束的类型参数合成一个 `Any` 上界，写侧把它当成显式约束写进了 CJO。
    - 用官方 cjc 1.1.3 编译同一份库源码得到 `.workbuddy/tmp/cjmp_probe113/ownermatrixlib.cjo`，其中 `_CNat3AnyE` 出现 0 次，`_CNat6ObjectE` 出现 1 次（主审查者核实）。
  - **只补了 Any 这一种形状**
    - 写侧对其他外部类型写的是短名（如 `"String"`），而读侧的跨包索引（`CjoPackageHeader.kt:160`）对官方声明只按 exportId 建键。
    - 真实 SDK 环境下，`String`（官方 exportId 为 `_CNat6StringE`）、`Object` 等引用都无法解析（推断）。
    - 测试环境没有 std.core.cjo，除 Any 外的标准库类型同样全部解析失败；而测试只用了 Int64 和用户类型，所以没暴露。
  - **可能掩盖加载失败**：这条捷径在正常解析之前执行。std.core.cjo 被版本门拒载时，Any 仍能解析，其他类型却失败，故障会表现为"只有某些类型坏了"。
- **证据**
  - 官方 `ASTWriter.cpp:1125-1162`：`SaveGeneric` 只写显式的 `genericConstraints`。
  - Kotlin `compiler/fir/fir-serialization/.../FirElementSerializer.kt:968-969`：跳过隐式默认上界，反序列化时再补回。
  - 计划 §11.3 登记了这条捷径，理由是"消费端不保证有 std.core.cjo"；真正的根因（写出了合成上界、跨包键约定不对称）没有登记。
  - 测试侧：
    - 未提交的 `CjoFullIdResolverTest.kt:98-129` 断言了这张硬编码表的边界；
    - `CjmpTwoPhaseCompilationTest.kt:798-805` 把"没有 std.core CJO 时 Any 也能解析"固化成了验收项。
- **框架级替代**
  - 写侧跳过隐式默认上界，只写显式的 where 约束；读侧用 `addDefaultBoundIfNecessary` 补回；两端都删除 `_CNat3AnyE`。
  - 跨包 FullId 的键需要唯一的 owner：要么是 MANGLING 阶段产出的 exportId，要么是读写两侧对称的键约定。
  - 没有 std.core cjo 时，标准库声明由 builtins provider 负责，而不是在 FullId 解析器里处理。
- **置信度与状态**
  - Any 的来源确定（主审查者核实）；SDK 环境下 String/Object 解析失败属于推断。
  - 捷径未提交，根因已提交。
  - 来源：序列化组 H2。

#### H2　common part 按"父目录 + 包名搜索"加载，而不是按显式路径

- **位置**（均已提交）
  - `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirSessionFactoryContextUtils.kt:42-56`
  - `compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/CfirFrontendPipelinePhase.kt:344-359`
  - `CjoManager.kt:46-72`
  - `CjoSearchPath.kt:60-84`
- **现状**
  - `librarySearchRoots = (classpath + commonPartRoots)`：common part 的父目录被追加到库搜索根里，并且排在 classpath 之后。
  - `dependsOnDependencies(paths)` 只用精确路径给模块**归类**，不负责**加载**。
- **为什么不是框架级**
  - **不尊重显式路径**：只要任意一个 classpath 目录里有同包的 cjo（例如上一次构建的产物），就会先命中它。实际加载的文件与配置的路径不一致，于是被当作普通库，结果全部报 NOT_MATCHED，却没有任何关于 common part 的报错。
  - **wrong-package 门在官方命名下走不到**：
    - 只有经遗留路径回退才会触发。测试是把 `other_pkg` 的 cjo 复制成 `cjmp_p.cjo` 才走到这个门的（`CjmpTwoPhaseCompilationTest.kt:866-883`）。
    - 按官方的 `<pkg>.cjo` 命名，根本查不到这个文件，只剩下级联的 NOT_MATCHED。
  - **副作用**：父目录会被递归 `walkTopDown` 建索引，里面无关的 cjo 都变成了可以 import 的库。
- **证据**
  - 官方 v1.1.3 `src/Modules/CjoManager.cpp:460-476` 直接 `ReadCjo(*globalOptions.commonPartCjo)`；`:478-489` 比较实际包名与期望包名，不一致就报错。
  - Kotlin `MetadataFrontendPipelinePhase.kt:45-52` 把 refines 路径同时绑定为 dependsOn 模块，并按显式路径加载库。
  - 计划 §10 Phase 4 的"模块装配"一行登记了"其目录并入库搜索路径"。
- **框架级替代**
  - 提供专门的 common part 加载入口，只读取配置指定的那个文件。
  - 用该文件 header 里的包名与源码包名比对。
  - 加载结果作为 depends-on 模块的 provider，不把它所在的目录并入通用搜索根。
- **置信度与状态**
  - 代码层面确定；上述两种真实触发场景属于可能。
  - 已提交。来源：序列化组 H3。

#### H3　CLI 路径下的 common 编译不产出 CJO

- **位置**（均已提交）
  - `compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/AbstractFrontendPipeline.kt:24-37`：只在 `compileCjd` 时设置 CJO 输出。
  - `CfirFrontendPipelinePhase.kt:201-230`：`writeLiveCjoOutputs` 在没有输出目录时静默返回 true。
- **现状**
  - `-Xcjmp-compile-common` 只切换编译模式（`configuration.cjmpChirOutput = arguments.cjmpCompileCommon`），既不写 cjo 也不报错。
  - 生产代码中，`cjoOutputDirectory` 只在 `AbstractFrontendPipeline` 里有两处赋值（主审查者核实）。
  - 测试都绕过了这个入口：
    - `CjmpTwoPhaseCompilationTest.kt:176-195,324-361` 直接调用 `CfirFrontendPipelinePhase.executePhase`，并手动设置 `cjoOutputDirectory` 等配置键；
    - CLI 测试（`CangJieCLICompilerTest.kt:29-45`）用的是桩 pipeline。
- **为什么不是框架级**
  - 两段式编译只有在测试手动设好配置时才成立，从生产入口跑不通。
  - §11.4 却依据桩测试宣布 CLI 已经闭环。
- **证据**
  - 官方 v1.1.3 `ImportManager.cpp:296-298`：在 CHIR 输出模式下设置 SerializingCommon 并写 cjo。
  - 计划中的探针命令 `cjc common.cj --output-type=chir` 也会产出 `<pkg>.cjo`。
  - 计划没有登记。
- **框架级替代**
  - 在配置阶段把 `-o`/`--output-dir` 映射到非 cjd 编译的 CJO 输出。common 编译模式下必须产出 CJO。
  - 两段式 e2e 改为经 `CangJieCLICompiler.exec` 或 `AbstractFrontendPipeline.execute` 驱动。
- **置信度与状态**
  - 确定（主审查者核实）。
  - 已提交。来源：序列化组 H4。

### 3. 中低严重度问题（48 条）

以下每条都列出位置、问题、依据、框架级替代和状态。

#### A3　同名组被整组重跑，失配记录不幂等（中）

- **位置**
  - `CfirCjmpMatchingProcessor.kt:130-145`
  - `CfirCjmpMappingStorage.kt:128-169`（`bind`）、`:243-252`
  - `LLCfirCjmpMatchingLazyResolver.kt:102-122`
  - `CfirCjmpResolver.kt:63-134`
- **问题**
  - eager 每访问同名组里的一个成员，就把整组重新配对一遍：N 个重载要做 N² 次匹配。
  - LL 在目标声明的锁内重配整组，把兄弟声明的结果也写进去，但兄弟声明的阶段并不推进。
  - 失配记录只在首次绑定成功时清空。重跑会在已绑定的声明上重新写入失配记录，还会记一条 `SECOND_BINDING`（:136-140）。`:147` 是死代码。
  - 示例：重载 `S1 f(Int64)` 和 `S2 f(String)` 分别对应 `C1`、`C2`。eager 跑完后 `mismatchKindsFor(S2)=[PARAMETER_TYPES]`；而 LL 只请求 S2 时，结果为空。
  - 项目自己的 `AbstractCfirCjmpMatchingTest` 把这些字段当作 eager 与 LL 必须一致的状态来比较（:50-56、:91-99），只是现有 fixture 没覆盖到这种情况。
  - enum 构造器不参与分组（`CfirCjmpResolver.kt:68`）。同一构造器重复声明时，谁先被解析，谁就绑定。
- **依据**
  - Kotlin 每个声明只计算一次（`FirExpectActualMatcherTransformer.kt:103-115`），并且在某个声明的锁下只写它自己的状态。
  - 官方只遍历一遍 specificDecls（`CheckCJMP.cpp:1193-1201`）。遍历顺序只影响"第二次绑定"的诊断报在哪个声明上（`:898-912,1115-1158`）。
  - §11.1 把这里登记为"幂等"，实际并不成立。
- **框架级替代**（任选其一，至少做到第三条）
  - 同名组的结果按组只计算一次，每个 specific 的结果一次性写入。
  - 或者：matcher 只按单个声明计算兼容性，"第二次绑定"由 checker 按官方顺序（文件名 + 声明顺序）推导。
  - 最低要求：让 `bind` 和失配记录在同一个事务里完成。
- **状态**：已提交；确定。来源：配对引擎组 #6、LL 组 #8。

#### D5　scope 按实例化后的签名判定实现关系，`memberOwnerExtend` 只在 owner 模式下修补（中）

- **位置**
  - `CfirClassUseSiteMemberScope.kt:757-796`，其中 `isImplementedByLocalFunction` 在 783-796
  - `CfirClassUseSiteMemberScope.kt:1246-1247`（`buildParentScopes` 里的传播）
  - `CfirExtendMemberScope.kt:192-197`（owner 模式下的 private 过滤）
- **问题**
  - 根因：scope 把实例化后的类型当作 ownerType，在具体签名上判定"本地成员实现了父成员"。只有在实例化后才相等的成员，也会被当作 override 吞掉。
  - 本批的修补只在 owner 组里生效，nominal 组和其他消费方仍然会吞。
  - 反例：把 `privateExtendOwnerGroups.cj` 第 44-50 行换成 `class Leaf<T> <: Child<T> { public func inherited(value: Int64): Unit {} }`。在 `Parent<Int64>` 这一层，Parent 的 private extend 成员先把 default 吞掉了，于是 `Leaf<Int64>` 漏报；官方会报。
  - 计划第 598 行把这次改动称为"框架修复"，与实际不符。
- **依据**
  - 官方在声明级判定（`MergeInheritedMemberHelper.cpp:164`）。
  - Kotlin 在 unsubstituted scope 上建立 override 图（`FirKotlinScopeProvider.kt:327-360`）。
  - 这一 K2 结构偏离没有登记。
- **框架级替代**
  - 在 scope 层按声明级签名判定实现关系。
  - 祖先 extend 的 private 成员，对所有消费方统一不进入继承图。
  - 去掉 `memberOwnerExtend` 开关。与 D1 同一批做。
- **状态**：规则与传播未提交；private 过滤已提交（`6e58cab6b`）。确定。来源：泛型实例化组 M1。

#### D6　内建目标（CPointer）路径新增的可见性过滤丢掉了 owner 自己的 private（中）

- **位置**：`CfirGenericInstantiationChecker.kt:974-1012`，新增的过滤在 1000-1006。
- **问题**
  - 官方对内建泛型目标同样逐个 extend 分组，并把 owner 自己的 private 成员加回来（`InstantiatedChecker.cpp:81-84,125-141`）。
  - CFIR 这里用的是另一套扁平合并算法。file checker 的上下文里没有 extend，所以 `privateAccessible`（`CfirAccessibilityChecker.kt:575`）必然判为不可见。可见性的判定视角也因此从 owner 变成了 use-site。
  - 反例：`extend<T> CPointer<T> { private func g(v: T) {}; public func g(v: Int64) {} }`。`CPointer<Int64>` 官方会报，改动后不报。
- **框架级替代**：内建目标复用 nominal 的 owner 组构造，可见性统一从 owner 视角判定。
- **状态**：未提交；确定。来源：泛型实例化组 M2。

#### D7　跨包的 EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE 挂在实例化检查器上（中）

- **位置**：`CfirGenericInstantiationChecker.kt:1341-1365`。对照 `CfirImportsChecker.kt:130-149`、`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendRuleQueryService.kt:321-335`。
- **问题**
  - 官方在 `CheckMembersWithInheritedDecls(extend)` 里报这个诊断，对象包括本包的 extend 和 `GetAllNeedCheckExtended` 选出的导入 extend（`StructInheritanceChecker.cpp:335,567-627,1454`），与是否实例化无关。
  - CFIR 要同时满足三个条件才报：owner 是泛型 extend（1271 行）、当前文件实例化了该类型、owner 有 source。因此以下情况都不报：
    - 非泛型目标；
    - 没有被实例化的类型；
    - 来自 CJO 的 owner。
  - `reportImportedExtendConflicts` 里没有做顺序检查。
- **依据**：计划 §11.2 第 582、589 行登记了这一设计。
- **框架级替代**：在导入 extend 的复查点上，对跨包的 extend 组调用同一个 `extendMemberPeerDecision` 来报告。
- **状态**：已提交（`d82a4d1b2`）；确定。来源：泛型实例化组 M3。

#### D8　CJO owner 的可见性用手写规则近似（低）

- **位置**：`CfirGenericInstantiationChecker.kt:1324-1328,1451-1460,1520-1530`。
- **问题**
  - 官方 `IsInvisibleMember` 只依赖包关系（`StructInheritanceChecker.cpp:56-65`）。
  - CFIR 在拿不到文件时退回 `samePackage || isPublicAPI` 的近似判定，"internal 对子包可见"之类的规则会与官方不一致。
  - 这相当于在源码路径和 CJO 路径之间加了一层兼容层。计划没有登记。
- **框架级替代**：统一走 accessibility checker 判定可见性，由 CJO 声明提供它需要的包和可见性元数据。
- **状态**：已提交；可能。来源：泛型实例化组 L1。

#### D9　共享 scope 带了只为一个检查器服务的专用开关（低）

- **位置**：`CfirClassUseSiteMemberScope.kt:238-243,1466-1472`；`CfirGenericInstantiationChecker.kt:1538-1566`。
- **问题**
  - `includedRootExtends` 和 `memberOwnerExtend` 只有泛型实例化检查器在用，却改变了共享 scope 的继承图语义。
  - 检查器还直接实例化了 scope 的 impl 类。
  - Kotlin 的 checker 通过 `scopeForClass`/ScopeSession 取 scope，scope 本身没有这类模式开关。
  - 这既违反接口优先，也是 K2 结构偏离。
- **框架级替代**：D1/D5 改为"声明级实现关系 + 替换 scope"之后，删除这两个开关；检查器改为经 scope provider 取 scope。
- **状态**：主体已提交，传播部分未提交；确定。来源：泛型实例化组 L2。

#### E5　common let 的不可变赋值被拆成一个独立检查器（中）

- **位置**：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonCtorImmutableAssignChecker.kt:27-63`。应当承担这条规则的是 `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentLegalityChecker.kt:455-474`。
- **问题**
  - 这个独立的 class-like 检查器遍历构造器体，只匹配 `CfirAssignment`，也不区分 `v = ...` 和 `this.v = ...`。
  - 官方把这条规则放在不可变赋值检查内部，作为其中一个分支：`NotAssignableVariable` 包含 `IsImutFieldInCtorOfCommonClassStruct`，再由 `CheckLetFlag` 按 REF_EXPR 或 MEMBER_ACCESS 选择诊断（`InitializationChecker.cpp:163-175,679-691,714-727`）。
  - 结果与官方分叉：

    | 写法 | 官方 | 本仓库 |
    |---|---|---|
    | 构造器里给 `common let v: Int64 = 1` 再赋值 | 只报 CJMP 诊断 | 通用诊断和 CJMP 诊断各报一次 |
    | `this.v = 1` | 报通用 `sema_cannot_assign_to_immutable` | 报 CJMP 诊断 |
    | `v += 1`、`v++` | 按 CJMP 规则处理 | 只有通用诊断 |

  - fixture 只测了"无初始值时写 `v = 1`"这一种形状。
- **框架级替代**：把 common let 规则并入 `CfirAssignmentLegalityChecker` 和自增自减检查，按官方的两条路径选择诊断。
- **状态**：已提交；确定。来源：检查器组 #6。

#### E6　PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION 缺少 SPECIFIC 豁免（中）

- **位置**：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCjmpParseRulesChecker.kt:165-188`。
- **问题**
  - 这里的 `hasDefaultImplementation` 只看有没有函数体、访问器或初始值。
  - 官方 `HasDefault` 第一句就是 `if (decl.TestAttr(SPECIFIC)) return true`（`ParseCJMPDecl.cpp:24-29,304-316`）。因此 `specific interface I { specific func m(): Unit }` 在官方是合法的，本仓库会误报。
  - 仓库里其实已经有忠实的移植 `cjmpHasCommonDefault()`（`CfirCjmpMappingStorage.kt:322-342`），这里却另写了一份缺豁免的副本。
- **框架级替代**：复用同一个 HasDefault 判据。
- **状态**：已提交；确定。来源：检查器组 #7。

#### E7　COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED 的禁止集不是官方的，另有跨检查器抑制（中）

- **位置**：`CfirCommonSpecificChecker.kt:673-684`；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt:106-107`。
- **问题**
  - 禁止集不对：
    - 本仓库：{C, JAVA, ENSURE_PREPARED_TO_MOCK}，加上平台注解 {JAVA_MIRROR, JAVA_IMPL}。
    - 官方 v1.1.3：{JAVA, CALLING_CONV, CONSTSAFE, ENSURE_PREPARED_TO_MOCK, UNKNOWN}（`CheckCJMPAnnotations.cpp:52-53,301-320`）。其他版本的集合在 P0 的跨版本差异清单中核对。
    - 后果：`@C`、`@JavaMirror`、`@JavaImpl` 被误报；`@CallingConv`、`@ConstSafe` 被漏报。
  - 自相矛盾：对 nominal 又把 `isC` 当作修饰符去比较（`:976`）。
  - 跨检查器抑制：通用注解检查器遇到 common/specific 声明时，会直接跳过 ILLEGAL_USE_OF_ANNOTATION，而且这个跳过不受版本门和模式门约束。在 mode=None 或 1.0.x 下，CJMP 检查器不报，通用检查器也不报。
- **框架级替代**：与 E2 共用按版本定义的分类表（NON_SERIALIZED / UNSUPPORTED / special-handled），由语言版本门禁选择；删掉通用检查器里的抑制。
- **状态**：已提交。禁止集差异确定，门禁交互可能。来源：检查器组 #8。

#### E8　泛型约束比较没有用上存储里的外层类型参数映射（中）

- **位置**：调用链为 `CfirCommonSpecificChecker.kt:922-925` → `CfirOverrideChecker.kt:407-460` → `cfir/providers/src/org/cangnova/cangjie/cfir/scopes/CfirOverrideSignatureUtils.kt:111-130`。
- **问题**
  - override 替换器只映射声明自身的类型参数。
  - 同一个方法里，变量类型和返回类型的检查都用了 `storage.typeParameterMappingFor(specific)`（:874-897），唯独约束检查没用。
  - 官方在匹配之前，就把 common 成员的类型整体替换到 specific 的类型参数空间（`UpdateSpecificMemberGenericTy`：`PreCheck.cpp:1820` 早于 `TypeChecker.cpp:2044`；`CheckCJMP.cpp:1314-1362`）。
  - 反例：`common class Box<T> { common func f<V>(x: V) where V <: Comparable<T> }` 对 `specific class Box<U> { specific func f<W>(x: W) where W <: Comparable<U> {...} }`，本仓库会误报 GENERIC_CONSTRAINT_NOT_LOOSER。
- **框架级替代**：把存储里的映射作为基础替换器，传给共享的约束检查。
- **状态**：已提交；可能。来源：检查器组 #9。

#### E9　`CfirCommonSpecificExtendChecker` 按名字判重（中）

- **位置**：`CfirCommonSpecificChecker.kt:428-440,453-483`。
- **问题**
  - extend 目标用 `coneType.toString()` 作键；private 成员用 `groupBy { it.name }` 分组；common-private 诊断和重复诊断会各报一次。
  - 官方以 `extendedType->ty` 加 `rawMangleName` 为键，其中 `rawMangleName` 包含参数类型（`CheckCJMP.cpp:1160-1191`，`ASTMangler.cpp:277-337`）；两种诊断之间是 else-if，同一成员只报一条。
  - 后果：两个无接口的 common extend 里分别写 `private func f(a: Int64)` 和 `private func f(a: String)`，会被误报为重复。
- **框架级替代**：复用 `CfirRedeclarationPresenter` 的签名表示；extend 目标按类型同一性比较。
- **状态**：已提交；代码级确定。来源：检查器组 #10。

#### E10　遗留的启发式判断与死代码（中）

- **位置**：`CfirCommonSpecificChecker.kt:216-228,230-239,253-263,286-307`。
- **问题**
  - **EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY**：只检查 common 类、只检查函数。官方覆盖 common 和 specific 类里的函数与属性（`DeclAttributeChecker.cpp:313-315`）。
  - **OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON**：本仓库在 class 级别也报。官方只对函数和属性报（`CheckCJMP.cpp:752-757`）。结果是 `specific open abstract class` 对 `common open class` 时报两条。
  - **COMMON_OPEN_CLASS_NO_INIT**：
    - 计划 §9.8 称它没有触发点，但报告器仍然存在；
    - 对 class 它是死代码，因为 PSI builder 总会补一个隐式主构造；
    - 对 `common open interface I {}` 反而会误报；
    - 官方只针对 CLASS_DECL（`CheckCJMP.cpp:470-498`）。
  - **重复 extend 循环**：在 class-like 声明里永远不会执行，而且按短类名作键。真正的报告点在 `:713-719`。
- **框架级替代**：删掉这些遗留判断，按官方条件并入 E1 所说的属性检查 owner。
- **状态**：已提交（2026-04 遗留）；确定。来源：检查器组 #11。

#### E11　property 的 mut 不一致被报成修饰符诊断（中）

- **位置**：`CfirCommonSpecificChecker.kt:977-982`。
- **问题**
  - 官方对 PROP 的 MUT 差异单独报诊断：`sema_property_have_same_declaration_in_inherit_mut`（锚在 specific）或 `_immut`（锚在 common）（`CheckCJMP.cpp:769-776`）。仓库里已经有对应的诊断工厂，这里却没有用。
  - 属性的比较顺序也与官方不同：官方依次比较 STATIC、MUT、…、OPEN、ABSTRACT，取第一个差异来报。
- **框架级替代**：按官方的顺序和诊断实现，并入 E12 的验证期比较。
- **状态**：已提交；确定。来源：检查器组 #12。

#### E12　K2 结构偏离：AbstractCjmpChecker 与 MppCheckerKind 都没有落地（中）

- **位置**
  - `resolution.common/src/org/cangnova/cangjie/resolve/calls/mpp/`：只有 matcher 和 context。
  - `CfirCommonSpecificChecker.kt:830-937`、`868-890`（未提交的返回类型协变复核）、`1033-1037`。
- **问题**
  - 计划 §4 Phase 2.2 承诺的 `AbstractCjmpChecker`（在验证期枚举不兼容项）没有落地。修饰符、超类型、变量类型、返回类型、注解的验证全部内联在检查器里。
  - 返回类型协变有两份实现：`AbstractCjmpMatcher.kt:78-92`，以及检查器里未提交的 `:868-890`。
  - §2 承诺的 `MppCheckerKind` 对位也没有做：
    - 模式判断散落在各处（`:117,426,507,710,742,1022`）；
    - common 方向的诊断由一个 file checker 自己遍历依赖模块的文件；
    - Kotlin 则由 collector 把 Platform 类检查器跑在所有模块的源码上（`MppCheckerKind.kt:8-19`）。
- **依据**
  - Kotlin 的 `FirExpectActualDeclarationChecker` 委托 `AbstractExpectActualChecker.getCallables/ClassifiersCompatibility` 计算兼容性，再把结果映射成诊断。
  - §11.5 声称"分层一致"，但 AbstractCjmpChecker 被撤销这件事没有登记。
- **框架级替代**
  - 在 `resolution.common` 落地 `AbstractCjmpChecker`，与 matcher 共享同一个 context；CFIR 检查器只负责把不兼容项映射成诊断。
  - 引入 MppCheckerKind 的等价物，由 collector 驱动 common 方向的检查。
- **状态**：混合（部分已提交、部分未提交）；确定。来源：检查器组 #14。

#### E13　隐式 abstract 的例外只覆盖函数，同一规则在三处各写一份（中）

- **位置**
  - `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt:4018-4031`
  - `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt:3435-3440`（属性分支在 :3472，传的是 `isFunction = false`）
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirStatusResolveProcessor.kt:1192-1197`
- **问题**
  - 当前判定是 `return isFunction && !modifiers.isAbstract && outer.isAbstract && (outer.isCommon || outer.isSpecific)`，只对函数生效。
  - 对 CJMP 抽象类里没写修饰符、也没有函数体的 `prop`，官方会取消它的 abstract 并报诊断（`Parser.cpp:462-466`、`ParseDecl.cpp:2164-2185`、`ParserImpl.cpp:270-277`），本仓库却仍按隐式 abstract 处理。
  - 这条规则在 PSI、LightTree、STATUS 三处各写了一份。
- **依据**
  - Kotlin 只在 `FirStatusResolver.kt:374-410` 一处统一计算隐式 modality。
  - 计划 Phase 4.4 的"隐式 abstract"一行只登记了函数。
- **框架级替代**：把 `CanBeAbstract` 和 `CheckClassLike{Func,Prop}Abstractness` 收敛成一个共享判定，放在 raw-cfir-common 或 STATUS 阶段的 modality 计算里，并补上属性分支。
- **状态**：已提交；确定。来源：resolve 组 #6。

#### E14　检查器的零散问题（低）

- **主构造器修饰符特判**
  - `CfirModifierChecker.kt:88-93` 单独特判主构造器上的 cjmp token，因为修饰符目标模型里没有 PRIMARY_CONSTRUCTOR 这一类。官方的主构造和次构造用两张不同的表（`ParserModifierRules.cpp:327-344,416-421`）。
  - `ModifierCheckerTargets.kt:236-238` 写着"留待 Phase 3"，但一直没有收口。
- **`CfirCjmpParseRulesChecker` 与官方有出入**（两处都只在模式错误等出错状态下才可达）
  - `:194-217` 的注释称"与官方逐字对位"，却比官方多了一个 `!containerIsSpecific` 条件（官方见 `ParseCJMPDecl.cpp:282-285`）。
  - `:327-329` 缺少官方的 JAVA_MIRROR_SUBTYPE 豁免（`ParseDecl.cpp:2152-2156`）。
- **file part 判定写了两份**：`CfirCjmpFilePartChecker.kt:72-86` 和 `CfirCommonSpecificChecker.kt:522-531`。官方只有 `File::isCommon/isSpecific` 这一处。
- **诊断参数与官方不符**
  - COMMON_GENERIC_FROZEN_NOT_SUPPORTED 传的是声明种类（`:390`），官方传声明名（`ParseCJMPDecl.cpp:208-209`）。
  - SPECIFIC_HAS_DUPLICATE_EXTENSIONS 传的是短类名，官方传完整的类型字符串（`CheckCJMP.cpp:438-439`）。
  - `reportUnmatched` 找不到候选时，用 specific 自己的种类同时填两边（`:816`）。
- **门禁不是单一入口**
  - `CjmpGate` 自称是单一入口（D16），但 FilePart、ConflictsHelpers、ModifierChecker、ModifierCheckerTargets、ModifiersCompatibilityUtils、ExtendExtraChecker 都直接调用 `supportsFeature`。
  - `ModifierCheckerTargets.kt:483-486` 的 `isWithinCommonSpecificAbstractClass` 完全不受版本门约束。
- **分层与接口**：`CfirCjmpCommonSideFacts` 是 cfir-tree 里一个公开的具体 object，还混入了诊断文案的格式化。它主要是为了给 H4 的 CLI 旁路共用而存在，违反接口优先和分层原则。
- **状态**：已提交；确定。来源：检查器组 #15。

#### F3　enum 构造器配对绕开共享 matcher（中）

- **位置**：`CfirCjmpMatchRunner.kt:74-78,256-286`。
- **问题**
  - 这里自己用 `firstOrNull COMPATIBLE` 选候选，并且忽略 `bind` 的返回值。
  - common enum 为非穷尽时什么都不记录，违反了"每个声明都有配对结果"的后置条件，只能靠 LL 在 `:59` 单独跳过 enum 构造器。
  - 官方做法：
    - 在同一个循环里走 `MatchCJMPEnumConstructor` → TrySetSpecificImpl（`CheckCJMP.cpp:1127,1031-1048`）；
    - 非穷尽豁免作用于外层带 COMMON_NON_EXHAUSTIVE 的所有 specific 声明（`1147-1150`），不只是构造器。CFIR 对这类 enum 里未配对的 `specific func` 仍会报 NOT_MATCHED。
  - Kotlin 在共享 matcher 内部处理 enum 条目（`AbstractExpectActualMatcher.kt:138-140`）。
- **框架级替代**
  - 给 `CjmpMatchingContext` 加上处理 enum 构造器的能力，让构造器走共享的 first-fit。
  - 非穷尽豁免按外层声明判定。
- **状态**：已提交。结构问题确定；非构造器成员的豁免差异属于可能，需要探针确认。来源：配对引擎组 #7。

#### F4　模块同一性按名字判定，DAG 只做了传递过滤（低）

- **位置**：`CfirCjmpResolver.kt:44-51,117,154,178-180,234`；`CfirCjmpMatchRunner.kt:299`。
- **问题**
  - **名字判等的理由不成立**：KDoc 说 `LLCfirModuleData` 每次都会重建，所以只能比名字。但它的 `equals` 按 caModule 比较（`LLCfirModuleData.kt:122-127`），完全可以直接用 `in` 判定；而名字并不保证唯一。
  - **缺少 first-wave 过滤**：Kotlin 有这一步（`FirExpectActualResolver.kt:148-171`），这里没有。
  - **callable 与 class-like 的 DAG 语义不一致**：callable 取全部传递依赖里的候选，class-like 只取第一个命中。这与计划 D3"按 DAG 实现"不符。
  - 计划登记了这一做法，但登记的理由不成立。
- **框架级替代**
  - 用 moduleData 的相等性判定模块同一性。
  - DAG 语义随 G6 按语言版本门禁处理：只支持单个 common part 的版本，只需要直接 depends-on；支持多 common parent 的版本，移植官方对应的语义（包括 first-wave 过滤）。
- **状态**：已提交；确定。来源：配对引擎组 #9。

#### F5　nominal 泛型参数个数的规则比官方判据宽（低）

- **位置**：`AbstractCjmpMatcher.kt:29-31`。
- **问题**
  - 本仓库只要泛型参数个数不同，就不合并。
  - 官方只看"是否为泛型"（`hasGenericMismatch`，`CheckCJMP.cpp:320-323`）。
  - 现有探针只覆盖了 `Box<T>` 对 `Box` 这一种情况。
- **框架级替代**：按官方判据实现；或者为 N≠M≥1 的情况补探针，并登记差异。
- **状态**：已提交；可能。来源：配对引擎组 #10。

#### F6　外围容器有四个来源，含 ThreadLocal 和一条绕开不变量的死路径（低）

- **位置**：`CfirCjmpMatchingProcessor.kt:108-109,258-274`；`CfirCjmpMatchRunner.kt:326-330`；`CfirCjmpResolver.kt:70-78,208-221`。
- **问题**
  - ThreadLocal 的注释说是为了多文件并行，但 eager 实际是逐文件串行执行的（`CfirTotalResolveProcessor.kt:66`）。
  - `findCommonCandidates` 的成员分支按 ClassId 直接查 common 容器，绕过了"只从已配对容器取候选"这一不变量。
  - Kotlin 的 transformer 没有状态，外围容器由符号加 `expectForActual` 推导出来（`FirExpectActualResolver.kt:48-58`）。
  - §11.1 只把这一做法作为事实登记。
- **框架级替代**：只保留一个从符号推导外围容器的函数，删除 ThreadLocal 和按 ClassId 查找的成员分支。
- **状态**：已提交；确定。来源：配对引擎组 #11。

#### F7　种类判别用字符串并带兜底；MISSING_BODY 与第二次绑定的判定顺序和官方相反（低）

- **位置**：`CfirCjmpMatcher.kt:147-162`；`AbstractCjmpMatcher.kt:123-129`。
- **问题**
  - 种类判别的 `else -> "class"` 分支会把 `CfirTypeAlias` 当成 class；callable 的种类用的是 `::class.simpleName`。
  - 官方 TrySetSpecificImpl 先判断"common 已被实现"，再判断 MissingBody（`CheckCJMP.cpp:900-909`），CFIR 的顺序正好相反。
  - 以上两点在合法输入下目前触发不到。
- **框架级替代**：种类改用枚举，并用穷举的 `when`；判定顺序对齐官方。
- **状态**：已提交；确定。来源：配对引擎组 #12。

#### F8　配对存储没有区分读接口和写接口（低）

- **位置**：`CfirCjmpMappingStorage.kt:60-301`。
- **问题**
  - 存储是一个 `open class`，读写 API 全部暴露给 checker、调用解析、LL 和 frontend。
  - D7/D8 的辅助函数也混在同一个文件里。
  - 这违反接口优先原则。
- **框架级替代**：拆成只读的结果接口，以及只给 matcher 用的写接口；把辅助函数移出去。与 A2 一起做。
- **状态**：已提交；确定。来源：配对引擎组 #13。

#### G2　门关闭时不推进阶段，BODY_RESOLVE 的前置阶段随 session 变化（中）

- **位置**
  - `LLCfirCjmpMatchingLazyResolver.kt:65-66,75,102-104`
  - `LLCfirLazyResolver.kt:97-110`（新增的 `shouldCheckIsResolved`）
  - `CfirCjmpSpecificCompilation.kt:26-31`（`bodyResolvePrerequisitePhase`）
  - 以及 `LLCfirTargetResolver.kt:394-411`、`FileElementFactory.kt:27-52`、`FileStructure.kt:231`、`FileStructureElement.kt:319`、`inBlockModification.kt:145,159,171`
- **问题**
  - `if (!isCjmpMatchingEnabled()) return true`：门关闭时既不调用 `performCustomResolveUnderLock`，也不推进阶段。
  - Kotlin 在 MPP 关闭时只跳过 transform，`withWriteLock(updatePhase=true)` 照样推进阶段。
  - 本仓库这样做，是为了配合测试在会话建好之后才注入模式：`f964f41bb` 时 `enabled` 在构造时就固定，`f781a7a88` 改成了动态读取。
  - 由此带来四个后果：
    1. 阶段阶梯随 session 不同而变化，甚至会随时间变化，需要在 6 个以上的文件里用 `bodyResolvePrerequisitePhase` 特判。
    2. `doResolveWithoutLock` 返回 true，却没有执行加锁解析，违反了它自己的 KDoc（`LLCfirTargetResolver.kt:261-271`）。
    3. 门关闭时，每次 `lazyResolveToPhase(CJMP_MATCHING)` 都要重新走一遍 designation 和加锁。
    4. 如果模式在解析中途翻转，已经到达 BODY_RESOLVE 的声明将永远不会再配对。
  - C26 只要求"门关闭时不改变行为"，并没有要求"不推进阶段"。
- **依据**
  - Kotlin `LLFirExpectActualMatcherLazyResolver.kt:46,65-69`、`LLFirLockProvider.kt:50-58`。
  - §10 Phase 2 和 §11 登记了"不推相位/动态读"，但 resolver 的 KDoc 声称"对齐 Kotlin"，与事实不符。
- **框架级替代**：在 G1 修好、模式于建 session 时固定之后，改为始终推进阶段、只门控 transform，并删除 `bodyResolvePrerequisitePhase` 和 `shouldCheckIsResolved`。
- **状态**：已提交；确定。来源：LL 组 #3。

#### G3　"按文件真实归属找模块"只修了测试 provider（中）

- **位置**
  - 测试侧（未提交）：`analysis/analysis-test-framework/testFixtures/org/cangnova/cangjie/analysis/test/services/CaTestPlatformServices.kt:159-190`
  - 生产侧：`analysis/analysis-api-standalone/src/.../CaStandaloneProjectStructure.kt:155-160`、`lsp/src/.../AnalysisApiLspProjectStructure.kt:316-323`
- **问题**
  - 生产 provider 仍让 use-site 模块覆盖文件的真实归属：
    - standalone 在 `computeSpecialModule` 之后执行 `useSiteModule?.let { return it }`；
    - LSP 在 317 行，甚至在特殊模块判断之前就返回。
  - 这个问题的 owner 是平台 `getModule` 的契约：只能返回 content scope 包含该元素的模块。
  - 只修测试以后，LL 测试依赖的是生产中并不存在的行为：
    - `LLCfirFileBuilder.kt:29-41` 的"模块一致"校验在生产中恒为真，形同虚设；
    - `LLSelectingCombinedSymbolProvider.kt:60-68` 的模块优先级会失效；
    - 在 CJMP 场景下，从 specific 侧查询 common 的 PSI，会被归到 specific 模块。
- **依据**
  - Kotlin `KotlinTestProjectStructureProvider.kt:31-57` 和 `KotlinStandaloneProjectStructureProvider.kt:50-80` 都不让 use-site 覆盖文件归属。
  - Kotlin `KotlinProjectStructureProvider.kt:20-22` 定义了这一契约。
  - 生产侧的问题没有登记。
- **框架级替代**：standalone 和 LSP 按 Kotlin standalone 的写法，按 content scope 找所属模块；use-site 只用于在多个候选模块之间消歧。
- **状态**：测试侧未提交，生产侧为已提交的旧代码。代码分叉确定，影响面待验证。来源：LL 组 #4。

#### G4　LL extend provider 合并漏掉了常规源码依赖（中）

- **位置**：`LLCfirAbstractSessionFactory.kt:743-766`；`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/CfirExtendProviderComposer.kt:62-73`。对照 eager：`cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt:330-338`。
- **问题**
  - 两边合并的依赖集合不同：
    - LL：`lazyCombine(ownProvider) { moduleData.allRefinementDependencies... + deserializedProvider }`
    - eager：`(moduleData.dependencies + moduleData.allRefinementDependencies)...`
  - `deserializedProvider` 只取反序列化来的 provider（`CfirDeserializedExtendProvider.kt:120-126`）。
  - 本批只为 CJMP 补上了 refinement 这一半。常规源码依赖模块里的 extend，在 LL 下仍然不可见，在 eager 下可见。KDoc 里"与主编译器完全一致"的说法不成立。
- **依据**：惰性合并本身在 §11 已登记，这个缺口没有登记。
- **框架级替代**：LL 与 eager 共用同一个依赖集合公式：`dependencies + allRefinementDependencies`。
- **状态**：已提交。组合差异确定；实际影响需要一个跨源码模块 extend 的 LL 用例验证。来源：LL 组 #5。

#### G5　LL 的 TYPES 阶段容器配置是手抄的副本（中）

- **位置**
  - `analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirTypeLazyResolver.kt:161-190`（`buildConfiguration`）
  - 对照 eager：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt:258-293,365-386`
  - 相关：`CfirTypeResolveTransformer.kt:436-452` 的 `constructorOwnerForTypeResolution`
- **问题**
  - 两边构造配置的方式不同：
    - LL 把成员本身当作 `topContainer`，再逐个补上外围类型参数；
    - eager 把外围容器当作 topContainer，并额外带上 `withThisTypeContext(thisTypeContextForExtend)` 和 `withEnclosingClassBodyScopes`。其中后者是 `bfbd76e5f` 在 2026-09-22 加的，只加到了 eager。
  - 这份副本仍有缺口：
    - `thisTypeContextForFunctionReturn` 读的是 `data.topContainer`，而在 LL 里它是函数本身。所以在 LL 下，类或 extend 成员的返回类型写 `This`，很可能会报"不允许"。
    - 类体里同名成员遮蔽类型名的规则，在 LL 下不生效。
    - eager 有 25 个 `func …: This` 夹具，但没有任何 LL 诊断套件覆盖。
  - `constructorOwnerForTypeResolution` 没有去补齐 LL 配置，而是按 ClassId 反查构造器的 owner。根因同样在 `LLCfirTypeLazyResolver.kt:161-177`：topContainer 被设成目标本身，且只收集 `CfirClass`。遇到重声明等情况时，会拿到错误的类型参数来源。
  - §11.1 只登记了 extend 类型参数的补丁，残余的分叉没有登记。
- **框架级替代**：由 transformer 提供一个成员作用域入口，产出与 eager 相同的配置；LL 调用这个入口，删除副本和按 ClassId 的反查。
- **状态**：已提交。副本结构的问题确定；`This` 和遮蔽规则的分叉待 LL 用例验证。来源：LL 组 #6、resolve 组 #10。

#### G6　多 common 模块的 first-fit 固化了 v1.1.3 不支持的语义（中）

- **位置**
  - `analysis/low-level-api-cfir/testData/cjmpMatching/firstFitCandidateDiagnostic.cj`（未跟踪的新文件）
  - `AbstractCfirCjmpMatchingTest.kt:103-152`（未提交）
  - `cfir/analysis-tests/testData/diagnostics2/common-specific/e2e/cjmpFirstFitCandidateDiagnostic.cj`（已提交）
- **问题**
  - 夹具写的是 `// MODULE: …()()(common1 common2)`，断言"common 候选按 dependsOn 的声明顺序排列，只绑定第一个兼容的候选"。
  - v1.1.3 只支持一个 common part：
    - `Option.h:473` 中是单个 `std::optional<std::string> commonPartCjo`；
    - `OptionAction.cpp:656-659` 多次传入时，后一个覆盖前一个；
    - `CjoManager.cpp:463-470` 只加载这一个。
  - origin/main 的多 parent 语义是 `CheckCJMP.cpp:1193` 的 `if (matched && !severalParents) break;`：有多个 parent 时，不会在第一次匹配后停下，每个 parent 各自绑定。
  - 因此当两个 parent 都兼容、且 common 没有默认实现时，本仓库只绑定第一个；第二个会在 common 方向被误报 NOT_MATCHED。
  - 官方的 first-fit 探针（`.workbuddy/tmp/cjmp_probe113/firstfit_common.cj`）测的是单个模块里两个同 key 的 common extend，不是多个 common 模块。
  - 计划 D3/C8 以 origin/main 为依据，但实现的并不是 origin/main 的语义（origin/main 是每个 parent 各自绑定），也没有语言版本门禁。
- **框架级替代**
  - 夹具改成官方探针的形状。
  - 多 parent 的语义按语言版本门禁处理（§12 决策 1）：
    - 在只支持单个 common part 的版本（如 1.1.x）下，多 parent 不可表达，按官方行为处理这类配置；
    - 从官方引入多 parent 的版本起，实现"每个 parent 各自绑定"。
  - 引入版本由 P0 的跨版本差异清单确定。
- **状态**：LL 夹具和测试未提交或未跟踪，analysis-tests 夹具已提交。确定。来源：LL 组 #7。

#### G7　`isReanalyzableContainer` 照搬 Kotlin，推断返回类型下会保留过期的签名和配对（中，既有问题被本批放大）

- **位置**：`LLCfirDeclarationModificationService.kt:631`，规则为 `CjNamedFunction.isReanalyzableContainer() = hasBlockBody() || typeReference != null`。
- **问题**
  - 在 Kotlin 里，块体函数不写返回类型就是 Unit，所以改函数体不会改签名。
  - 仓颉的函数体都是块，不写返回类型时由函数体推断。改函数体会被当作块内修改处理，于是保留了过期的隐式返回类型。
  - 本批让配对依赖推断出来的返回类型（§11.4/§11.5）；同时在 specific 模式下，`phaseWithoutBody` 保留到 CJMP_MATCHING，过期的配对结果也不会重算。
  - 属于类别 8。
- **框架级替代**：按仓颉的语义定规则：省略返回类型的函数不作为可块内重分析的容器；或者改动函数体后，让签名和配对结果失效。
- **状态**：已提交（既有代码）；确定。来源：LL 组（被本批放大的既有问题）。

#### G8　覆写测试调换了求值顺序（低）

- **位置**：`analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/cases/components/symbolRelationProvider/AbstractOverriddenDeclarationProviderTest.kt:71-72`（未提交）。
- **问题**
  - 改成先算 `directlyOverriddenSymbols`、再算 `allOverriddenSymbols`，没有说明原因。Kotlin 的同名测试（`AbstractOverriddenDeclarationProviderTest.kt:39-40`）是先算 all。
  - 如果顺序会影响结果，说明 `processOverriddenFunctions` 对 base scope 的预热存在顺序依赖，而这次调换把它掩盖了。可能属于类别 7。
- **框架级替代**：恢复 Kotlin 的顺序，重跑 `functionChain`、`propertyChain`、`substitutionFunction` 三个夹具；如果失败，修掉预热的顺序依赖。
- **状态**：未提交；可能。来源：LL 组 #12。

#### G9　LL 按包枚举文件时借用了 facade 的契约（低）

- **位置**：`analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/providers/LLCfirProvider.kt:146-155`。
- **问题**：实现依赖 `findFilesForFacadeByPackage`。这个接口的契约是"参与 facade 的文件"，只是现有 provider 恰好返回了包内的全部文件，契约并不明确。
- **框架级替代**：在 declaration provider 上增加一个语义明确的"按包列出全部文件"接口。
- **状态**：已提交；确定。来源：LL 组 #11。

#### H4　`CjmpDeserializedCommonSideReporter` 是 CLI 层的字符串旁路（中）

- **位置**
  - `compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/CjmpDeserializedCommonSideReporter.kt:34-79`，调用点在 `CfirFrontendPipelinePhase.kt:143-147`。
  - 与之互补的跳过逻辑：`CfirCommonSpecificChecker.kt:1031-1036,1045`（`source ?: continue`）。
- **问题**
  - 这个 reporter 在管线层重新做了一遍 common 方向的判定，把手写的英文字符串经 `MessageCollector.report(ERROR, ...)` 直接输出，没有经过 `CfirErrors`。
  - 第 35-36 行自带一套版本门和模式门，没有走 `CjmpGate`，违反 D16。
  - 同一个诊断因此有两个 owner：源码中的 common 由 checker 报告，二进制中的 common 由这条字符串旁路报告。后果：
    - IDE、LL、Analysis API 永远看不到二进制 common 的 common 方向诊断；
    - 这些诊断不能抑制，也没有诊断工厂名；
    - 文案与 `CfirErrorsDefaultMessages.kt:1162-1165` 重复。
- **依据**
  - Kotlin `compiler/ir/ir.actualization/.../IrActualizerUtils.kt:85-87` 经 `IrDiagnosticReporter` 加 `IrActualizationErrors.NO_ACTUAL_FOR_EXPECT` 报告。
  - 计划 §10 Phase 4.4 的 G20 一行已登记这条旁路。
- **框架级替代**
  - 用 CJO 位置为反序列化声明构造一个二进制 source element。related-information 已经支持文件、行、列。
  - 由 `CfirCjmpCommonSideChecker` 统一经 `CfirErrors` 报告。
  - 删除这条旁路，以及 `CfirCjmpCommonSideFacts` 里的文案格式化。
- **状态**：已提交；确定。来源：序列化组 M1、检查器组 #13。

#### H5　features/options 门按内容启发式决定作用对象（中）

- **位置**
  - `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/CfirDeserializedSymbolProvider.kt:89-106`
  - `CjoPackageHeader.kt:200,301-315`：`isCjmpCommonPart` 的判据是"存在任一 CJMP 位、options 或 features"
  - `CjoManager.kt:58-68`：版本门和包名门作用于所有 CJO，而包名门用的是 common part 的文案
- **问题**
  - specific 编译中，任何被 import 进来、带 CJMP 位的其他库都会被当作 common part 过门，例如别的 CJMP 构建产物，或本仓库自己的 specific 输出（它带 SPECIFIC 位）。
  - 结果是出现虚假的 "missing serialized options" 警告或 features 错误。
  - 而 provider 其实已经通过 `moduleDataFor(loaded.sourcePath)` 知道哪个文件属于 depends-on 模块，不需要启发式。
  - KDoc 声称这些门只针对 common part，实现却用了启发式；计划没有登记。
- **依据**：官方只对 `--common-part-cjo` 的加载器施加这些门（`CjoManager::GetCommonPartCjo`、`PreloadCommonPartOfPackage`）。
- **框架级替代**：以"是否为配置指定的 common part 路径、是否属于 depends-on 模块"作为判据，即使用 H2 的专用加载入口。
- **状态**：已提交。代码层面确定，实际触发频率属于可能。来源：序列化组 M2。

#### H6　加载门、CLI 形态和模式判据只实现了 origin/main 的行为，没有语言版本门禁（中）

- **位置**
  - `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjmpCommonPartLoadGate.kt:51-152`
  - `CjoManager.kt:58-68`
  - `cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/CfirCjmpSettings.kt:69-96`
  - `AbstractFrontendPipeline.kt:49-63`
  - `compiler/arguments/src/org/cangnova/cangjie/arguments/description/compilerArguments.kt:75-104`
  - `ModuleFormat.fbs` 中新增的字段
- **本仓库的实现**
  - 版本门对所有 CJO 执行"缺版本即拒载"。
  - 有 features 子集门和 options 门，缺 options 时报 WARNING。
  - 支持传入多个 common part。
  - cjo 与 chir 的数量不一致时报配对错误。
  - Specific 模式的判据是 `commonPartChirPaths` 非空。
- **v1.1.3 的实际行为**
  - `Option.h:473` 只有单个 `commonPartCjo`。
  - `Option.h:1145-1153` 中，Specific 的判据是位置参数 `inputChirFiles`。
  - `ParseCJMPDecl.cpp:103-106` 中，解析层的 `compileSpecific` 取决于是否给了 common part cjo。
  - `ASTLoader.cpp:298-331` 只做 Verifier，没有版本门、features 门、options 门。
  - v1.1.3 的 `DiagnosticModule.def` 里没有 `module_common_cjo_no_options`、`_debug_mismatch`、`_opt_mismatch` 和 features 子集相关的诊断；`module_version_not_identical` 是 WARNING，而且在 src 中没有被引用。
  - v1.1.3 的 schema 里没有 `FileInfo.feature`/`Package.options`。
- **可以断定的错误结果**
  - cjc 1.1.3 产出的 common part 必然触发 "missing serialized options" 警告。
  - 只给 cjo、不给 chir，会被判为配对错误。
  - 测试传入一个并不存在的 `.chir`，仅仅用作模式开关（`CjmpTwoPhaseCompilationTest.kt:356-357`）。
  - §10 Phase 4 验证段"官方加载同样拒绝无版本 cjo"的说法，对 v1.1.3 不成立。
- **类别**：属于类别 8 的变体。对 1.1.x 语言版本而言，引入了该版本官方没有的语义。
- **依据**
  - origin/main 的出处：`DiagnosticDriver.def:54`、`ASTLoader.cpp:352-374`、`ASTLoaderCJMP.cpp:66,158`。
  - 计划以 origin/main 为依据登记了这些实现，但这与计划自己的 §7 风险 3、Phase 4.4"以 v1.1.3 为准"相冲突，冲突本身没有登记。
- **框架级替代**：按语言版本门禁同时支持各版本的行为（§12 决策 1）。
  - 1.1.x：只接受单个 common part cjo，只校验包名，Specific 模式按输入的 chir 文件判定。
  - 从官方引入的版本起，启用版本门、features 门、options 门、多 common part，以及 cjo/chir 数量配对。
  - 每个门对应一个带引入版本的 `LanguageFeature`，引入版本由 P0 的跨版本差异清单确定。
- **状态**：已提交；确定。来源：序列化组 M3。

#### H7　门的比对输入：options 只有测试能设，LightTree 下 features 恒为空（中）

- **位置**：`cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/configuration/CfirFrontendConfigurationKeys.kt:283-291`；`CfirFrontendPipelinePhase.kt:72-75,188-194`。
- **问题**
  - **options**：`cjmpModuleDebug`、`cjmpModuleOptLevel` 在生产代码里没有任何赋值点，全仓只有 `CjmpTwoPhaseCompilationTest.kt:336-337,359-360` 在设置它们；CLI 参数表里也没有 `-g`/`-O`。生产中永远是默认值比默认值。
  - **features**：
    - specific 侧只从 `CjPsiSourceFile` 采集 features。LightTree 采集器产出的是 `CjVirtualFileSourceFile`（`GroupedCjSources.kt:147`），所以采集结果恒为空集。
    - 写侧用的 `CfirFile.featuresDirective` 在 PSI 和 LightTree 两条路径下都会构建。
    - 结果：LightTree 模式下，只要 common 带有 features，就必然报 FEATURE_NOT_SUBSET。
  - 测试里设置的 `cjmpPackageFeatures` 会被第 75 行覆盖，是一条死设置。
- **框架级替代**：debug/opt 从 CLI 选项映射过来；两侧的 features 都取自 `CfirFile.featuresDirective`。
- **状态**：已提交；确定（LightTree 不是 CLI 的默认模式）。来源：序列化组 M4。

#### H8　加载门诊断只在 CLI 中外显（中）

- **位置**：`cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/CfirCjmpLoadDiagnostics.kt:59-66`（可空访问器）；`CfirDeserializedSymbolProvider.kt:56-61,92`。唯一的读取方是 `CfirFrontendPipelinePhase.kt:165-180`。
- **问题**
  - LL/IDE 没有注册收集器：features 门和 options 门直接被跳过；版本门或包名门拒载后，静默表现为"包不存在"。
  - LLT 只收集诊断，不做断言。
  - CLI 在 resolve 期间按需拒载，但要等所有检查结束后才报。
  - 属于类别 2 + 6。
- **依据**
  - Kotlin 用 checker 报告不兼容的二进制（`FirIncompatibleClassExpressionChecker.kt:24`、`FirIncompatibleClassTypeChecker.kt:14`），CLI 和 IDE 都生效。
  - 计划登记了 G17（CLI 外显、LLT 断言），但 LLT 断言没有实现，IDE 侧的缺口也没有登记。
- **框架级替代**：把加载门的结果作为 session 级事实，由 checker 报告（对位 Kotlin 的 incompatible-class checker），CLI、IDE、LLT 走同一通道；补上 LLT 断言。
- **状态**：已提交；确定。来源：序列化组 M5。

#### H9　specific 编译写出的 CJO 不包含 common part 的声明（中）

- **位置**：`CfirFrontendPipelinePhase.kt:201-230`；写侧 `CfirCjoPackageMetadataProducer.kt:134-138` 只接受来源为 Source 的声明。
- **问题**
  - 输出里只有 specific 源码中的声明。只存在于 common 的声明（例如带默认实现、没有 specific 对应物的函数）会丢失，下游无法使用。
  - 官方 `ASTWriter.cpp:490-505` 在 body 中先写 `specificImplementation`，再写没有被 `doNotExport` 的 common 成员。
  - 探针产物 `full/cjmp_p.cjo` 的 allFiles 同时列出了 `cjmp_p\common.cj` 和 `cjmp_p\specific.cj`。从 allFiles 的形态和时间戳判断，它是官方 specific 编译的产物。
  - 计划 C25 只在 resolve 层实现了 doNotExport，输出侧没有登记。
  - 这个问题目前被 H3 挡住，CLI 路径走不到这里。
- **框架级替代**：specific 输出时，把 depends-on 模块中未被遮蔽的反序列化声明一并写出（依赖 C1 的合并视图）。
- **状态**：已提交；可能。验证方法：用 cjc 1.1.3 编译一个只在 common 中声明的函数，检查 specific 产物的 cjo 是否包含它。来源：序列化组 M6。

#### H10　identifierPos 另起了一套定位（低）

- **位置**：`CfirCjoPackageMetadataProducer.kt:189-226`（未提交）。
- **问题**
  - 写侧自己实现了 PSI 和 LightTree 两套 identifierPos 提取，没有复用 `SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME`。
  - 写侧在 LightTree 分支里补上了 `OPERATION_NAME`，但共享的 `LightTreePositioningStrategies.kt:528-529` 仍然只找 `IDENTIFIER`；PSI 的 `CjNamedDeclarationStub.kt:89` 则两者都认。
  - 也就是说，这个修正没有落到共享 owner 上，运算符函数的定位在 PSI 与 LightTree 之间仍不一致。
  - §11.5 登记了双路提取。
- **框架级替代**：写侧复用共享的定位策略，并在共享的 LightTree 策略里补上 `OPERATION_NAME`。
- **状态**：未提交；确定。来源：序列化组 L1。

#### H11　CJO 位置到诊断位置没有统一的 owner（低）

- **位置**
  - `CfirCommonSpecificChecker.kt:175-192`（未提交）：C45 的 note 取 identifier 位置。
  - `CfirGenericInstantiationChecker.kt:2210-2216`（已提交）、`CfirExtendCheckers.kt:240-241`，以及 H4 的 reporter：取声明起点。
- **问题**
  - 三个检查器各自手工构建 `CjDiagnosticRelatedSourceLocation`：一处用 identifier，另外几处用声明起点。
  - 官方 `InstantiatedChecker.cpp:435` 的 note 用的是 `MakeRangeForDeclIdentifier`，所以 CJO 候选的 note 列号不对。
  - 源码候选的 note 锚在函数名上，与 CJO 候选不一致。
- **框架级替代**：由反序列化器从 `CfirCjoDeclarationPosition` 产出统一的源码元素或位置解析；用 identifier 还是声明起点，只在这一处决定。与 H4 一起处理。
- **状态**：混合（部分已提交、部分未提交）；确定。来源：序列化组 L2、检查器组 #13。

#### H12　排序修补放错了层（低）

- **位置**：`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/deserialization/ModuleDataProvider.kt:106-115`（`8b3bdc5cf`）。
- **问题**
  - 把"精确路径过滤优先于 TakeAll"的排序逻辑放进了 `getModuleData`。Kotlin 是在 builder 里排序的（`DependencyListForCliModule.kt:128-147`）。
  - 本仓库的 builder 本来就把 TakeAll 追加在最后，所以注释描述的场景在当前实现下不会发生。
  - 计划已登记。
- **框架级替代**：只在 builder 中保证顺序，删除 `getModuleData` 里的补丁。
- **状态**：已提交；确定。来源：序列化组 L3。

#### H13　`recordCjoDeclarationPosition` 忽略了 `Position.pkgId`（低）

- **位置**：`CfirDeclDeserializer.kt:150-162`（已提交）。
- **问题**：当官方写出的位置指向其他包的文件时，这里会取到错误的文件路径。
- **框架级替代**：按 `pkgId` 查文件表来解析路径。
- **状态**：已提交；可能。来源：序列化组 L4。

#### H14　生产路径里残留了调试探针（低）

- **位置**：`compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt:721`，随 `f964f41bb` 进入。
- **问题**：每次宏调用都会执行 `System.err.println("PROBE-EXE: ...")`。
- **框架级替代**：删除。
- **状态**：已提交；确定（主审查者核实）。来源：序列化组 L5。

#### I1　推断返回类型时的错误过滤只看根类型（中）

- **位置**：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt:1294-1298`（过滤）、`:1312`（尾部错误传播）。
- **问题**
  - 过滤条件是 `expression in explicitReturnExpressions && expression !== bodyTailReturnExpression && type is ConeErrorType`，只检查根类型。
  - 官方的 `IsTyCorrect` 是递归判断：只要元素类型无效，整个类型就无效。所以 `return [missing]` 这类嵌套错误在这里不会被过滤，反而进入 join，可能多报一条函数级错误。
  - 仓库自己已有 `containsErrorType()`（`cfir/cfir-cones/.../ConeTypeUtils.kt:15-22`，KDoc 明确写了"不能只检查根类型节点"），matcher 也在用，这里却没用。
  - 新测试 `invalidExplicitReturnDoesNotPoisonAValidInferredBodyTail` 只覆盖了根类型出错的情况。
  - 另一处可能的问题：理想类型只在 join 时替换，表达式节点本身仍保留 IDEAL 类型；官方是在节点上替换的（`ReturnExpr.cpp:42`、`Block.cpp:20`）。这可能影响逐表达式读取类型的消费者。
- **依据**
  - 官方 `TypeChecker.cpp:2769-2774`、`Types.h:414`、`Types.cpp:851-854`。
  - §11.5 登记了这个过滤，但没有登记判定条件与官方的差异。
- **框架级替代**
  - 过滤和尾部传播都改用 `containsErrorType()`。
  - 评估是否改为在表达式节点上执行 ReplaceIdealTy。
- **状态**：未提交；确定，具体形态待复现。来源：resolve 组 #7。

#### I2　歧义包限定符靠回滚错误引用来触发重新解析（中）

- **位置**：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:670-684`（未提交），叠加在 `c7731df8c` 的 `:688-712`、`:732-746` 之上。
- **问题**
  - 当 `isUsedAsReceiver && importedPackageQualifier?.isAmbiguous == true && calleeReference is CfirErrorNamedReference` 成立时，这里把错误引用换成普通命名引用，再重新解析一次。
  - 包限定符的处理已经分散在四个条件块里；这次改动又不问原因地抹掉了先前写入的错误状态。
  - 它是为了修复 `c7731df8c` 引入的 `err_ambiguous_00/01` 回归而加的。
  - 属于类别 5 + 2。
- **依据**：Kotlin 由 `FirQualifiedNameResolver` 一次性产出 `FirResolvedQualifier` 或歧义结果。计划没有登记。
- **框架级替代**：在包限定符解析处一次性给出结果（唯一包，或包名歧义）；表达式 transformer 不再回滚已经写入的状态。
- **状态**：未提交；确定。来源：resolve 组 #8。

#### I3　`CfirTypeResolver` 里按 `CjBinarySourceElement` 分出的分支只有单测能走到（低）

- **位置**：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt:530-539`（`c7731df8c`）。
- **问题**
  - 分支条件是 `useSiteFile == null && typeRef.source is CjBinarySourceElement`。
  - 生产代码里没有任何地方会产生"source 为二进制的 user type ref"：CJO 和 stub 的反序列化都直接生成已解析的类型。
  - 只有测试 `CfirTypeResolverTypeAliasExpansionTest.kt:65-79` 自己构造了一个 `TestBinarySourceElement`。这个分支是为了让该测试在引入"源码中的限定名必须经 import binding"之后仍能通过而加的。
  - 属于类别 6；计划没有登记。
- **框架级替代**：删除这个分支；测试改为直接构造 resolved 或 ClassId 形态的类型。
- **状态**：已提交；可能（全库 grep 没有找到生产侧来源）。来源：resolve 组 #9。

#### I4　import binding 按 session 存储，LL 需要为外部 session 的文件补录（低）

- **位置**：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirImportsResolveProcessor.kt:62-70,107`；`analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/lazy/resolve/LLCfirModuleLazyDeclarationResolver.kt:159-200`。
- **问题**
  - 只要在非 owner 的 session 里解析某个文件，就必须记得补录它的 import binding，否则 `requireBindings` 会直接抛异常。
  - Kotlin 把 `FirResolvedImport` 挂在 FirFile 上，不存在这个问题。
  - 这是一处 K2 结构偏离，计划没有登记。
- **框架级替代**：按文件的 owner session 存取 binding，或者直接挂在 CfirFile 上。
- **状态**：已提交；确定。来源：resolve 组 #11。

#### I5　`coneTypeSafe` 为一个从未被构造的节点加了分支（低）

- **位置**：`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/CfirTypeUtils.kt:40-52`（未提交）。
- **问题**
  - 全库 grep 显示 `ResolvedImplicitTypeRef` 从来没有被实例化过。
  - KDoc 却声称"已成功推断的隐式类型"会走这个分支，属于误导。
  - Kotlin 的 `coneTypeSafe` 只处理 `FirResolvedTypeRef`。
  - 属于类别 1。
- **框架级替代**：删除这个分支。
- **状态**：未提交；确定。来源：resolve 组 #12。

#### I6　测试辅助的阶段顺序与生产相反（低）

- **位置**：`cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirMacroAnnotationSourceModuleTest.kt:307-329`（未提交）。
- **问题**
  - `resolveThroughBody` 先跑 CJMP_MATCHING，再跑 IMPLICIT_TYPES。
  - 生产中的 `CfirTotalResolveProcessor.kt:54` 按枚举顺序执行，先跑 IMPLICIT_TYPES。
  - 这个测试恰好用了 Kotlin 的顺序，因此掩盖了 A1。
  - 属于类别 6。
- **框架级替代**：测试直接按 `CfirResolvePhase.entries` 的顺序驱动。
- **状态**：未提交；确定。来源：resolve 组 #13。

#### J1　`runCatching` 静默兜底（中）

- **位置**
  - `CfirCjmpMappingStorage.kt:354`（`cjmpHasDefaultValue`）。
  - `CfirCjmpMappingStorage.kt:313-314`：可空访问器在找不到组件时按"无配对"处理。
  - `CfirCommonSpecificChecker.kt:1026-1028,1035-1036`（`CfirCjmpCommonSideChecker`）：`49504baf1` 为兼容旧版 LL 的 `error("Should not be called")` 而加。
  - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConflictsHelpers.kt:575`。
- **问题**
  - `runCatching` 捕获的是 Throwable。在 IDE 中，`LLCfirModuleData.session` 可能在这里现取甚至现建 session（`LLCfirModuleData.kt:102-104`），此时会吞掉 `ProcessCanceledException`。吞掉之后，"没有默认值""没有 common 文件"之类的结论就固化进了结果。
  - 漏注册了组件的 session 会静默失去全部 CJMP 行为，而不是尽早报错。
  - provider 抛异常时，"同一个包只处理一次"的去重会失效，导致重复报告，或者静默跳过某个模块。
  - 属于类别 2。
- **依据**：Kotlin 对应的访问器是非空的（`ExpectActualAttributes.kt:113`）。计划没有登记。
- **框架级替代**
  - 去掉所有 `runCatching`。
  - 组件统一注册，只保留非空访问器。
  - 是否启用 CJMP，由 `isCjmpSpecificCompilationEnabled` 判定。
- **状态**：已提交。代码层面确定；ProcessCanceledException 实际被吞属于可能。来源：配对引擎组 #8、检查器组 #15、LL 组 #11、resolve 组 #4。

### 4. 未提交修改中的非框架级改动

提交这批未提交修改前需要先处理。

**生产代码**

| 条目 | 文件 | 改动 |
|---|---|---|
| B1 | `CfirCjmpResolver.kt`、`CfirCommonSpecificChecker.kt` | `!isPrimary` 过滤 |
| C3 | `CfirCommonSpecificChecker.kt` | 构造器分支、note |
| D1、D2 | `CfirGenericInstantiationChecker.kt` | `ownerDirectSignatures` 补签名、`containsUnfixedTypeParameterOrVariable` 早退 |
| D3 | `CfirExtendExtraChecker.kt` | `isIndependentInterfaceDefault` 的 private 条件 |
| D5 | `CfirClassUseSiteMemberScope.kt` | `isImplementedByLocalFunction` 与 `memberOwnerExtend` 的传播 |
| D6 | `CfirGenericInstantiationChecker.kt` | CPointer 路径上的 use-site 可见性过滤 |
| H1 | `CjoFullIdResolver.kt`、`CfirCjoPackageMetadataProducer.kt` | 读写两端的 `_CNat3AnyE` |
| H10 | `CfirCjoPackageMetadataProducer.kt` | identifierPos 双路提取 |
| A1 | `AbstractCjmpMatcher.kt`、`CfirCjmpMatchingContext.kt` | `hasInferredReturnType` 分支（阶段前移后，应改为按"是否隐式返回类型"判定） |
| I1 | `CfirDeclarationsResolveTransformer.kt` | 错误过滤只看根类型 |
| I2 | `CfirExpressionsResolveTransformer.kt` | 歧义包限定符的回滚 |
| I5 | `CfirTypeUtils.kt` | `coneTypeSafe` 的死分支 |

**测试与夹具**

| 条目 | 文件 | 问题 |
|---|---|---|
| D4 | `CjmpTwoPhaseCompilationTest.kt` | 期望被翻转 |
| H1 | `CjoFullIdResolverTest.kt` | 把硬编码表的边界写成了断言 |
| A1 | `AbstractCjmpMatcherTest.kt` | 把"配对晚于 IMPLICIT_TYPES"固化成了期望 |
| I6 | `CfirMacroAnnotationSourceModuleTest.kt` 的 `resolveThroughBody` | 阶段顺序与生产相反 |
| G6 | `firstFitCandidateDiagnostic.cj` 及相关断言 | 固化了多 common 模块的 first-fit 语义 |
| G1 | `AbstractCfirCjmpMatchingTest.kt` | 继续扩展按模块名分支的写法 |
| G3 | `CaTestPlatformServices.kt` | 只修了测试 provider |
| G8 | `AbstractOverriddenDeclarationProviderTest.kt` | 求值顺序被调换 |

**经审查没有问题的未提交修改**

- `CaCfirSymbolRelationProvider` 改用 scope 遍历，并 unwrap fake override。对应 Kotlin 的 `KaFirSymbolDeclarationOverridesProvider`，同时去掉了原来按字符串身份做 BFS 的兼容层。唯一的问题是 `allOverriddenSymbols` 的 KDoc 还写着"广度遍历…按稳定身份去重"，已经过时。
- `CaSymbolByCfirBuilder.buildType` 在公开类型的边界上近似 Ideal 类型，与 Kotlin `KaSymbolByFirBuilder.kt:476` 相同。
- overriddenSymbols 的三个 testData 补上了 `open`/`override`。依据是 v1.1.3 `StructInheritanceChecker.cpp:1125-1128`：覆盖非 open 成员会报错，所以原夹具本身就是非法仓颉代码。
- 两份 importAliasUsages testData 补上了 `public`。因为 `sample.consumer` 不是 `sample.provider` 的子包，从它导入 internal 声明本来就不合法。
- `CangJieReferencesSearchExecutor.kt` 审查时内容与 HEAD 相同，没有实际改动。
- `CjTestModuleStructureFactory` 写入 dependsOn，对应 Kotlin `TestModuleStructureFactory.kt:149`（生产侧的缺口见 G1、G3）。
- `bottomType03.cj` 新增的 `INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE`，有 cjc 探针支持（`TypeChecker.cpp:2790-2796`）。
- `cjmpInferredReturnTypePostCheck.cj` 对应 `CheckCJMP.cpp:543-567`。

**提交处理（§12 决策 2）**：D1–D4、H1 这几处补丁会在修复计划的 P4、P8 中被替换，它们不能作为"修复"提交。按决策 2 的处理方式：

- 作为临时补丁提交，提交说明中明确标注"临时补丁，待 P4/P8 替换"；
- 在本文件中逐条登记；
- 由 P4、P8 负责替换并删除。

2026-09-29 已验证并分批保存临时基线（不是下列条目的最终修复）：

| 提交 | 已保存的原有修改 | 后续责任阶段 |
|---|---|---|
| `465b87d13` | A1/B1/C3 的 matcher/checker 修改，G1/G3/G6 的 LL 测试和 dependsOn 测试装配 | P1/P2/P3/P7 |
| `3701bf948` | D1–D6 的 owner/private/signature 补丁、D4 两段式期望、H1/H10 的 CJO 修改，以及 I1/I2/I5/I6 的推断与测试辅助 | P1/P4/P8/P9 |

共享文件中 import 物理源码、compiled-stub 与 Analysis override 预热的 hunk 未纳入这些提交。

### 5. 排除项：看起来像特判，但确认是框架级或官方语义

下列写法经核对属于官方语义，或者放在了正确的 owner 上，不计入问题。修复时应当保留。

**配对与存储**

- **first-fit 保留先前候选的诊断，由 checker 重放**
  - 官方的做法是即时报告、不撤销（`CheckCJMP.cpp:898-913,936-957`）。
  - "matcher 记录、checker 报告"的分层与 Kotlin 一致。
  - 唯一的问题是重跑会污染记录，见 A3。
- **matcher 包含返回类型协变和"两侧都有默认值"的判定**：对应 v1.1.3 `MatchCJMPFunction`（`CheckCJMP.cpp:915-991`）；计划 §10 Phase 4.4 已登记。
- **推断返回类型的不兼容延后到配对之后，报 RETURN_TYPE_INCOMPATIBLE**：对应官方 Quest + PostTypeCheck（`CheckCJMP.cpp:545-569`），§11.4 有探针。函数体出错的情况是例外，见 A1。
- **显式写出的无效类型记为 TYPE_NOT_RESOLVED，最终报 NOT_MATCHED**：官方的无效类型同样配不上（`TypeManager.cpp:1952-1965`）。
- **父容器未配对时，成员不单独配对**：有 cjc 探针支持（`cjmpKindMismatch*.cj`、`cjmpNominalGenericArityMismatch.cj`），对应官方"先合并、再配对"（`CheckCJMP.cpp:219-295`）。
- **extend 的合并规则**：同 key 的全部 common 合并进一个 specific；重复的 specific 判为重复；key 未解析的候选跳过。见 `CheckCJMP.cpp:387-452`；被报者顺序上的差异已在 §9.8 登记。
- **common enum 为非穷尽时，specific 多出的构造器不报**：`CheckCJMP.cpp:186,1147-1150`。
- **缺函数体（MISSING_BODY）时不绑定，只报 must-have-implementation**：`CheckCJMP.cpp:905-909`，有 1.1.3 实测去重。
- **`cjmpHasCommonDefault` 对 specific 返回 true**：对应官方 `HasDefault`（`ParseCJMPDecl.cpp:25-28`）。`needToReportMissingBody` 对应 `CheckCJMP.cpp:730-738`。
- **顶层同名组按文件名和源码位置排序；LL 在锁外先把整组推进到前一阶段**：
  - 排序对应官方按文件名顺序的 first-fit（`CompileStrategy.cpp:141-144,422-425`；`CheckCJMP.cpp:679-693,1193-1200`）；
  - 锁外推进由 Kotlin `LLFirTargetResolver` 中 `doResolveWithoutLock` 的 KDoc 许可。
- **被拒绝的第二个实现保留在反向表中**：官方每次 TrySetSpecificImpl 失败都会报 MULTIPLE（`CheckCJMP.cpp:900-903`）。
- **static init 不参与配对**：`ParseCJMPDecl.cpp:258`。
- **主构造不能作为对应物**：这个结论本身正确（`CheckCJMP.cpp:682`），问题只在实现的落点，见 B1。
- **版本门和模式门在 `processFile` 处整体早退**：对齐 Kotlin 的 `FirExpectActualMatcherProcessor.processFile`。
- **eager 与 LL 共用同一个入口**：都走 `transformMemberDeclaration → CfirCjmpMatchRunner.matchDeclaration`。
- **LL 进入外层 class-like/extend 时，先推进外层并完成配对**：与 Kotlin `LLFirExpectActualMatcherLazyResolver.kt:48-56` 相同。
- **存储只写 specific 一侧**：符合 C25，Kotlin 也只写 actual 一侧。读取方式的问题见 A2。

**检查器**

- **冲突与 extend 豁免**：`CfirConflictsHelpers.isCommonAndSpecific`、`CfirExtendExtraChecker.isCjmpCounterpartOf` 按属性位豁免，并受版本门约束。
  - 官方：`PreCheck.cpp:256-268` 的 `multiPlat`、`CheckCJMP.cpp:372-379`。
  - Kotlin：`FirConflictsHelpers.kt:446-454`。
  - 其中 `runCatching` 的问题见 J1。
- **`checkModifiers` 的三个例外**（abstract 可由 open 实现、static abstract、sealed 对 abstract）：`CheckCJMP.cpp:741-790`。
- **缺函数体时只报 SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION，不再追加 NOT_MATCHED**：`CheckCJMP.cpp:730-738,904-908`。
- **`mustReportNotMatched` 的两个豁免**（接口成员；带 COMMON_WITH_DEFAULT 的 enum 构造器）：`CheckCJMP.cpp:696-727`。
- **COMMON_NON_EXHAUSTIVE… 只在 common 穷尽、specific 非穷尽时报**：`CheckCJMP.cpp:182-191`。
- **CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS 只针对 class**：`CheckCJMP.cpp:1241-1266`。
- **COMMON_PACKAGE_HAS_MAIN 按文件判定，只在 COMMON 模式下报**：`TypeCheckDecl.cpp:125-133`、`ParseCJMPDecl.cpp:125-138`。
- **file part 诊断每个文件报一次，锚在 package 上**：`ParseCJMPDecl.cpp:108-140`；D10、§8.1 已登记。
- **泛型约束复用 `CfirOverrideChecker.checkGenericConstraintCompatibility`**：官方同样复用 `CheckGenericTypeBoundsMapped`（`CheckCJMP.cpp:792-799`）。类型参数映射的问题见 E8。
- **解析族规则放在检查器里**：
  - Kotlin 有 `FirExpectConsistencyChecker`、`syntax/FirDelegationInExpectClassSyntaxChecker` 的先例；D13 已登记；
  - `modeAdmits` 的短路与官方 `CheckCJMPModifiers` 返回 false 一致。
- **`CfirCommonSpecificExtendChecker` 只在 COMMON 模式、只对无接口的 extend 生效**：`CheckCJMP.cpp:1164-1171,1273-1274`。判重键的问题见 E9。
- **static init 中给 common let 赋值报 COMMON_ASSIGN_…**：官方 `CheckLetFlag` 先于 `GlobalVarChecker` 执行；§9.8 已登记。
- **`ModifierCheckerTargets` 的 cjmp 目标谓词**：与 `ParserModifierRules.cpp:390-431` 一致；main 的例外已登记为 C43。
- **`CfirCjmpMatchingChecker` 只消费 storage，并注册为 basic 声明检查器**：对位 Kotlin `FirExpectActualDeclarationChecker`。
- **MULTIPLE_COMMON_IMPLEMENTATIONS 经存储反向索引锚在 common 上**：`CheckCJMP.cpp:898-903`；已登记为 C17。
- **SPECIFIC_HAS_DEPRECATED_ANNOTATION**：`CheckCJMPAnnotations.cpp:55-66`。
- **名称相近但与 CJMP 无关的代码**：
  - `CfirGeneralSemanticsChecker`、`CfirCJMappingCheckers`、`CfirNotImplementedOverrideChecker` 中命中的 `cjmp`，指的是 CJ Mapping 互操作（`resolvedInteropInfoOrNull()?.cjmp`）；
  - `CfirImportsChecker` 里没有 CJMP 代码；
  - `CfirCjmpMappingInfo.isSupportedOnCommon/isSupportedOnSpecific` 同样属于互操作。

**序列化与会话装配**

- **写侧的 `position()` 统一把 0 基换算成 1 基，读侧原样保存**：官方位置就是 1 基（探针 `common.cj:4:1`、C45 `4:12`）。
- **common part 中所有声明都写 FROM_COMMON_PART**：官方 `ASTWriter.cpp:1576`。问题只在读侧，见 B1。
- **nominal/extend 的 COMMON_WITH_DEFAULT 由全部 common 成员派生**：`ParseCJMPDecl.cpp:150-170`。
- **enum 构造器的序列化形状**：无 payload 写 VarDecl，有 payload 写 FuncDecl；`EnumInfo.body` 排除合成的主构造。这是官方 AST 形状，见 §11.3 探针。
- **构造器不写 generic 和 GENERIC 位**：`ASTWriter.cpp:1127-1131`。
- **类型参数按 symbol 归一**：对应官方 `SaveGeneric` 中的 gty→decl 归一。
- **common/specific 声明上的普通自定义注解会被序列化**：`ASTWriter.cpp:1727-1741`。
- **common part 的 allFiles 写完整路径**：`ASTWriter.cpp:847`。
- **是否写 common part 取决于 CHIR 输出模式**：`ImportManager.cpp:296-298`。
- **反序列化声明按路径挂到 depends-on 模块数据上**：与 Kotlin `DependencyListForCliModule`/`MultipleModuleDataProvider` 同构。问题只在于加载的是哪个文件，见 H2。
- **依赖 provider 和 extend provider 都纳入 `allRefinementDependencies`**：Kotlin `FirAbstractSessionFactory.kt:359`；LL 侧纳入 `transitiveDependsOnDependencies`，对应 Kotlin `LLFirAbstractSessionFactory.kt:654`。
- **`parseCommandLineArguments` 由参数 schema 驱动**：对应 Kotlin cli-common。
- **`CfirCjmpSettingsComponent` 作为 session 组件承载编译模式（D14）**：结构本身是框架级的。模式判据的来源问题见 H6，LL 不注册的问题见 G1。
- **C45 的 note 使用 identifierPos**：官方 `Diags.cpp:195`。

**泛型实例化与 extend owner**

- **`instantiatedDirectExtends` 只收带类型参数的 extend**：对应官方 `InstantiatedChecker.cpp:125-141` 中的 `TestAttr(GENERIC)`。
- **owner 自己的 private 纳入、peer 的 private 排除**：官方 `InstantiatedChecker.cpp:81`；`StructInheritanceChecker.cpp:49-65,409,699`。
- **`bothExtendsAreExternalAndPeerVisible` 的同包、跨包分支**：`StructInheritanceChecker.cpp:535-563` 的 `areAllFromOtherPkg`。
- **peer 是否适用，按 owner 声明的目标类型判定**：`StructInheritanceChecker.cpp:686-694`。
- **`extendMemberPeerDecision` 的 INCLUDE / EXCLUDE / UNDECIDABLE 三种结果**：对应 `StructInheritanceChecker.cpp:567-627`，放在共享的 resolve 服务里。
- **每个触发源只报一条诊断，并附 found candidate note**：`DiagnosticEngine.cpp:795`、`InstantiatedChecker.cpp:416-437`。
- **`overridesFunctionCandidate` 先 `lazyResolveToPhase(TYPES)`**：Kotlin `FirStandardOverrideChecker.kt:95-96,157-158`。
- **`CfirTypeSubstitutorByMap.fromTypeParameterMapping`、`createTypeParameterSubstitutorForOverride`**：providers 层的共享工具。
- **extend 的 provider 与索引**：
  - `CfirCompositeExtendProvider` 按 symbol 去重；
  - `CfirExtendProviderComposer.lazyCombine`（依赖集合的缺口见 G4）；
  - `CfirExtendIndexStore.recordDeclarationSources`。
- **`isImplementableForVisibility`**：`StructInheritanceChecker.cpp:1179-1188`。
- **`includedRootExtends` 只作用于根**：`StructInheritanceChecker.cpp:448-510` 的 `ignoreExtends=true`。开关本身的分层问题见 D9。
- **`sameCjoSourceFile` 放宽已撤回**：工作树和 `git log --all -S` 中都没有残留。撤回后对应的测试期望被翻转，见 D4。

**resolve、raw 构建与 PSI**

- **`CfirCompositeSymbolProvider.withoutCjmpShadowedCommon` 让 specific 类型优先**：对应官方 `PreCheck.cpp:408-441`（同一作用域层级内 specific 优先于 common）。行为正确，只是 KDoc 引用的出处错了。
- **`common`/`specific` 作为软关键字，以及标识符位置的后随集合**：`Lexer.cpp:46-51`、`ParserUtils.cpp:663-676`。
- **隐式 abstract 的函数部分**：class 体内的 common 成员不隐式 abstract（`ParserImpl.cpp:270-277`）；CJMP 抽象类里没写 abstract 的函数不是 abstract（`ParseDecl.cpp:2164-2185`）。属性部分的问题见 E13。
- **修饰符不从容器传染到成员**：对应 `parse_cjmp_outdecl_miss_match`。
- **isCommon/isSpecific 在 raw 构建和 STATUS 阶段的装填与透传**：对位 Kotlin 的 isExpect/isActual。
- **`Cjmp*SymbolMarker`**：对位 Kotlin `DeclarationSymbolMarker`。
- **`cjmpHasDefaultValue` 进入 `CfirMapArguments`**：参数映射本身就是 owner，不算旁路。覆盖面的问题见 C5。
- **推断返回类型时把理想字面量默认化**：官方 `ReturnExpr.cpp:42`，不以 CJMP 为条件。
- **保留尾部 return 的错误**：`ReturnExpr.cpp:51-55`、`TypeChecker.cpp:2764-2799`。
- **`c7731df8c` 让源码中的限定类型名只经 import binding 解析**：`PreCheck.cpp:482-513`。

### 6. 核实记录

**主审查者亲自核对的项（全部成立）**

| 条目 | 核对内容 |
|---|---|
| B1 | `CfirDeclDeserializer.kt:804` 的映射；官方 `CollectDecl` 只认 COMMON，并跳过 PRIMARY_CTOR_DECL |
| H1 | 读写两端都有 `_CNat3AnyE`；`addDefaultBoundIfNecessary` 会合成 Any 上界，写侧原样写出 `parameter.bounds`；官方产物 `ownermatrixlib.cjo` 中 `_CNat3AnyE` 出现 0 次 |
| G1 | `CfirCjmpSettingsComponent` 只在 eager 工厂、测试门面和测试中注册；测试按模块名 `"modeNone"` 决定模式 |
| D4 | 两段式测试期望被翻转的 diff |
| H3 | 生产代码只在 `AbstractFrontendPipeline` 中给 `cjoOutputDirectory` 赋值，且仅在 `compileCjd` 时；`writeLiveCjoOutputs` 没有输出目录时直接返回 true |
| F1 | `typesEquivalent` 最后退回 `==` 比较 |
| F2 | 官方 `ParseCJMPDecl.cpp` 只禁止 `common` 的模式声明 |
| A1 | 阶段顺序；`transformFunctionContent` 中 `bodyResolved` 导致跳过；`reduceCjmpShadowedCommonCandidates` 遇到 `storage.isEmpty` 早退 |
| E4 | `Compatibility.kt` 注释的前提，与官方 `CLASS_BODY_FUNCDECL_MODIFIERS` 对照 |
| E3 | 超类型只比较 `superTypeRefs.size` |
| E1 | `checkCJMPAbstractClassMembers` 与官方 `DeclAttributeChecker.cpp:311-322` 对照 |
| H14 | `PROBE-EXE` 调试输出 |

**审查组之间结论冲突的裁决**

- **CJMP_MATCHING 的阶段位置**
  - LL 组的看法：放在 IMPLICIT_TYPES 之后有官方依据，理由是返回类型协变需要推断结果。
  - 配对引擎组和 resolve 组的看法：应当前移。
  - 裁决：前移。官方在 `TypeChecker.cpp:2042-2047` 中，在所有函数体检查之前完成配对；隐式返回类型按 Quest 接受，由 PostTypeCheck 复核。因此协变判定并不依赖推断结果。
- **F2 的严重度**
  - LL 组以"官方在解析期拒绝所有 CJMP 模式声明"为前提，把 F2 定为低严重度。
  - 核实 `ParseCJMPDecl.cpp:251-254` 后发现，官方只禁止 `common`，这个前提不成立。
  - 裁决：按配对引擎组的判断，定为高严重度。

**需要用探针或 fixture 定性的项**（作为 P0 阶段的输入）

| 条目 | 需要确认的内容 |
|---|---|
| B1 | 官方的 enum 构造器是否携带 COMMON 位（`SetCJMPAttrs` 只补 COMMON_WITH_DEFAULT，其他传播点尚未确认） |
| C1 | specific 类的隐式构造（官方 AddDefaultCtor 的条件） |
| C3 | 未标记的 common 成员与未标记的 specific 成员同签名时的冲突 |
| D3 | 反例 `extend B <: I1 {}` 加 `extend B { private func foo(): Unit {} }`，以及 `private_function_instantiation.cj` |
| D4 | 用当前的库源码重跑两段式探针 |
| E2 | 两侧都带 `@OverflowWrapping` 时的行为 |
| E4 | 类成员上的 `common private` |
| E8 | 存在外层泛型映射时的约束比较 |
| E9 | 两个 common extend 中各有一个 private 重载 |
| F2 | LL 下 `specific let (a, b)` 是否抛异常 |
| F3 | 非穷尽 enum 中未配对的 `specific func` |
| F5 | 泛型参数个数 N≠M≥1 的情况 |
| G8 | 求值顺序是否影响结果 |
| H9 | specific 产出的 CJO 是否包含 common 声明 |

**审查的局限**

- 本次是只读审查，没有运行 gradle，也没有运行 cjc。标为"推断"的结论都来自对代码路径的逐行推演。
- 行号以 2026-09-28 的工作树为准。审查期间，另一个 agent 修改了 `CjoSearchPath.kt`、`CfirDeclDeserializer.kt` 等文件。

---

## 第二部分：修复计划

### 7. 修复原则

1. **每个阶段修一个共享 owner**。owner 修好后，把它在 §2/§3 中对应的下游补丁全部删除，不保留兼容层，也不保留双路径。每个阶段都列出"必须删除的补丁"清单，作为验收条件。
2. **语义以官方为准，覆盖全部官方版本（§12 决策 1）**。
   - 依据：各官方版本的 cjc 实测结果，以及 `external/cangjie_compiler` 中对应 tag 的源码。Kotlin 只作架构参考。
   - 不以某一个版本作为唯一基线。版本之间行为有差异时，统一这样处理：
     1. 用 `git -C external/cangjie_compiler tag --contains <提交>`，或各版本 cjc 实测，确定该行为从哪个版本引入；
     2. 在 `common/src/org/cangnova/cangjie/LanguageVersionSettings.kt` 中定义对应的 `LanguageFeature`，KDoc 写明官方提交与日期，与 `CommonSpecificDeclarations` 等现有写法一致；
     3. 用语言版本门禁选择行为。
   - 引入版本还没有对应的 `LanguageVersion` 常量时（当前只到 `CANGJIE_1_1_3`，官方已有 1.2.0 beta、1.3.0 alpha），按仓库现有约定补充常量。
   - 每一处语义改动都要能追溯到一次探针结果或一行官方源码。
3. **登记所有偏离**。与 Kotlin K2 的结构偏离（例如合并视图、first-fit、Quest 延后判定）写进 KDoc，并登记到 [原实施计划](cjmp-implementation-plan-20260923.md) 的偏离记录中。
4. **修改 fixture 必须通过 skill `cangjie-cfir-llt-repair` 的 Fixture Edit Gate**。
   - 只有在官方证据证明原期望错误时，才允许修改期望。
   - 不为迎合当前实现而改期望。
   - D4、G6，以及 H1 相关的期望，按 P0 的探针结论恢复或重写。
5. **先红后绿**。每个问题先写出能复现它的 fixture（PSI/LightTree 的 LLT、LL、两段式编译），确认它确实失败，再动手修。
6. **按阶段验收**。
   - 每个阶段结束时，先跑受影响的定向测试，再跑全量 `:cfir:analysis-tests:test`。
   - 全量结果与 v10 快照（`20260928-cjmp-final-full-v10`）逐条对比：除本阶段有意修正的期望外，不允许出现任何回归。
7. **协调**。gradle 一律走共享队列；与并行的 agent 如何协调见 §11。
8. **禁止项同样适用于修复本身**：§0 列出的 8 类非框架级写法，在修复中同样不允许出现。

### 8. 阶段总览与依赖

说明：修复阶段编号使用 P 前缀（P0–P10）；A1–J1 是审查条目的编号。注意审查条目中的 F1–F8 是匹配器相关问题，不是阶段。

| 阶段 | 修复的共享 owner | 覆盖条目 | 前置阶段 |
|---|---|---|---|
| P0 取证与红测试 | 无 | §6 中所有待探针项；跨版本差异表；每条高严重度问题的复现 fixture | 无 |
| P1 配对阶段与读取入口 | `CfirResolvePhase`；配对结果的访问器 | A1、A2、A3、F6、F8、J1、I6 | P0 |
| P2 反序列化 common 语义 | `CfirDeclDeserializer`；共享的候选谓词 | B1 | P0 |
| P3 合并视图 | providers 层的合并 scope；tower 上的 actualizing scope；默认参数解析 | C1–C5 | P1、P2 |
| P4 声明级实现关系 | `CfirClassUseSiteMemberScope`；实例化成员图 | D1–D9 | P3 |
| P5 匹配器共享判据 | `CjmpMatchingContext`、`AbstractCjmpMatcher` | F1–F5、F7 | P1 |
| P6 检查器对齐与分层 | `AbstractCjmpChecker`；修饰符分表；注解分类表；成员属性检查 | E1–E14 | P3（仅 E 组中与合并视图相关的部分）、P5 |
| P7 生产 LL/IDE 入口 | LL session 工厂；平台 provider；阶段门 | G1–G9 | P1 |
| P8 序列化、CLI 与诊断通道 | CJO 读写；common part 加载器；CLI 配置；二进制 source element | H1–H14 | P2；其中 H9 还依赖 P3 |
| P9 resolve 顺带修复 | body resolve；包限定符；类型解析；import binding | I1–I5 | 无 |
| P10 收口 | 无 | 全量回归、文档、parity 复审 | 全部 |

**执行顺序**

- 关键路径：P0 → P1 → P2 → P3 → P4 → P6 → P10。
- 可并行推进的旁支：
  - P5、P7：P1 完成后即可开始；
  - P9：随时可以开始；
  - P8：H1–H8 在 P2 之后即可开始，H9 要等 P3 完成。
- 这些旁支改动的文件与关键路径基本不重叠。
- 先做 P1–P3 的原因：配对阶段位置、读取入口和合并视图是其他大部分补丁的根源。这三步做完后，C 组和 D 组的多数补丁可以直接删除，而不需要逐个重写。

### 9. 分阶段任务

#### P0　取证与红测试

- **目标**：为每条高严重度问题和 §6 中的待探针项建立可复现的证据，作为后续各阶段的验收基线。
- **任务**
  1. **cjc 1.1.3 探针**
     - 两段式流程：
       - common 侧：`cjc common.cj --experimental --output-type=chir`；
       - specific 侧：`cjc specific.cj <pkg>.chir --experimental --common-part-cjo=<pkg>.cjo -o out.dll --output-type=dylib`；
       - `CANGJIE_HOME` 必须指向 1.1.3。
     - 探针源码和输出存放在 `.workbuddy/tmp/cjmp_probe113/review-20260928/`，覆盖 §6 待探针表中的全部条目。
  2. **官方源码核对**：enum 构造器 COMMON 位的来源（B1）；AddDefaultCtor 的触发条件（C1）；CFIR 中与官方 PRIMARY_CTOR_DECL 对应的表示（B1）。
  3. **跨版本差异清单与门禁设计（§12 决策 1）**
     - 在 v1.0.x、v1.1.x、v1.2.0 各 beta、v1.3.0 各 alpha 和 origin/main 之间，逐项比对 CJMP 相关行为。重点：
       - H6：加载门、CLI 形态、Specific 判据；
       - E2、E7：注解分类表和禁止集；
       - G6、F4：多 common parent 与 DAG；
       - F5：泛型参数个数判据；
       - 其他在审查中已经发现差异的项。
     - 对每一项差异：
       1. 用 `git -C external/cangjie_compiler tag --contains <提交>` 确定它从哪个 tag 开始出现；
       2. 能跑 cjc 的版本，就用 cjc 实测确认；
       3. 写出对应的 `LanguageFeature` 草案：名称、引入版本、官方提交与日期、控制的行为；
       4. 如果需要新增 `LanguageVersion` 常量，也一并列出。
     - 输出一张差异表，存放在本文件的附录中。P5、P6、P7、P8 按这张表实现各自的版本门。
  4. **按官方行为编写复现 fixture**（当前应当失败），至少包括：

     | 条目 | fixture |
     |---|---|
     | A1 | `func use3() { a() }`、`func use4() { platform() }` 两个 e2e，调用方省略返回类型 |
     | A2 | LL 诊断用例：跨文件的调用方先于被调方被分析 |
     | B1 | 两段式测试的 common 源里加一个未标记的 `helper`，并断言 `messages`/`completed`；`specific func g()` 对应 common 中未标记的 `func g()` |
     | C1 | `Repo().extra()` 调用 common 的默认成员 |
     | C2 | 未配对的子类型 `D<Int64>`，以及对应的两段式 CLI 用例 |
     | C3 | 未标记 common 与未标记 specific 的同签名成员 |
     | C4 | 函数引用 `apply(platform)` |
     | C5 | 隐式 `super()` 加 common 默认参数 |
     | D1 | public 的 owner 成员；有界 owner 自带接口 |
     | D2 | 具体实例化 `Holder<Int64>` |
     | E1–E4、F1、F2、G1、H1–H3 | 各自的最小复现 |

- **约束**
  - 复现 fixture 只能按官方行为写期望，不能把当前的错误输出写进期望。
  - P0 只产出探针证据和 fixture 草稿，不合入主干。复现 fixture 随对应阶段一起提交，在阶段内部完成先红后绿。
- **验收**
  - 每条高严重度问题至少有一个复现 fixture。
  - §6 中每个待探针项都有探针结论的记录。
  - 跨版本差异表完成，每一项都有引入版本、官方出处和 `LanguageFeature` 草案。

#### P1　配对阶段与读取入口（A1、A2、A3、F6、F8、J1、I6）

- **目标**：配对结果先于所有消费者产出；所有读取都经过带惰性推进的访问器。
- **改动**
  1. **移动阶段**
     - 在 `CfirResolvePhase` 中，把 CJMP_MATCHING 移到 EXTENSIONS 与 IMPLICIT_TYPES 之间。
     - 同步更新：阶段 KDoc、`CfirResolveProcessors.kt` 中的阶段序列说明、BODY_RESOLVE 的输入说明。
  2. **matcher 按 Quest 语义处理返回类型**
     - specific 省略返回类型时，按官方 Quest 先接受，延后到 checker 复核协变。
     - 显式返回类型照常判断协变。
     - specific 非空函数体的真实 Quest 返回延后检查；空体在预检已是 Unit，显式错误类型不能作为 Quest 放行。common 源码依赖在进入 CJMP 锁前完成 IMPLICIT_TYPES，与两段式编译的 common CJO 一致。
     - LL 在 `doResolveWithoutLock` 的短读锁中收集 common 候选，退出读锁后准备其签名，再返回标准流程获取 CJMP 写锁。读锁和写锁内都不得推进到 IMPLICIT_TYPES：阶段契约在 project 内跨 session 共享。参照 Kotlin `LLFirAnnotationArgumentsLazyResolver.kt:96–116`，不得放宽契约或永久把原始省略类型当作 Quest（2026-09-29 精确探针与锁链复核修正）。
     - 删除 `hasInferredReturnType`。协变复核只保留一份，由 P6 统一到 `AbstractCjmpChecker`。
  3. **访问器**
     - 正向配对结果和外层类型参数映射，作为 specific 声明上的 `CfirDeclarationDataKey` 属性保存（对位 Kotlin 的 `expectForActual`）。
     - 对外只提供一个读取入口，例如 `CfirBasedSymbol<*>.cjmpCommonCounterpart`。它在读取前先执行 `lazyResolveToPhase(CJMP_MATCHING)`。
  4. **存储收窄**
     - session 级存储只保留 checker 需要的反向聚合：MULTIPLE_COMMON_IMPLEMENTATIONS、common 方向的 NOT_MATCHED。
     - 拆分只读接口和写接口（F8）。
     - 反向查询只允许在 checker 中使用，并且要先经 specific session 的 provider 找到候选、把候选推进到 CJMP_MATCHING 之后再读。
  5. **同名组只计算一次（A3）**。两个方案二选一，建议选第二个：
     - 按组计算一次，并一次性写入各个成员；
     - 或者 matcher 只对单个声明计算兼容性，"第二次绑定"交给 checker，按官方顺序（文件名 + 声明顺序）推导。第二个方案更接近 Kotlin"每个声明只写自己的状态"的做法。
  6. **外围容器单一来源（F6）**：删除 ThreadLocal 和按 ClassId 查找的成员分支。
  7. **去掉兜底（J1）**：删除所有 `runCatching`；组件统一注册；只保留非空访问器。
  8. **测试**
     - `resolveThroughBody` 改为按 `CfirResolvePhase.entries` 驱动（I6）。
     - `AbstractCjmpMatcherTest` 按 Quest 语义重写相关用例。
- **参照**
  - Kotlin：`FirResolvePhase` 中 EXPECT_ACTUAL_MATCHING 的位置、`ExpectActualAttributes.kt`、`FirExpectActualMatcherTransformer`。
  - 官方：`TypeChecker.cpp:2042-2047`、`CheckCJMP.cpp:545-569,898-913`。
- **必须删除的补丁**
  - `hasInferredReturnType` 分支；
  - ThreadLocal 外围上下文；
  - 按 ClassId 查找的成员分支；
  - 存储访问中的 `runCatching`；
  - 可空访问器中的"无组件即无配对"兜底。
- **验收**
  - A1、A2 的复现 fixture 转绿。
  - 同名重载组在 eager 和 LL 下的存储 dump 一致：把 `AbstractCfirCjmpMatchingTest` 的一致性断言扩展到重载组。
  - CommonSpecific 切片、LL CJMP 测试、两段式测试全绿，全量 LLT 与 v10 相比没有回归。
- **风险**
  - 阶段前移会改变 IMPLICIT_TYPES 阶段能看到的信息。
  - 由于门关闭时 CJMP_MATCHING 是空操作，非 CJMP 模块理论上不受影响；但门关闭时仍需推进阶段，这要等 P7 修正，因此 P1 和 P7 的阶段门改动要一起验证。

#### P2　反序列化的 common 语义与共享候选谓词（B1）

- **目标**：`isCommon` 在源码路径和 CJO 路径上含义一致；"能否作为 CJMP 对应物"只有一个判据。
- **改动**
  1. **状态恢复**：`CfirDeclDeserializer.buildStatus` 中，`isCommon` 只取 COMMON 位。FROM_COMMON_PART 改为独立的来源属性，给需要它的地方读取，例如 E1 中官方 `checkedBefore` 对应的判断。
  2. **enum 构造器**：按 P0 的结论处理。
     - 官方构造器携带 COMMON 位：写侧显式写出 COMMON。
     - 不携带：构造器的候选资格由外层 enum 决定（沿用 §11.1 的逻辑），不再依赖 FROM_COMMON_PART。
  3. **共享候选谓词 `isCjmpMatchingCandidate`**
     - 放在 cfir-tree 的 session 包中，与存储处于同一层。
     - 判据：看 COMMON/SPECIFIC 位，并在结构上排除官方的 PRIMARY_CTOR_DECL 形态。按 P0 的核对结果映射，不能简单等同于 `isPrimary`。
     - resolver、`CfirCjmpCommonSideFacts`、common 方向的 checker 共用这一个谓词。
  4. **删除特判**：删除 `CfirCjmpResolver.kt:227-235,259-262` 和 `CfirCommonSpecificChecker.kt:126,136` 中的 `isPrimary` 特判。§11.3 中写侧对 enum 合成主构造的排除是官方 AST 形状（见 §5），保留。
  5. **补断言**：两段式测试补上对 `messages` 和 `completed` 的断言。
- **官方依据**：`CheckCJMP.cpp:664-693`、`ASTLoader.cpp:298-331,604`、`ParseCJMPDecl.cpp:57-65,143-175`。
- **验收**
  - B1 的复现 fixture 转绿：
    - 未标记的 helper 不再被 CLI 报告器报错；
    - `specific func g()` 对应 common 中未标记的 `func g()` 时，CJO 路径和源码路径都报 NOT_MATCHED。
  - `CjmpTwoPhaseCompilationTest` 全绿，CommonSpecific 切片没有回归。

#### P3　合并视图（C1–C5）

- **目标**
  - 给已配对的 specific nominal 提供一个统一的合并成员面。调用解析、继承、实例化、冲突检查、CJO 写侧都只看这一处。
  - callable 的遮蔽在 tower 中正向完成。
  - 默认参数只有一个共享的解析入口。
- **改动**
  1. **合并成员视图（C1）**
     - 在 providers 层新增 `CfirCjmpMergedDeclaredMemberScope`，装饰现有的 `CfirClassDeclaredMemberScope`。
     - 它的内容是 specific 自己声明的成员，加上配对的 common nominal 中没有被 specific 成员替代的那些成员，包括未标记的成员和构造器。
     - 配对关系通过 P1 的访问器获取；common 成员来自 CJO 时同样适用。
     - 由 `CfirCangJieScopeProvider.getUseSiteMemberScope` 在构建 `CfirClassUseSiteMemberScope` 时统一使用。
     - 这是仓颉相对 Kotlin 的一处结构偏离，需要写进 KDoc：官方对应的是 `MergeCommonIntoSpecific`；Kotlin 没有对应物，因为 actual 必须声明全部成员。
  2. **隐式构造（C1）**
     - 按 P0 核对出的官方 AddDefaultCtor 条件实现。
     - 预期结论是：raw 构建阶段不再为 specific 类补隐式 `init()`，由合并视图提供 common 的构造器。
     - PSI 和 LightTree 两条 raw 路径共用同一个判定。
  3. **重定义检查（C3）**
     - `collectClassMembers` 和 `collectExtendMembers` 改为在合并视图上检测重定义。
     - 在组级别实现 `FilterOutCommonCandidatesIfSpecificExist`（`CheckCJMP.cpp:500-520`）。
     - 所有同签名成员都附上 note（`Diags.cpp:193-196`）。
  4. **actualizing scope（C4）**
     - 在 `ScopeBasedTowerLevel`（`TowerLevelHandler.kt:130`）上，为非类型作用域包一层 `CfirCjmpActualizingScope`。
     - 当同一批候选中出现 specific 时，经 P1 的访问器正向取出它对应的 common，再把这些 common 从候选中剔除。
     - 对位 Kotlin 的 `FirActualizingScope`，以及 `TowerLevels.kt:360-363`。
     - 函数引用和 precollected 两条路径本来就经过 tower，不需要再单独处理。
     - 评估是否恢复 `ConeEquivalentCallConflictResolver` 的兜底。
     - 类型层的 `withoutCjmpShadowedCommon` 保留（依据官方 `PreCheck.cpp:408-441`），只修正 KDoc 里的出处。
  5. **extend 侧**
     - extend 的遮蔽继续由 `CfirExtendMemberScope` 负责，它是 extend 侧的 owner。
     - 判据改为 P1 提供的统一判据。
  6. **默认参数（C5）**
     - 在 providers 层提供 `CfirValueParameter.itOrCommonHasDefaultValue()`，对位 Kotlin 的 `itOrExpectHasDefaultParameterValue`，内部经 P1 的访问器按需推进阶段。
     - 以下调用点统一改用它：`CfirMapArguments`、`CfirConstructorDelegationChecker.requiredParameterCount`、`CfirRecursiveConstructorCallChecker`、CJO 写侧。
  7. **实例化检查（C2）**：`CfirGenericInstantiationChecker` 改为消费合并视图，与 P4 的重构同步进行。
- **必须删除的补丁**
  - `CfirCallResolver.reduceCjmpShadowedCommonCandidates`。
  - checker 中零散的 `isCjmpShadowedCommonDeclaration` 过滤：`CfirGenericInstantiationChecker.kt:999,1007,1465-1469,1489-1492,1507-1511`、`CfirInheritanceDeepChecker.kt:1783,1862`。
  - `CfirCommonSpecificChecker.kt:115-206` 中成对的重载补丁。
  - `CfirGenericInstantiationChecker.kt` 中的投影与自造谓词：`1190-1235,1378-1418,2614-2660`。
  - `cjmpHasDefaultValue`，由新入口替代。
- **验收**
  - C1–C5 的复现 fixture 转绿。
  - `cjmpDefaultReadThrough`、`cjmpUnmarkedDirectMemberSignatureMerge`、`cjmpSpecificInitConflictsWithUnmarkedCommonPrimary`、`cjmpGenericCounterpartInheritedDefaultInstantiation` 等现有 fixture 保持绿。
  - 两段式测试覆盖 CLI 路径下的合并视图，即 common 来自 CJO 的情况。
- **风险**：合并视图会改变 specific 类的成员查找结果，可能暴露新的冲突诊断。官方同样会报的属于正确行为，但需要逐条对照探针确认。

#### P4　声明级实现关系与实例化 owner 组（D1–D9）

- **目标**：实现关系和 override 关系在声明级（替换之前的签名）上建立，实例化只在其上套一层替换；泛型实例化检查不再补签名。
- **改动**
  1. **scope（D5）**
     - `CfirClassUseSiteMemberScope` 判断"本地成员实现了父成员"时，改为在替换之前的签名上进行。
       - Kotlin 对应：`FirKotlinScopeProvider.kt:327-360`，override 图建在 unsubstituted scope 上，实例化时套一层 `FirClassSubstitutionScope`。
       - 官方对应：`MergeInheritedMemberHelper.cpp:139-177`。
     - 祖先 extend 的 private 成员，对所有消费方一律不进入继承图（`StructInheritanceChecker.cpp:49-65,409,699`）。
  2. **owner 组（D1）**
     - 泛型视图改用 owner extend 自己声明的目标类型，也就是带着 where 约束的类型参数。
     - 先在这个视图上建成员图，再按具体实例做替换。
     - 官方对应：`InstantiatedChecker.cpp:60-122`，逐个 extend 处理，并在第 81 行加回 owner 自己的 private 成员。
  3. **删除补丁（D1、D2、D5、D9）**
     - 删除 `ownerDirectSignatures`、`containsUnfixedTypeParameterOrVariable` 早退、检查器中对 `createExtendDeclarationSubstitutionForConstraintDerivation` 的调用、`isImplementedByLocalFunction`，以及 `memberOwnerExtend` 和 `includedRootExtends` 两个开关。
     - 检查器改为经 scope provider 获取 scope。
     - 部分泛型实例按官方同样检查（`InstantiatedChecker.cpp:203-261`）。
  4. **内建目标（D6）**：CPointer 等内建泛型目标复用 nominal 的 owner 组构造，可见性从 owner 的视角判定；删除 use-site 可见性过滤。
  5. **CJO 来源（D4、D8）**
     - 反序列化得到的成员也参与实例化成员图，去掉 `CfirGenericInstantiationChecker.kt:1675` 中的 origin 过滤。
     - 可见性统一走 accessibility checker，所需的包和可见性元数据由 CJO 声明提供。
  6. **shadow 检查（D3）**
     - 先按 P0 的探针定位 OwnerBox 误报在签名比较上的根因。
     - `isIndependentInterfaceDefault` 改为对齐官方的 peer 收集规则，并在声明级比较参数。
     - 删除两个启发式条件。
  7. **跨包顺序诊断（D7）**：从实例化检查器中移除，改到 `CfirImportsChecker.reportImportedExtendConflicts`，对跨包的 extend 组调用 `extendMemberPeerDecision`。
  8. **测试期望（D4）**
     - 按 P0 的探针结论恢复 `CjmpTwoPhaseCompilationTest` 的期望。预期为一条 GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS，并带 CJO 位置的 note。
     - 以下几个 fixture 的期望也按探针复核：`privateFunctionsCauseGenericInstantiationAmbiguity.cj`、`default_implement_19.cj`、`privateExtendOwnerGroups.cj`。
- **验收**
  - 以下复现 fixture 转绿：
    - D1：public owner、有界 owner 的接口、同约束的 peer；
    - D2：`Holder<Int64>`；
    - D5：`Leaf<Int64>`；
    - D6：`CPointer<Int64>`。
  - D4 的两段式测试按官方结论通过。
  - v3–v10 期间反复回归的 fixture 全部稳定为绿，包括 `interface_default_implemented_func_generic_invalid_2.cj`、`default_implement_19.cj`、`privateExtendOwnerGroups.cj`、`privateFunctionsCauseGenericInstantiationAmbiguity.cj`、`Import00.testErrAmbiguous00/01`。
- **风险**：override 图改为在声明级建立，会影响非 CJMP 的继承、override 和实例化诊断，是本计划中影响面最大的一步。需要逐条对照 cjc 探针，并在全量回归中逐项审查差异。

#### P5　匹配器共享判据（F1–F5、F7）

- **改动**
  1. **类型等价（F1）**
     - CFIR context 只保留一份实现：先用外层映射加上声明自身的类型参数映射构造 substitutor，替换 common 类型，再用 `AbstractTypeChecker.equalTypes` 比较。
     - `areTypesEquivalent`、`matchEnumConstructors`、`haveSameExtendKey` 都复用这一份实现。
     - 删除手写的结构比较和 `==` 兜底。
     - 对位 Kotlin：`AbstractExpectActualMatcher.kt:176-194`、`FirExpectActualMatchingContextImpl.kt:344-374`。
  2. **模式变量（F2）**
     - 每个 binding 作为一个独立的 specific，送进共享的 first-fit。
     - `CfirPatternVariable` 本身记录模式级的结果。
     - 按官方 `TryMatchVarWithPatternWithVarDecls` 的规则报告：逐个变量报 NOT_MATCHED，再对整个模式报一次。
     - 全局变量不经过"第二次绑定"判定（`CheckCJMP.cpp:1075-1077`）。
     - LL 的后置条件与写入逻辑共用同一个判据。
  3. **enum 构造器（F3）**
     - `CjmpMatchingContext` 增加对 enum 构造器的支持，构造器走共享的 first-fit，并正确处理 `bind` 的结果。
     - 非穷尽豁免按外层声明判定，覆盖外层中所有 specific 成员，而不只是构造器。
     - 删除 LL 里对 enum 构造器的单独跳过。
  4. **模块同一性与 DAG（F4）**
     - 模块同一性改用 moduleData 的相等性判断。
     - DAG 语义按 P0 差异表的语言版本门禁处理：只支持单个 common part 的版本，只需要直接 depends-on；支持多 common parent 的版本，移植官方语义（包括 first-wave 过滤）。
  5. **泛型参数个数（F5）**：按探针结论，对齐官方的 `hasGenericMismatch`。
  6. **种类与判定顺序（F7）**
     - 种类改用枚举，并用穷举的 `when`。
     - TrySetSpecificImpl 的判定顺序对齐官方：先判断"已被实现"，再判断 MissingBody。
- **验收**
  - `apply<T>(f: (T) -> Unit)`、`f(p: (T, Int64))`、`Value((T) -> Unit)` 三种形状都能配对成功。
  - `specific let (a, b) = (1, 2)` 在 eager 和 LL 下都得到与官方一致的配对结果，LL 不抛异常。
  - `resolution.common` 的 matcher 单测覆盖新的判据。

#### P6　检查器对齐官方，并按 Kotlin 分层（E1–E14）

- **改动**
  1. **分层（E12）**
     - 在 `resolution.common` 中落地 `AbstractCjmpChecker`，与 matcher 共享同一个 context。
     - 由它在验证期枚举所有不兼容项：修饰符、超类型、变量和属性的类型、返回类型协变、注解、泛型约束。
     - CFIR 检查器只负责把不兼容项映射成诊断。返回类型协变的复核只保留这一份。
     - 引入 MppCheckerKind 的等价物，由 collector 驱动 common 方向的检查；删除 file checker 自己遍历依赖模块文件的做法。
  2. **超类型（E3）**：在验证期按类型比较。先按映射替换，再逐个比较接口类型和超类类型（`CheckCJMP.cpp:818-856`）。
  3. **注解（E2、E7）**
     - 按 P0 差异表，为每个官方版本建立注解分类表（NON_SERIALIZED、UNSUPPORTED、special-handled），由语言版本门禁选择，与 CJO 写侧共用。
     - 对全部已配对的声明（包括 nominal）做双向一一匹配，并比较注解实参。
     - 实现 `PostCheckNonSerializedAnnotations` 的属性位比较。
     - 禁止使用的注解集合同样按版本定义。v1.1.3 为 {JAVA, CALLING_CONV, CONSTSAFE, ENSURE_PREPARED_TO_MOCK, UNKNOWN}，其他版本以 P0 差异表为准。
     - 删除通用注解检查器里对 common/specific 声明的抑制。
     - 分层参照 Kotlin 的 `AbstractExpectActualAnnotationMatchChecker` 和 `FirActualAnnotationsMatchExpectChecker`。
  4. **修饰符（E4、E14）**
     - 冲突查询按修饰符目标（声明种类、位置、所在容器）分表，对位官方的 `SCOPE_MODIFIER_RULES`，其中包括主构造和次构造两张表。
     - 删除 extend 特判和主构造特判。
     - 登记与 Kotlin 全局冲突表之间的差异。
  5. **成员属性检查（E1、E10、E13）**
     - 把官方 `CheckCJMPAttributesForPropAndFuncDeclInClass` 的完整条件移植到一个统一的 class 成员属性检查处，包括：
       - 同时覆盖函数和属性；
       - `isCJMP` 豁免；
       - FROM_COMMON_PART 豁免；
       - EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY 同时适用于 common 和 specific。
     - 隐式 abstract 的例外（`CanBeAbstract`、`CheckClassLike{Func,Prop}Abstractness`）收敛成一个共享判定，并补上属性分支。
     - 删除 `checkCJMPAbstractClassMembers`、class 级的 OPEN_ABSTRACT 诊断、COMMON_OPEN_CLASS_NO_INIT 的非官方报告点，以及相关死代码。
  6. **赋值（E5）**：把 common let 的规则并入 `CfirAssignmentLegalityChecker` 和自增自减检查，删除 `CfirCommonCtorImmutableAssignChecker`。
  7. **其他（E6、E8、E9、E11、E14）**
     - 解析规则复用 `cjmpHasCommonDefault`。
     - 泛型约束比较以存储中的映射作为基础替换器。
     - extend 判重复用 `CfirRedeclarationPresenter` 的签名表示，并按类型同一性比较。
     - property 的 MUT 差异按官方的诊断和比较顺序处理。
     - file part 的判定只保留一份。
     - 诊断参数对齐官方。
     - 门禁收口到 `CjmpGate`。
     - `CfirCjmpCommonSideFacts` 改为接口，文案格式化移回诊断渲染器。
- **验收**
  - E1–E4 的复现 fixture 转绿。
  - 按新实现更新原计划 §9.8 的诊断工厂逐条对照表。
  - 注解和修饰符相关的探针结论（E2、E4）全部能够复现。

#### P7　生产 LL/IDE 入口（G1–G9）

- **改动**
  1. **模式来源（G1）**
     - 在 `CaModule`（或对应的平台接口）上暴露模块的 CJMP 角色。
     - LL session 工厂建 session 时注册 `CfirCjmpSettingsComponent`，角色判定规则：
       - `directDependsOnDependencies` 非空 → SPECIFIC；
       - 有实现模块或 `isCommon()` 为真 → COMMON；
       - dangling 模块继承 context 模块的角色。
     - standalone、LSP、IDE 的 project structure provider 负责写入 dependsOn 边。
     - 测试基建改为读取 `// CJMP_MODE` 指令，并走同一个入口。
     - `AbstractCfirCjmpMatchingTest` 删除按模块名、声明名分支的写法，断言改为"存储 dump + golden 文件"。
  2. **阶段门（G2）**
     - 模式在建 session 时固定之后，`LLCfirCjmpMatchingLazyResolver` 始终推进阶段，只门控 transform（对位 Kotlin `LLFirExpectActualMatcherLazyResolver.kt:46,65-69`）。
     - 删除 `bodyResolvePrerequisitePhase`、`shouldCheckIsResolved`，以及分散在各文件中的相关特判。
  3. **模块归属（G3）**：standalone 和 LSP 的 `getModule` 按 content scope 查找文件所属模块，use-site 模块只用于多个候选之间的消歧（对位 Kotlin `KotlinStandaloneProjectStructureProvider.kt:50-80`）。
  4. **extend provider（G4）**：LL 与 eager 使用同一个依赖集合公式 `dependencies + allRefinementDependencies`。
  5. **TYPES 配置（G5）**
     - `CfirTypeResolveTransformer` 对外暴露成员作用域入口，`LLCfirTypeLazyResolver` 改为调用它。
     - 删除 `buildConfiguration` 副本，以及 `constructorOwnerForTypeResolution` 中按 ClassId 反查的逻辑。
  6. **first-fit 夹具（G6）**
     - `firstFitCandidateDiagnostic.cj` 和 `cjmpFirstFitCandidateDiagnostic.cj` 中的单模块 first-fit 场景，改成官方探针的形状：单个模块内两个同 key 的 common extend。
     - 多 common parent 按 P0 差异表的语言版本门禁，分版本编写 fixture：
       - 只支持单个 common part 的版本，按官方行为处理这类配置；
       - 支持多 parent 的版本，每个 parent 各自绑定。
  7. **块内修改（G7）**：`isReanalyzableContainer` 改用仓颉的规则：省略返回类型的函数不作为可块内重分析的容器；或者在函数体修改后，让签名和配对结果失效。
  8. **测试顺序（G8）**：恢复与 Kotlin 一致的求值顺序；如果因此出现失败，就修复预热中的顺序依赖。
  9. **按包枚举文件（G9）**：在 declaration provider 上增加一个"按包列出全部文件"的接口。
- **验收**
  - standalone 或 LSP 下的 CJMP 工程（一个 common 模块加一个 specific 模块）：
    - 不再报 `PARSE_*_IN_NON_*_FILE`；
    - 能够完成配对；
    - 为此新增 standalone 集成测试。
  - LL 的 CJMP 测试改由指令驱动后全部通过。
  - LL 下 `func …: This` 和类体遮蔽的用例通过。
  - 跨源码模块使用 extend 的用例，LL 与 eager 结果一致。

#### P8　序列化、CLI 与诊断通道（H1–H14）

- **改动**
  1. **默认上界（H1）**
     - 写侧跳过隐式的默认上界，只写显式的 where 约束（对位官方 `SaveGeneric`，以及 Kotlin `FirElementSerializer.kt:968-969`）。
     - 读侧用 `addDefaultBoundIfNecessary` 补回默认上界。
     - 读写两端都删除 `_CNat3AnyE`。
     - 同时删除两项测试断言：`CjoFullIdResolverTest` 中针对硬编码表的断言；`CjmpTwoPhaseCompilationTest` 中"没有 std.core 时 Any 也能解析"这一验收项。
  2. **FullId 键（H1）**
     - 跨包 FullId 的键约定只由一个 owner 决定：要么是 MANGLING 阶段产出的 exportId，要么是读写两端对称的键。
     - 没有 std.core cjo 时，标准库声明由 builtins provider 提供。
     - 补一个用例：引用 String/Object 的 CJO 能正确往返。
  3. **common part 加载（H2、H5）**
     - 新增专用的加载入口，只读取配置中指定的文件，并校验文件头里的包名与源码包名一致。
     - 加载结果作为 depends-on 模块的 provider。
     - 不再把 common part 的父目录并入库搜索根。
     - features、options 等门只对经这个入口加载的文件生效。
  4. **CLI 产物（H3）**
     - 在配置阶段，把 `-o`/`--output-dir` 映射为非 cjd 编译的 CJO 输出位置。
     - common 编译模式必须产出 CJO；没有输出位置时报错。
     - 两段式 e2e 改为经 `CangJieCLICompiler.exec` 或 `AbstractFrontendPipeline.execute` 驱动。
  5. **诊断通道（H4、H8、H11）**
     - 反序列化得到的声明，带一个由 CJO 位置构造的二进制 source element。诊断锚在标识符还是声明起点，只在这一处决定。
     - common 方向的诊断由 checker 经 `CfirErrors` 统一报告，删除 `CjmpDeserializedCommonSideReporter`。
     - 加载门的结果作为 session 级事实，由 checker 报告（对位 Kotlin 的 incompatible-class checker）。CLI、IDE、LLT 走同一个通道，并补上 LLT 断言。
  6. **各版本的加载行为（H6）**：按 P0 差异表的语言版本门禁，同时支持各版本的行为。
     - 1.1.x：只接受单个 common part，只校验包名；前端角色由 common CJO 输入决定，驱动的 specific 链接判据来自 inputChirFiles，不能混用。
     - v1.2.0-alpha.20 起：启用 features 门、options 门、多 common part 与专用 commonPartChirs 输入；v1.3.0-alpha.05 起再启用 CJO 格式版本门。每个行为按附录 A.2 的精确引入版本设置 `LanguageFeature`。
     - 门禁判断统一经过一个入口，不在各处直接调用 `supportsFeature`。
     - 门禁测试矩阵按原计划 §8.4 扩展到新增的版本。
  7. **比对输入（H7）**
     - debug、opt 选项从 CLI 参数映射而来。
     - common 和 specific 两侧的 features 都取自 `CfirFile.featuresDirective`。
  8. **specific 输出（H9）**：按探针结论，specific 编译输出时，一并写出 depends-on 模块中未被遮蔽的反序列化声明。这里使用 P3 的合并视图。
  9. **其他（H10、H12、H13、H14）**
     - identifierPos 复用共享的定位策略，并在共享的 LightTree 策略中补上 `OPERATION_NAME`。
     - `ModuleDataProvider` 的排序只在 builder 中保证。
     - 位置解析按 `pkgId` 进行。
     - 删除 `PROBE-EXE` 调试输出。
- **验收**
  - H1–H3 的复现 fixture 转绿。
  - 新增一个不手动设置配置键的"CLI 两段式"e2e。
  - cjc 1.1.3 产出的 common part 可以正常加载，并且不产生虚假警告。
  - specific 输出的 CJO 所含的声明集合，与官方探针产物一致（以 H9 的探针结论为准）。

#### P9　resolve 顺带修复（I1–I5）

- **改动**
  - **I1**：错误过滤和尾部传播改用 `containsErrorType()`；评估是否改为在表达式节点上执行 ReplaceIdealTy。
  - **I2**：包限定符解析一次性给出结果，要么是唯一的包，要么是包名歧义（对位 Kotlin `FirQualifiedNameResolver`）。删除回滚逻辑。
  - **I3**：删除 `CjBinarySourceElement` 分支，测试改为直接构造已解析的类型。
  - **I4**：import binding 按文件所属的 owner session 存取，或者直接挂在 CfirFile 上；删除 LL 里的补录。
  - **I5**：删除 `coneTypeSafe` 中的死分支。
- **验收**
  - 以下用例保持绿：`Import00.testErrAmbiguous00/01`、`bottomType03.cj`、`binary_error_report_09.cj`、`expose4.cj`、`upper_bound_{int,float}_binary.cj`。
  - 新增一个"显式 return 带嵌套错误类型"的用例。

#### P10　收口

- **回归**
  - 全量跑一次 `:cfir:analysis-tests:test`，生成新的快照，沿用 testcase-key ledger 格式。
  - 与 v10 快照比对：除各阶段有意修正的期望外，不允许有回归。
  - 以下测试任务全部通过：`:analysis:low-level-api-cfir:test`、`:compiler:frontend:test`、`:cfir:cfir-serialization:test`、`:compiler:cli:test`、`:resolution.common:test`。
- **文档**
  - 更新原实施计划中的以下章节：§2 架构对位矩阵、§3 语义裁决点、§9.8 诊断工厂逐条对照、§10 各 Phase 表。
  - 在本文件中，为每个已修复的条目标注"已修复"及对应的提交号。
- **复审**：Kotlin parity 复审重点覆盖以下内容：
  - 合并视图；
  - actualizing scope；
  - CJMP_MATCHING 的阶段位置；
  - 声明级 override 图；
  - LL 模式的来源。

### 10. 验证策略

- **gradle 统一走共享队列，并在后台运行**：

  ```
  java -jar gradle-queue-cli/build/libs/gradle-queue-cli.jar --project-dir <仓库根> <任务…> --continue --console=plain
  ```

- **各阶段的定向测试**

  | 范围 | 任务 |
  |---|---|
  | CJMP 的 LLT 切片 | `:cfir:analysis-tests:test --tests '*CommonSpecific*'` |
  | LL 配对 | `:analysis:low-level-api-cfir:test --tests '*CfirCjmpMatchingTestGenerated'` |
  | 两段式编译 | `:compiler:frontend:test --tests '*CjmpTwoPhaseCompilationTest'` |
  | matcher 单测 | `:resolution.common:test --tests '*AbstractCjmpMatcherTest'` |
  | 序列化与 CLI | `:cfir:cfir-serialization:test`、`:compiler:cli:test` |
  | 泛型实例化与 extend | `:cfir:analysis-tests:test`，配合 `--tests` 过滤 `privateExtendOwnerGroups`、`defaultImplement19`、`interfaceDefaultImplementedFuncGenericInvalid2`、`privateFunctionsCauseGenericInstantiationAmbiguity`、`Import00.testErrAmbiguous0*` |

- **全量回归**：跑 `:cfir:analysis-tests:test`，按 testcase-key 生成快照，与 v10 对比。PSI 和 LightTree 的结果分开统计，这是 skill 的要求。
- **证据留存**
  - cjc 探针的源码和输出存放在 `.workbuddy/tmp/cjmp_probe113/review-20260928/`。
  - 每一处语义改动，都在提交说明中引用对应的探针结果或官方源码行。

### 11. 执行与协作

- **执行方式**
  - 2026-09-28 用户确认：目前没有其他 agent 在做 CJMP 相关的工作。
  - 但同日 18:40 观察到，工作树里仍有 import 分组相关的改动正在进行（codex 进程在运行，工作树共 192 处改动）。其中 `CfirCompositeSymbolProvider.kt`、`PsiRawCfirBuilder.kt`、`LightTreeRawCfirDeclarationBuilder.kt` 等文件与本计划重叠。
  - 在提交或修改源码之前，需要先确认这部分工作的状态，避免把它的半成品混进 CJMP 的提交。
  - 本文件是 CJMP 框架级修复的唯一跟踪处，统一用条目编号（A1–J1）和阶段编号（P0–P10）跟踪进度。
- **开工前先处理未提交修改**（§4、§12 决策 2）
  1. 先验证当前工作树：跑定向测试，再跑全量 LLT。
  2. 验证通过后分批提交。
  3. D1–D4、H1 等补丁，按"临时补丁，待 P4/P8 替换"的说明提交，并在 §4 登记。
- **原计划的处理**：原计划正文不改，只在原计划 §11 末尾加一行，指向本文件。
- **进度标注**：每个阶段完成后，在本文件对应的条目上标注"已修复"和提交号。

### 12. 决策记录（2026-09-28）

1. **语义基线**
   - **决策**（用户决定）：不以某个特定版本为准，而是覆盖全部官方版本，用语言版本门禁控制。
   - **影响**
     - §7 原则 2 相应调整。
     - H6、E2、E7、G6、F4、F5 等跨版本差异，统一按"确定引入版本 → 定义 `LanguageFeature` → 门禁选择行为"来实现。
     - P0 新增跨版本差异表。
     - 原计划 §7 风险 3 中"以 v1.1.3 为准"的表述，被本决策取代。
2. **未提交补丁**（用户接受）：作为临时补丁提交，在提交说明和本文件中登记，由 P4、P8 负责替换。
3. **合并视图**（用户接受）：接受在 providers 层引入 CJMP 合并 scope，作为相对 Kotlin 的有意结构偏离登记。
4. **A3 的方案**：用户未另行指定，按建议采用"第二次绑定由 checker 推导"。如需调整，在 P1 开始前提出。
5. **协作**：目前没有其他 agent 在工作，不需要额外协调（见 §11）。

### 13. 执行记录（2026-09-29）

- **状态**：P0 取证补全和开工基线验证中；P1–P10 尚未验收。附录原有探针与草稿不等于复现 fixture 已执行。
- **协作复核**：`补全审查导入 PSI 方案` 已完成、当前为 idle；其 import、Analysis API、stub、IDE 等未提交改动保留。CJMP 基线提交仅选择已审计的文件或 hunk，不把共享文件中的其它改动一并提交。
- **新鲜定向基线**：执行共享队列中的 matcher、CommonSpecific、LL 配对与两段式编译测试，`BUILD SUCCESSFUL`。XML：matcher 7/7、LL 配对 7/7、两段式 17/17；CommonSpecific 234 个 testcase-key，其中 159 passed、75 skipped、0 failed。
- **全量基线**：`gradlew-queue.bat :cfir:analysis-tests:test --continue --console=plain --max-workers=1` 已 `BUILD SUCCESSFUL`（14m05s）。新快照 `cfir/analysis-tests/build/ffi-annotation-verification/20260929-cjmp-framework-baseline`：1,265 XML、8,930 testcase-key，8,543 passed、387 skipped、0 failed；相对 v10 为 0 regressed、0 new、0 removed。PSI 与 LightTree LLT 各 3,410 passed、0 failed、0 skipped。
- **P0 版本表复核**：已按全部本地 46 个 tag 的提交、日期及源码更正附录 A.2。旧 checkout/main 混淆、CJO 格式版本门被误判死诊断等结论撤回；早期预发布仍有未细化的语义变化，不宣称 P0 全部完成。
- **P0 复现输入**：`P0-HIGH-SEVERITY-FIXTURE-INDEX.md` 覆盖 22 个高条目，新增 40 份仓颉 scratch 源码并复用已有探针；G1/H2/H3 宿主 harness、H1 wire 断言、F2 LL pattern 验证和部分新负例 JSON 位置仍待补齐。
- **P1 精确实测**：`p1-cjc-20260929/` 保存 cjc 1.1.3 的 12 组完整命令、退出码和 JSON。use3/use4 零诊断；空体 Unit 在配对时判断，非空体 Quest 延后；配对前显式错误类型与配对后 body 错误分别产生 NOT_MATCHED 和 RETURN_TYPE_INCOMPATIBLE，不可混淆。
- **基线提交**：`465b87d13`、`3701bf948`，在 `codex/cjmp-framework-review-fixes-20260929` 分支保存已验证原有 CJMP 修改；临时补丁责任清单见 §4。
- **P1 框架复核**：除了阶段位置，还必须一并处理 matcher 内的 IMPLICIT_TYPES 请求、声明结果属性、第二绑定诊断聚合、LL 固定模式和关闭门时的阶段推进。复现草稿位于 `.workbuddy/tmp/cjmp_probe113/review-20260928/p1-tests/`；LL 草稿首次语义请求为 caller 文件诊断，并检查最终 call 的 specific 符号身份。
- **P1 红测试已复现（尚未修改生产实现）**：正式添加两个 LLT fixture 和两个 LL caller-first 用例。CommonSpecific 切片共 240 testcase-key，PSI/LightTree 的 `testCjmpSpecificShadowingBeforeImplicitTypes` 各因 `AMBIGUOUS_FUNCTION_CALL` 失败；LL 两项分别得到 `CFIR_AMBIGUOUS_FUNCTION_CALL + CFIR_NOT_MATCHED` 和 `CFIR_NOT_MATCHED`，均应无诊断。默认参数纯诊断 fixture 通过不能证明绑定正确，LL 测试另有 specific 符号身份断言。命令与原始失败消息保存于 `p1-tests/red-results-20260929.json`。这些红例随 P1 修复一起提交，不修改期望来消除失败。
- **接续入口**：`P1-FRAMEWORK-MAPPING.md` 已完成 Kotlin 对位及锁链预审；下一步落实其 §8：声明级不可变配对事实、matcher 单目标写入、checker 重放有序候选推导第二绑定、公共 containing-owner 路径、LL 模式装配与锁外 common 签名准备，再迁移全部消费点。P0 宿主复现欠项按各阶段补齐；阶段和总目标均未标记完成。
- **文档校验**：已运行 `gradlew-queue.bat validateDocumentation --console=plain --max-workers=1`。本次 CJMP 文档未报错；聚合任务因既有的两处 CJD `#c5-v3--v31复核报告的处置` 锚点和 module-catalog 缺少 `:compiler:cli`/`:compiler:cli:cli-base` 共 3 条错误失败，尚未宣称文档聚合全绿。

---

## 附录 A：P0 取证结果（2026-09-28，P0 阶段产出）

### A.1 探针结论（cjc 1.1.3 实测）

探针源码、运行日志、CJO 属性位解码脚本与结论明细：
`.workbuddy/tmp/cjmp_probe113/review-20260928/`（`PROBE-FINDINGS.md`、`probe_results.log`、各 `p_*` 目录）。
§6 待探针表逐项结论：

| 条目 | 探针结论 | 对修复阶段的影响 |
|---|---|---|
| B1 | **已实证**：官方 CJO 中 enum 构造器（VarDecl 与 FuncDecl 两形态）均带 COMMON+FROM_COMMON_PART、无 COMMON_WITH_DEFAULT；解析器 `ParseDecl.cpp:1484-1489` 显式传播 | P2：读侧恢复两个独立位即可，候选资格直接来自 COMMON 位 |
| C1 | **已实证**：specific 类省略全部构造器 → 0 诊断；官方管线 = 先合并 → 再 AddDefaultCtor（仅 `!COMMON && !JAVA_MIRROR` 的 CLASS/STRUCT）→ 再配对。**新发现**：官方对缺构造器的 common 类直接报 "at least one constructor is required in common class" | P3：合并视图提供 common 构造器；raw builder 不为 specific 类合成 `init()`；P6 补 common 类缺构造器诊断（E10 关联） |
| C3 | **已实证**：未标记 common 与未标记 specific 同签名成员 → "function 'f' has overload conflicts" | P3：重定义检查跑在合并视图上 |
| D3 | **推翻审查预测**：private 反例只报可见性错误（1 条），**不报** EXTEND_MEMBER_CANNOT_SHADOW；与本仓现状及 `privateFunctionsCauseGenericInstantiationAmbiguity.cj` 期望一致 | P4：private 豁免保留（表述对齐 IsInvisibleMember）；`implementsInterface` 启发式仍按官方 peer 收集规则重审 |
| D4 | **已实证**：bound 满足 → 恰 1 条歧义 + 2 个 found candidate note（锚 CJO 记录的源文件行列）；未满足 → 0 条 | P4：恢复两段式测试原期望；反序列化成员参与实例化成员图 |
| E2 | **已实证**：双侧 @OverflowWrapping 与仅 specific 侧，官方都报注解失配（锚 specific，note 指 common） | P6："common 单侧注解永不报"作废；双向比较 + 版本分类表 |
| E4 | **已实证**：类成员 `common private` 编译通过 | P6：分作用域冲突表 |
| E9 | **已实证**：两个 common extend 的 private 重载 0 重复诊断；附带：extend 不能带 `public` | P6：判重键 = (目标类型, rawMangleName) |
| F2 | 非 cjc 可观测（本仓 LL 行为）；官方只禁 `common` 模式声明已静态确认 | P1/P7 用 LL fixture 验证 |
| F3 | **已实证**：`...`（hasEllipsis）enum + 未配对 specific func → 0 诊断；specific 侧 E 带 COMMON_NON_EXHAUSTIVE | P5：豁免按外层声明判定、覆盖全部成员种类 |
| F5 | **修正审查预期**：`Box<T>` 对 `Box<T,U>` 官方**报错** "type argument of function 'Box' is different in parent class or interfaces"（`sema_generic_member_type_argument_different`，CheckCJMP.cpp:984 的 GenericsCount 检查 + StructInheritanceChecker.cpp:1224/1282 同族）；泛型 vs 非泛型（hasGenericMismatch，:320-323）才静默跳过合并 | P5：两个判据分开实现 |
| G8 | 非 cjc 可观测（本仓测试求值顺序） | P7 用测试验证 |
| H9 | **已实证**：specific 编译产出自己的 CJO，**包含** common-only 声明（attrs=COMMON+FROM_COMMON_PART+COMMON_WITH_DEFAULT）。注意：specific 编译会把同目录同包名 `<pkg>.cjo` 覆盖掉 | P8：H9 方案成立；两段式测试基建须分离输出目录 |

### A.2 跨版本差异表与 LanguageFeature 草案（2026-09-29 复核更正）

**本表替代 2026-09-28 的版本表。** external 工作树 HEAD 实际为 v1.0.0；此前 main 列不是从 `origin/main` 取证，关于“main 回退单 common”和“版本诊断全版本未引用”的结论作废。

本次读取全部本地 **46 个 tag** 和固定的 `origin/main=799e9f6545cc8a83355c5d77e777f8f571215815`（2026-09-22）。完整 tag/提交/日期矩阵、`tag --contains` 原始清单和行号位于：
`.workbuddy/tmp/cjmp_probe113/review-20260928/VERSION-EVIDENCE-20260929.md`。此次没有 fetch；“全部”限定为该本地参考仓库现有官方 tags。

| 行为 | 官方版本边界和证据 | 门禁设计 |
|---|---|---|
| CJMP 与关键词 | 早期 1.1 预发布使用 common/platform；`acd64576b0653987272f458111475d1321e0fcc0`（2026-02-19）改为 specific，最早包含该源码形态的本地 tag 为 v1.1.0-beta.20 | 稳定 1.1.0 起沿用 `CommonSpecificDeclarations`；早期预发布的关键词、泛型和后置检查必须分别建模，不能宣称与正式版等同 |
| 多 common parent | v1.2.0-alpha.19 尚无；alpha.20 起 `commonPartCjos` 与 `severalParents` 生效，所有后续本地 beta/1.3 alpha/main 均保留 | `CjmpMultipleCommonParents`；精确引入点 alpha.20 |
| common part features 门 | 同上，从 alpha.20 的 `ASTLoaderCJMP.cpp` 引入；main 仍存在 | `CjmpCommonPartFeatureGate` |
| common part options 门 | 同上，从 alpha.20 引入 `CompilationOptions` 校验 | `CjmpCommonPartOptionsGate` |
| Specific 判据 | 1.1.x 前端 parser/matcher 看 `commonPartCjo`，驱动看 `inputChirFiles`；alpha.20 起前端看 `commonPartCjos`，驱动看专用 `commonPartChirs` | 区分前端角色与驱动链接输入；`CjmpDedicatedCommonPartChirInputs` 控制专用输入来源，禁止笼统实施“CJO → CHIR 模式切换” |
| UNSUPPORTED 注解集 | alpha.20 在原集增加 FOREIGN_GETTER_NAME / FOREIGN_SETTER_NAME / NON_PRODUCT；后续一直保留 | `CjmpUnsupportedAnnotationsExpanded` |
| ObjC 属性位注解表 | alpha.20 增加 OBJ_C_INIT / OBJ_C_OPTIONAL；后续一直保留 | `CjmpObjCAnnotationAttributeBits` |
| common/specific 同文件规则 | alpha.20 起两类修饰符均接受任一 CJMP 编译角色（`ParseCJMPDecl.cpp:107–130`）；1.1.x 分角色检查 | `CjmpMixedCommonSpecificDeclarations` |
| JAVA_MIRROR 注解表示 | v1.3.0-alpha.02 起从 NonSerializedAnnotations 位表删除 JAVA_MIRROR；提交 `1b67893f23c3ed54c60cf2fa36a4d5893fc3232d`（作者 2026-07-27、提交 07-28） | `CjmpJavaMirrorAnnotationMetadata` 草案；须连同官方 Java 属性模型迁移实现，不能解释成免检 |
| CJO 格式版本门 | v1.3.0-alpha.04 尚无，alpha.05 起 common 加载调用 `CheckCjoVersion`；提交 `12883f2bd87561ccebea71fa5b01f7ea2a7ddde6`（作者 2026-09-03、提交 09-16） | `CjmpCommonPartFormatVersionGate`；比较格式 major 相同、producer minor ≤ consumer minor，忽略 patch；不是 cjc 版本字符串相等 |
| nominal 泛型数量 N≠M | 正式 1.1.0–1.1.3 已报告 `sema_generic_member_type_argument_different`；由声明泛型约束检查处理 | 不新增版本门；合并只区分 generic/non-generic，数量差异不能改写成 nominal 配对失败 |

alpha.20 同批行为来自 `7a9258ae8e69f49d234f971ca8e7dd117d1c3470`（2026-05-25）；alpha.20 指向提交为 `4a3970486e6ff08ab35b4bf06e1b2e77e248f571`（2026-06-05）。提交日期不等于发行日期，`tag --contains` 也不能替代各 release 分支的源码核对。

**版本模型约束**：现有 `LanguageVersion.versionString/parse` 丢弃或不接受预发布标签；仅补一个 `CANGJIE_1_2_0` 会混淆 alpha.19/alpha.20，仅补 `CANGJIE_1_3_0` 会混淆 alpha.04/alpha.05。实现需先支持精确预发布版本身份与顺序，再把上述门接入统一 CJMP 门禁 owner。未查明引入提交的早期 generic/default/后置检查变化继续列为 P0 欠项，不用已推翻的旧表补猜。

### A.3 P0 复现 fixture 清单（P0 任务 4）

P0-1 的探针源码即为各条目复现 fixture 的官方行为依据，转换清单（P0 验收项，随对应阶段提交）：

- A1：`use3()/use4()` e2e（调用方省略返回类型）→ P1
- A2：LL 跨文件诊断用例 → P1
- B1：两段式 common 源加未标记 `helper` + 断言 `messages`/`completed`；`specific func g()` 对未标记 `func g()` → P2
- C1：`Repo().extra()`；specific 类省略构造器（=p_c1）→ P3
- C2：未配对子类型 `D<Int64>` + CLI 两段式 → P3
- C3：未标记同签名成员（=p_c3）→ P3
- C4：函数引用 `apply(platform)` → P3
- C5：隐式 `super()` + common 默认参数 → P3
- D1：public owner 成员、有界 owner 接口 → P4
- D2：`Holder<Int64>` 具体实例化 → P4
- D4：ownermatrixlib（=p_d4，含 sat/unsat 两个变体）→ P4
- E1–E4：各自最小复现（=p_e4 等）→ P6
- F1/F2：函数/元组形参配对 → P5
- G1：standalone CJMP 工程 → P7
- H1–H3：CJO 往返与 CLI 两段式 → P8
