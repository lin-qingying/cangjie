# CJMP 家族读取迁移台账（2026-10）

本文件跟踪 [主计划](cjmp-framework-review-and-fix-plan-20260928.md) 中"迁移全部消费者、删除旧可变存储/写 API"
的进度。每一行都必须能由代码位置与实测验证复现。

## 已完成（已提交并验证）

| 消费者 | 读取方式 | 提交 |
|---|---|---|
| `CfirCommonSpecificChecker`（common 方向 + 全部私有辅助） | ✅ 全部经 `CfirCjmpFamilySnapshot`，**零 `storage.*`** | `5ef6ec701` |
| `CfirCjmpCommonSideFacts` | ✅ 收 `CfirCjmpFamilySnapshot` | `ca88fa548` |
| `CjmpDeserializedCommonSideReporter`（CLI specific 编译） | ✅ 经快照 + `cjmpSpecificDeclarationsInPackage` | `ca88fa548` |
| `CfirActualizingScope` → `getMatchedCommonForSpecific` | ✅ 已是规范入口：自带 `lazyResolveToPhase(CJMP_MATCHING)`，返回**全部** counterpart 符号 | 无需改动 |
| `TowerLevelHandler` / `CfirTowerResolver` | ✅ 经 scope 间接消费，无直读 | 无需改动 |

> `TowerLevelHandler` 本身不读配对存储：`ScopeBasedTowerLevel` 包装 `CfirActualizingScope`
> （对位 Kotlin `FirActualizingScope`），遮蔽由 scope 层完成，即计划 C4 的框架级替代形态。

## 阻塞（有实测证据，不能直接迁）

`isCjmpShadowedCommonDeclaration` 的调用点：**在作用域/实例化解析过程中**被调用，
若在此触发 `ensureFamilyComplete`，会把按需推进提前到解析尚未收敛的时刻。

实测回归（把该谓词改为**直接触发家族完备**，其余不变）：

- `testCjmpMultipleCommonExtends` → 新增 `GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS`、
  `RETURN_TYPE_INVARIANCE`
- `testCjmpGenericOuterTypeParameterMembers` → 同上

### 已落地的解除机制（`853f5acd9`）

`CfirDeclaration.settledCjmpCommonShadowed`（声明数据键）：

- `null` = 家族尚未定案 → 读取方回落配对存储当前态（**不触发完备**）；
- `true` / `false` = 定案后纯查表结论，任何阶段读取都安全。

发布点：`CfirCjmpFamilyAggregate.ensureFamilyComplete` 的实现在返回快照前调用
`publishSettledShadows`（LL 与批处理均已接线）。因此该谓词现在**先查已定案事实**，
只有未定案时才回落直读——解析过程中依旧不触发完备。

受影响、**仍待迁移**的调用点（迁移方式：改为只消费已定案事实，或保持现状均可，
因为回落路径已保证语义不变）：

| 调用点 | 位置 |
|---|---|
| 泛型实例化检查器（5 处） | `CfirGenericInstantiationChecker.kt:999,1007,1466,1490,1508` |
| 继承深度检查器（2 处） | `CfirInheritanceDeepChecker.kt:1783,1862` |
| extend 成员 scope（1 处） | `cfir/providers/.../CfirExtendMemberScope.kt:198` |

> 这些调用点目前**已经**通过谓词间接受益于定案事实，无需逐个改动即可获得
> "定案后纯查表"的行为。把它们改成语义更明确的直读入口属于收敛性整理，
> 优先级低于"删除旧写 API"。

**仍缺的构件**：写入侧仍需可变存储。真正的最终形态是主计划 §8 item 2/3——
声明级**不可变 plan**（由求值器产出、缓存）替换 `CfirCjmpMappingStorage`
的正反向 `ConcurrentHashMap` 与 `bind`/`record*`/`clear` 写 API。

## 尚未迁移（写入侧，属"删除旧存储"范畴）

`CfirCjmpMappingStorage` 的写 API（`bind` / `recordMismatch` / `recordUnmatched` /
`recordParameterMismatch` / `addCommonExtendCounterpart` / `clear`）与直读它们的运行时：

- `CfirCjmpMatchRunner`（写入核心）
- `CfirCjmpMatchingProcessor`（`beforePhase` 的 `clear()`）
- `LLCfirCjmpMatchingLazyResolver`
- `CfirCjmpResolver`（候选查找的 `commonCounterpartsFor`）
- `LLCfirCjmpFamilyAggregate` / `CfirCjmpBatchFamilyAggregate`（快照构造，预期保留）

这些要等"声明级不可变 plan 发布 + 求值器"落地后整体替换，不能逐点删。

### 生产接线的前置条件

以下保留早期接线的演进记录；2026-10-02 已将函数与 enum 改为真实 callable 同名组，
使用 `buildCallableMatchPlan`。当前 mixed 接线仍待完整回归验收。

1. **资格判定的唯一来源（已处理，`3d695e35a`）**。共享 matcher 的 `AbstractCjmpMatcher.match`
   同时做签名与参数检查；plan 走 `matchSignature` + 求值器的有序参数程序。原先工厂自己再算一次
   `matchSignature`、`CfirCjmpFunctionCandidate` 里又存了一份 `eligibility` —— 同一判定有三个来源。
   早期曾由工厂接收 `candidateEligibility` 独立入参；2026-10-02 复核发现 runner 仍会先调用
   完整 matcher，提前执行参数检查。当前工厂只调用共享 `prepareCandidate`：资格、签名、映射
   在同一入口准备，参数程序由求值器执行；已经移除独立 eligibility 入参与 runner 的重复判定。

2. **首个成功之后的候选处理（已核实无差异）**。现状 `matchAgainstCandidates` 在
   `storage.bind(...)` 成功时 `return common`，不会为后续候选记录 mismatch；
   求值器 `FIRST_SUCCESS` 命中后 `break`，同样不产出后续 `visits`/事件。两侧一致。

3. **参数程序的停止边界（2026-10-02 官方复核更正）**。
   官方 v1.1.3 `CheckCJMP.cpp:923–936` 在类型签名失败时直接返回，不执行 named/default 程序。
   本台账此前将“失败签名仍应报告参数错误”称作修复，是错误解释，现明确撤回。
   参数事实可以在计划中预先建模，但求值器只在签名成功后执行；结构资格拒绝时甚至不读取参数。
   成功签名内按位置执行名字、双方 HAS_INITIAL 冲突、真实 helper 来源转移。
   后参数失败保留已转移前缀，后续候选成功保留早期已发生的诊断，不能反推失败签名也执行参数程序。
   当前共享求值器与真实 callable 工厂已遵循此顺序；默认前缀的运行时发布/消费仍属 P8 欠项。

   > 注意：`p1-first-fit-20260929` §1.2 指出"单 common part 内两个同签名同 key 成员"会被
   > common 编译以 `sema_param_named_mismatched`/`EXTEND_MEMBER_CANNOT_SHADOW` 拒绝。
   > 但 `firstFitCandidateDiagnostic` 夹具用的是**两个独立 common 模块**（`()()(common1 common2)`），
   > 即两个 source-set，各自合法——该场景是可达的，不是非法程序的产物。

4. **组聚合已落地（本轮）**。前一轮的成员路径回归根因是
   `buildFunctionMatchPlan` 只对传入的**单个** specific 建模，而占用状态
   （官方 `common.specificImplementation`）在整个包内共享——逐个 specific 独立求值无法表达
   "首个成功者绑定、其余报 SECOND_BINDING"。

   已实现 `CfirCjmpMatchRunner.matchFunctionGroupWithPlan`：按
   `findSpecificDeclarationsInMatchOrder` 取同名 specific 组，为**每个**成员建 plan 后
   **一次**交给 `CjmpMatchEvaluator.evaluate`，再经 `writePlanAggregate` 翻译回存储。

   翻译阶段的三条不变量（均由测试固定）：
   - 配对统一经 `storage.bind`：既建正向映射，也把**被拒绝的第二绑定**留在反向表里
     （`MULTIPLE_COMMON_IMPLEMENTATIONS` 判据依赖它），并由 `bind` 自行登记 `SECOND_BINDING`；
   - 当前 `Occupied` 事件通过 `recordOccupied` 保留真实 common/前一 specific 与被拒 specific；
     2026-10-02 后续修正由 `recordOccupied` 同次发布反向拒绝索引，删除事件翻译中的再次 `bind`。
     后续候选成功不清除该事件，历史事件不重写成功后的最终失配状态；此修正正在重新验证。
     重复实现消息按失败配对双方的 native kind 判定：双方 enum 为 constructor，mixed FUNC 为 function；
   - 共享 matcher 的上下文以**符号**为键、工厂以**声明**为键，两者各自转换，不能混用。

   验收：`testTopLevelSpecificFirstMatch` / `testParallelTopLevelSpecificFirstMatch` /
   `testMemberFirstMatch` / `testFirstFitCandidateDiagnostic` 全部通过，
   且 :analysis:low-level-api-cfir:test 146/146、:cfir:analysis-tests:test 8963/0/396。

   **后续**：把同一形态推广到 property / variable / enum / pattern 的 plan 工厂，
   然后才能删除 `bind`/`record*`/`clear` 之外的旧写路径。

### plan 路径的声明种类覆盖（逐类推进）

| 声明种类 | 状态 | 说明 |
|---|---|---|
| `CfirNamedFunction` / `CfirConstructor` | ✅ 已接入 | `is CfirFunction` 分支，组内一次性求值（`8170a51d6`） |
| `CfirProperty` | ✅ 已接入 | `buildPropertyMatchPlan` + `matchPropertyGroupWithPlan`（`8fed6ba83`） |
| `CfirVariable`（static/global） | ✅ 已接入 | `buildStaticOrGlobalVariableMatchPlan`，`VariableAssignment(STATIC_OR_GLOBAL)`（`d7bd9c36b`） |
| `CfirVariable`（实例成员） | ⬜ 未接入 | 官方在**容器合并**时配对；需要 `CjmpContainerMergeEvaluator` 的 `initialBindings` 作为求值 seed |
| `CfirEnumConstructor` → enum 构造项 | 专用路径已接入并验证 | 同名组走不可变计划、共享 TryBind 与统一结果发布；mixed 普通函数候选仍须适配，不能计作完整 enum 覆盖 |
| `CfirPatternVariable` | ⬜ 未接入 | 求值器已有 `TuplePattern` / `NonTuplePattern` 分支 |

### 新发现的未建模项（属其他阶段）

- **`doNotExport` 在存储与 CJO 侧完全未建模**：官方在 Match/合并阶段对"被 specific 实现的 common 声明"
  置 `doNotExport = true`，以阻止其被再导出。确切位置：`CheckCJMP.cpp:911`（`TrySetSpecificImpl` 内，
  所有配对共用）、`:293`（合并路径）、`:1077`（`MatchCJMPVar` 的 static/global 分支）；
  另有 `:238` 在合并时把成员重置为 `false`。本仓库除本次新增的注释外没有任何对应概念。
  这属于 P8（CJO 导出）范畴，**不是读取侧问题**，在 plan 覆盖率补齐后应单独处理。

**property 路径的两个注意点**：

1. 官方 `MatchCJMPProp`（`CheckCJMP.cpp:1001-1010`）对本体与两侧访问器**分别** `TrySetSpecificImpl`。
   求值器的 `requireRootBinding` 只作用于**本体**，因此 getter/setter 可以带自己的 specific/common
   符号——它们在占用表里是三个不同的键，不会互相判成第二绑定。
2. 此前语料里**没有任何 CJMP property 用例**，该路径没有任何测试覆盖。
   已补两处：
   - `cfir/analysis-tests/testData/diagnostics2/common-specific/e2e/cjmpPropertyMember.cj`（e2e，只校验诊断）；
   - `analysis/low-level-api-cfir/testData/cjmpMatching/propertyMemberMatches.cj` + 断言分支
     （断言 `commonFor(specificProperty.getter) === commonProperty.getter`）。

   后者立刻抓出一个真实缺陷：**访问器绑定发生在 property 绑定规则内部，不产生
   `CjmpCandidateVisit`**，而 `writePlanAggregate` 只翻译 visits，因此 getter/setter 从未写入存储。
   已修（`a66a9a07d`）：在 visits 之后按 `aggregate.acceptedBindings` 补齐；
   `storage.bind` 对同一 (specific, common) 对幂等，本体重复写入安全。

   > 一般化教训：**求值器的产出有两类——visits（候选级）与 acceptedBindings
   > （含绑定规则内部的访问器等）。翻译层必须覆盖两者，只翻译 visits 会静默丢绑定。**
   > 后续接入 variable / enum / pattern 时要按此检查。

## 读取模式契约（务必遵守）

见 `CfirCjmpFamilyAggregate` 接口 KDoc：**阶段完备读取**（checker 阶段）与
**解析中当前态读取**（resolver/scope/实例化阶段）必须严格区分。

## 验证入口与最近一次结果

| 入口 | 覆盖 | 最近结果 |
|---|---|---|
| `:analysis:low-level-api-cfir:test` | LL 按需解析（含 CJMP 匹配/诊断/家族完备用例） | 完整 159 项 0 失败 |
| `:cfir:analysis-tests:test` | CFIR eager 全量（含 common-specific e2e 语料） | 8966 项 0 失败 397 跳过 |
| `:compiler:frontend:test --tests '*CjmpTwoPhaseCompilationTest*'` | **CLI 两段式入口**（common part 产出 → specific 编译加载） | 19 项 0 失败 |
| `:cfir:resolve:test` | resolve 单元（含 CJMP 规划与候选测试） | 148 项 5 失败，均为**干净 HEAD 基线同样存在**的既有失败（已用独立基线工作树逐项对照） |

两段式在 plan 路径接入（函数/构造器、property、static-global 变量）**之后**复跑通过，
说明接线没有改变 CLI 侧行为。

### 2026-10-02 enum 专用接线后的新鲜验证

- LL 完整 150/150；CFIR XML 8966 项：8569 passed、397 skipped、0 failed。组合命令 BUILD SUCCESSFUL（31m24s），未用 build cache。
- 快照 `20261002-enum-plan-full` 对比首轮 `20261002-enum-plan-first-full`：2 fixed、0 regressed、0 new、0 removed。
- enum 计划新单测 6/6，原函数计划与返回状态各 4/4；CJMP 两段式 19/19。
- 三个被接线暴露的 owner 问题已修：复合类型映射统一交替换器与类型系统；LL enum BODY 使用真实 transformer/StateKeeper；LL callable TYPES 使用 designation 的外围容器，不能把当前 enum 构造项当 owner。
- 混合候选仍需继续：官方合法的普通 common 函数 `Value(x:Int64):E { A }` 可由 specific payload variant `Value(Int64)` 对应。探针见 `.workbuddy/tmp/cjmp_probe113/review-20261002/enum-plan-main/README.md`，不能保留 only-enum 限制作为最终语义。

### 2026-10-02 mixed 接线的当前验收状态

- 最后一轮组合 BUILD SUCCESSFUL（21m46s，禁用 build cache）：resolve 定向 49/49、CLI 19/19、
  完整 LL 159/159、完整 CFIR 8966 项（8569 passed、397 skipped、0 failed）。共享算法 57/57
  在此前源码相同的共享层定向轮已通过。
- CFIR 权威快照 `20261002-mixed-callable-final-full`，相对 `20261002-enum-plan-full`：
  0 regressed、0 new、0 removed，失败集合为空。末轮 XML 时间为 2026-10-02T14:55–15:13Z。
- 发布器修正：visits 保存最终失配状态，events 保存真实缺实现/占用声明锚点；
  参数失败不再经 events 重录，历史事件不覆盖后来成功的最终状态。
  provider/namespace 保留全部结构候选，仅按真实 ClassId 去除 builtin 重复，类型选择移到
  `CfirTypeCandidateCollector` 的确定 scope 层，简单类型解析直接消费该层集合，构造调用复用该入口。
  新增九项类型候选回归；resolver 与 CLI reporter 的依赖/同模块筛选改用 moduleData 身份/相等性。
- 构造 classifier 使用 originalScope 保留查找来源；visits 只写
  mismatch state，真实 MissingImplementation 事件单独保存准确声明锚点，不从 property root
  的返回值伪造 MissingBody。CLI reporter 枚举全部 classifier 结构候选。
  两项同名模块公开 runner 回归验证真实 refinement 过滤和本模块匹配组归属。
- 首轮完整 LL 的 firstFitCandidateDiagnostic 失败已定位并修复重复失败写入；
  functionMatchesCommonVariant 末轮完整通过，但未做单因子隔离，不给其首轮失败编造独立根因。
- 本批 mixed callable、类型查找与诊断入口修正已通过完整回归和职责复审。
  声明属性、纯组缓存、真实 merge seed、pattern、旧写 API 删除及
  默认前缀消费继续是主计划欠项。
- 下一批完整调用面见 `docs/cjmp-immutable-publication-migration-20261002.md`；该文档为取证迁移清单，
  不把旧 storage 的本轮发布器修正当作不可变架构完成。

### 2026-10-03 pattern 与声明事务：定向验证，完整回归待完成

- 工作树 HEAD 仍为 `fcfeede71`；以下为未提交修改，不代表九步不可变发布迁移完成。
- pattern 计划使用原有 binding symbol；tuple 只执行直接 VAR 前缀，非变量停止，先前接受项保留。
  普通 binding root 只记录根的处理结果，不能把根当作真实变量写入 counterpart。
- LL binding 首次请求通过 provider 回到真实 pattern owner；TYPES/STATUS 处理所有原有子声明头，
  IMPLICIT 不能因 root 显式类型而跳过仍为隐式类型的 enum binding。
- 源码 LL provider 原来只枚举顶层 callable 外壳，文件声明 provider 也不索引 pattern binding。
  两个共享入口都已补齐真实绑定名称与 symbol；不能在 CJMP resolver 另建候选兜底。
- TYPES/BODY keeper 保存原有 pattern/binding 身份；共享解析快照保存 explicit receiver、原始实参列表、
  resolved/error 参数映射及 context 独占表达式。取消恢复测试包含两次取消和成功重试。
- 2026-10-03T04:47Z 的 resolve 定向 XML 为 **63/63**（含 pattern 8、快照 6），作业 exit 0。
  2026-10-03T04:45–04:46Z 的 LL 定向 XML 为 **29/29**（pattern 7、enum 3、mixed 9、
  pattern keeper 2、既有 keeper 8）。组合轮 exit 1 的原因是随后已修复并单独验证的测试泛型声明错误，
  不能把组合轮本身写成成功。
- 新官方 1.1.3 取证在 `review-20261003/explicit-tuple-header` 与 `enum-binding-header`，
  两组有效命令均 exit 0；LL 阶段与对象身份由测试证明，不由官方编译成功推断。
- 完整 LL + CFIR + CLI 组合已 exit 1（25m34s）；LL 168/168、CLI 19/19。
  CFIR XML 8966 条：8561 passed、8 failed、397 skipped；相对 mixed 基线 8 regressions、0 new/removed。
  gate 最新复审因代理额度错误未返回，剩余不可变发布协议亦未闭合。
- 完整 LL 已返回新鲜 XML **168/168**（2026-10-03T04:48:45–04:50:11Z），无失败/错误/跳过。
  CFIR 最终失败为 var/let、let 类型差异、CPointer 泛型与 foreign 变量各 PSI/light-tree 两项。
  官方允许配对后报告对应诊断，现有 fixture 已要求该诊断；先核对 native 声明头/源码范围，
  不能因真实 binding 接线而直接改变 fixture 或退回根 symbol 配对。
- TYPES 已保留派生 binding 原 type ref source，消除伪造绑定名类型语法的重复报错。
  `7e4438d6` / handle `72972` 已 exit 1，5m1s；完整 FFI 切片 82 项中只有 foreign 两失败。
  同轮 LL pattern 新鲜 XML 7/7，0 failed/error/skipped。
  该切片不能作为完整 CFIR 复验；普通 VAR 声明头与显式 @C 请求归属继续待修。

### 2026-10-03 声明头元数据与真实 binding 键

- provider getDeclarationMetadataOwner 复用已有归属索引；不复制注解、重建声明或更换匹配 symbol。
- CJMP 普通 VAR 的诊断读取原声明范围；tuple 子变量仍使用自身范围；注解比较读取原声明头。
- foreign checker 使用 header.interopInfo.abiRequest.hasExplicitC，排除外壳重复检查，保留真实 binding 报告。
- 首轮 queue f5533245 / handle 19806 exit 0（8m54s）：CFIR 切片 261 passed /85 skipped /0 failed，
  LL pattern 7/7。官方显式 @C foreign var 探针只报注解目标非法。
- 随后补 reportUnmatched 的普通 VAR 范围和新 fixture foreignVariableExplicitC.cj；
  queue f26aa19b / handle 40102 已 exit 0（6m10s）：最终 CommonSpecific/FFI 263 passed /85 skipped /0 failed，
  完整 LL 168/168、CLI 19/19，新 fixture 两入口通过。
  完整回归 queue dfe213ff / handle 33883 已 exit 0（22m15s）：CFIR 8571 passed /397 skipped /0 failed，
  XML 8968 条。快照 20261003-pattern-header-final-full 对比旧绿色基线 0 regressions、2 new、0 removed；
  先前八项失败逐 key 均 passed，新增 fixture 两入口通过。LL/CLI 同输入 UP-TO-DATE。
  本批完整回归完成；框架 gate 与九步不可变迁移继续待完成。
