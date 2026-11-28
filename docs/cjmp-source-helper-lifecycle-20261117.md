# CJMP SOURCE helper 生命周期与 R5 接续（2026-11-17）

> 最终默认参数/lambda定向集75/75已核验：queue `d6c71a37`，2026-11-17T10:36:07.347Z至10:38:50.700Z，DONE/退出0；复制19、metadata36、生命周期18、原负例2，无失败/错误/跳过。六原XML15,334字节均在窗口，6826输入SHA `BE1412F21B6D7368542D2A6DF00587E82F5A7F5B44DB0F33D8E376641B84CB81` 与事前一致；[终态、逐case、完整压缩XML](cjmp-source-helper-20261117/front-default-final-75-results.json)。旧session54359已不可读取，依据磁盘终态而非重构终端记录；新四late/OBL/PCLA均通过。该轮不包括SOURCE14，也不关闭S1/S2完整、R5或原条目全验收。

> 当前最新目标是[原CJMP方案前端子集](cjmp-frontend-subset-audit-20261117.md)，排除后端，不扩全语言前端。原CJO/SOURCE前端产物保留完整条件，通用R5逐consumer必要性核对。下面S0–S7整链优先级及原91.22是历史，不覆盖新任务排序；default/lambda必要正确性修复继续。

唯一实现树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD 仍为 `b4dd742d589e4b5ee6592d454634c44f634df08a`。主仓库和 external 只读；历史已暂存/未暂存修改全部保留，没有提交、回退或联系停止的会话。原70项、P0–P10完整范围和计分保留。H6排除余额保留分母、不实施；v10待办不阻断授权实现。

## 已接入的 S1 生产生命周期

后续有界复审对receiver/visibility限定接受，但拒绝最初phase发布的候选提交时机及late signature/helper绕过两项。主线程已继续把发布移到实际DEFAULT推断会话、无活动候选、无未完成外层函数的正式根，接Named最终返回类型、Normal lambda results writer、helper及ctor/initializer/codeFragment；当前为新候选编译/复审中，不能用下面14/14证明新提交边界已经通过。

- `providers/resolve/providers/macro/DefaultParameterFunctions.kt` 在最终 construction 图的 metadata seal 前创建独立 helper。两 parser、Identity 和实际 source macro construction 共用；不是 writer 临时 clone。
- helper prototype、前序 fresh 参数、自有泛型/bounds、assignment 和显式 Return 位于同一 owned-copy 域。名称为 `parameter.ordinal`、自有泛型按 `T$0/U$1` 重命名。构造器 helper STATIC，外层泛型借用；前序参数不复制 named/member/default/helper 状态。原 assignment 保留诊断和语法范围。
- `CfirAnnotationMetadataBuilder` 遍历 parameter-owned helper；source provider 登记真实 file/outer，helper 非 physical top-level，不加入 class body/package 名称索引。
- TYPES 在原函数自有泛型 configuration 前处理 helper header；STATUS 包含 helper 专用参数入口；BODY 在真实 lexical outer 建 helper 函数域，完整 enter/visit/exit 后生成真实 CFG。`BodyResolveContext.withNamedFunction.beforeEnter` 在登记原函数后、装入其自身泛型与参数前执行，保留合法递归及默认值的前序参数边界。
- LL 文件初建阶段建立稳定 helper header；`CfirLazyBodiesCalculator` 通过原 owner 的正常 revive/stage/adopt 物化既有 helper body，保持 header/symbol 身份。helper BODY slot 纳入原 publication/keeper，直接 helper 请求转发原 owner 的锁和生命周期。这里已有实际源码接线和编译证据，尚未执行完整 LL runtime/adoption/取消验收。
- 默认参数 checker 已消费函数自身 ABSTRACT/DEFAULT；不再将接口祖先内的合法局部函数误判为 interface DEFAULT。

## 新十项生命周期测试暴露并修复的三个根因

`CfirDeclarationOwnerTest` 新增十个 PSI/LT 成对用例，四个新 fixture 位于 `testData/declarationOwner/defaultHelpersS1/`。复用真实 construction→provider→runResolution，检查身份、绑定、泛型/bounds、receiver、Return target 和 CFG。

首次扩大窗口中的六失败为三个问题的双入口复现，正确 fixture/期望未改：

1. 局部 helper 的 factory PRIVATE 被 STATUS 一律改成 LOCAL。`CfirStatusResolver.resolveVisibility` 现在保留 `Synthetic.DefaultParameterHelper` 的真实生成可见性；局部名称域仍由 isLocal 表达，没有伪造 explicit modifier。
2. 实例 helper 缺少 dispatch receiver。prototype 使用原 owner 已有的真实 receiver；构造器/STATIC 明确无 receiver。该 Cone 经同一个显式 type remapper 处理，外层 class 泛型不复制。
3. 嵌套函数/lambda 已有 typed body、绑定和独立 CFG，却仍为 RAW。`publishCompletedFunctionBodyPhase` 位于普通 `transformFunctionContent` 的实际 BODY 完成点，只在 body/CFG/完整签名存在且不含未完成推断变量时发布 BODY；IMPLICIT_TYPES 临时遍历不发布 BODY。

这些修复已有十四项实际生命周期通过；不据此关闭完整 S1、S2、R5/R6 或 SOURCE 全链。

## 固定源码与官方新 fixture 证据

Kotlin 固定为 `3c7a5041f238102285764d370665f56f587f9dc4`，官方 v113 固定为 `14276740ffec1262b81b6b5236b1d840af270b37`，所有源码考证通过 git show 固定对象。新工作使用新建 Mapper/Authority/TestWriter/Gatekeeper，不读取旧会话作为实现依据。

[官方新 fixture ledger](cjmp-source-helper-20261117/native-new-ten-fixtures-verification.json) 保存 SDK executable/envsetup SHA、实际 `cjc --version`、四次真实命令/进程/终态、可恢复 stdout/stderr、source/CJO/AST SHA 和 sema 取证。实际编译器为 1.1.3 cjnative，四次退出均0、error均0，未删除 unused warning。

官方 replay 的 fixture/test hash 保持不变，旧26份 CJO/a/AST 未覆盖。它以早先 85 输入 manifest 取样，随后观察到三处主线程生产修订造成的聚合 SHA 改变；ledger 如实记 `NATIVE_EVIDENCE_PASS_WITH_LATER_FROZEN_INPUT_DRIFT`。这不是 Gradle 85窗口漂移：三生产修订发生在该队列终态之后，并在后续30/最终85的独立事前冻结中取样。不能把官方 replay 当作本仓 JUnit 或无全程漂移证明。

## 已结束的真实窗口

| 队列/窗口 | 实际结果 | 输入与原 XML |
| --- | --- | --- |
| `92a8a98c` / 39155，最终 S1 前候选75 | copying19/19、S0 36/36、lifecycle4/4、SOURCE4/14、原负例2/2；共65通过/10失败，零errors/skipped | 6816输入 SHA前后同为 `DE75311A3076ED86CA6226025A829570764509CC7B82FB0C93A6F65C349E994F`；10原XML共57184字节全部真实窗口。[inputs](cjmp-source-helper-20261117/s1-final-joint-75-inputs.json) / [results](cjmp-source-helper-20261117/s1-final-joint-75-results.json) |
| `987b2d25` / 12972，扩大85 | copying19/19、S0 36/36、lifecycle8/14、SOURCE4/14、原负例2/2；共69通过/16失败，零errors/skipped | 6824输入 SHA前后同为 `48204197420FA1B138CD982637B492992C55AC74B325AECEA77E798989FD51EB`；10原XML共67143字节。[inputs](cjmp-source-helper-20261117/r5-shared-s1-joint-85-inputs.json) / [results](cjmp-source-helper-20261117/r5-shared-s1-joint-85-results.json) |
| `cddf93bb` / 37471，三根因修复后的30 | lifecycle14/14、SOURCE4/14、原负例2/2；共20通过/10旧SOURCE失败，零errors/skipped | 6824输入 SHA前后同为 `5964DD5061F7B16467DDB1004890F67ACA513BBCA2F935CAA71909468D9FBDB6`；7原XML共47805字节。[inputs](cjmp-source-helper-20261117/s1-header-phase-30-inputs.json) / [results](cjmp-source-helper-20261117/s1-header-phase-30-results.json) |

各窗口都有独立 classname+testcase key、原 XML gzip/base64、长度/SHA 和 queue submit/start/finish/exit。不得拼重复计数。当前最终85回归由独立 [事前冻结](cjmp-source-helper-20261117/s1-final-joint-85-inputs.json) 和后续实际终态记载；此段不将运行中报告视为通过。

该85已实际终态：`e21ee6b7` / 52854，copy19/19、S036/36、lifecycle14/14、SOURCE4/14、原负例2/2，共75通过/10旧SOURCE失败、零errors/skipped；6824输入仍为`5964DD5061F7B16467DDB1004890F67ACA513BBCA2F935CAA71909468D9FBDB6`，10真窗口XML58761字节。[results](cjmp-source-helper-20261117/s1-final-joint-85-results.json)。该轮先于上述phase提交边界再修订，有界拒绝报告与再修订必须独立记载。

R5首两轮生产编译分别 `4b709d55`（缺 provider import）及 `ce4008d5`（内部 Snapshot 访问、pattern 类型等）；第三轮 `1566aad2` 已实际退出0，resolve/LL及闭包编译成功。三根因修订后的30也完成了其受影响编译。编译日志不计行为通过，也不宣称三轮编译均有事前冻结。

## R5 实际候选、审查与不可关闭边界

- tree 新增 `CfirOwnedGraphAnalysisState`，复制后分析要求与 CFG 分离。COMPLETE 分支需绑定具体完成产品，不只看 enum；RAW helper 仍走正常解析。
- semantics.api 暴露实际 owner context、完成分析接口和 rebuild 接口；resolve 新增 `CfirResolvedGraphAnalysisRebuilder`，保持 phase/type/binding，以 actual containing files 的显式 old→new 视图驱动既有 DFA/CFG。providers Snapshot/provider、BODY、LL readiness 与 checker root 已开始接 guard。
- 现有 IfEnter/IfExit 增加真实 then/else branch entry；普通 BODY、let条件递归和 resolved visitor 使用共同 If/AND/OR 事件。IfExit 采用互斥分支共有事实合流，不因一条终止分支使另一条死亡；Void visitor 与 copier 的方法面补齐。
- named/inout wrapped lambda 通过同一个 tree utility 定位真实匿名函数，DFA call 参数与 mini 参数分组共享。mini 具备 If/短路/可见模式绑定，条件绑定作用域不留给 else；已解析 global reference 不按同名 local 做替代。
- 取消/失败不许把拓扑当完整 DFA：当前新 rebuild 完成门检查真实 node.flow；未实现完整 Flow 的图明确失败，不能给空 flow 或非空空 CFG。

**完整 R5 仍 REJECT。** [初审11项](../build/source-expression-codec-audit-v113/source-r5-initial-production-review-20261117.md) 是实际接续队列；有界已修 B02/B03/B06/B11 的部分，但全域未复审接受。以下余额仍需生产实现，不用上述编译/十四生命周期替代：

1. B01：每个 root/非 CFG-owner initializer 的要求与显式 Header/Body 能力；single-root API 不能丢 pending ticket；typed/IMPLICIT 已完成事实不能漏登记，NO_BODY 不得由旧CFG为空猜测。
2. B07/B09：直接 symbol/target 读取与所有子图 getter都须服从同一 old→new/staged 产品；先核验完整事件/栈/图/借用输入闭合，再原子写回。当前 aggregate staged query不足以覆盖 LocalFunction/default/postponed/lambda getter。
3. B04/B05：实际 get/op/set/multiple-write 序列及共享左值一次求值，for guard false→continue/latch；两解析路径共用。
4. B08：接现存 LogicSystem/Flow/VariableStorage、完整 merge/default/alternate flows、真实 variables/versions/aliases、If/Boolean/type-test producers与批准、loop/capture/finally。固定 K 该版本没有独立 VariableStorage 类，不能冒用其他版本证据。[固定 Flow 事件切缝](../build/source-expression-codec-audit-v113/source-r5-kotlin-flow-events-20261117.md) 已逐方法考证；不得整块移植 Kotlin nullable/contract/sealed-only 语义。
5. B10：可达正式 resolved-copy compiler/LL/source consumer 装配及 Header-only 能力；定义 rebuild/guard不是正式消费通过。Coalescing/Availability 当前仍显式拒绝；Resume/Perform/Spawn 等完整语义/终止/capture产品需完成，不能 child-only 冒充。

没有 R5 实际 consumer 行为验收，没有源码门 ACCEPT 完整 S2。

## 下一授权生产工作

当前原SOURCE十四保持4/14。十失败为两parser的 walk/calculate/evaluate/recover完整正文及 DEFAULT value正文，仍被有限 `expressionOrNull` 拒绝；原正确源码、表数量、非零正文、占位、pattern/FullId/loop edge等断言均保留。

接续仍按原完整链：完成 R5/owned要求域和 LL source macro 的实际正常展开/采用 → S3 typed canonical图 → S0同代冻结 → S4独立d/e/t池统一reserve/fill encoder → S5 decoder/materialization → S6真实Source→CJO/common/ordinary/depends-on/frontend/CLI/LL消费 → S7及原70项/P0–P10余额验收。不得持续更换基础合同而不交付正文。

LL revive当前产生新的 unresolved macro carriers，postProcess只seal/validate/commit，未运行正常expansion。usage/carrier复制不是展开；须在真实原owner上下文完成宏，再生成完整helper图。完整runtime/adoption/rollback验证未执行。

S3需保存原 explicit retType/tail事实，当前 `CfirFunctionBodyDiagnosticData` 只有参数列表诊断位置，不含该事实。increment/compound恢复、Range for-in iter/stop/update/condition、is/as Match/Some/None/runtime flags、资源关闭图和30+3全部槽必须由语义owner生产。writer不能补这些节点。Source/imported snapshot身份、append-only declaration slots、完成状态与正式消费仍由原清单验收。

原70项/13余额全部保留，计分仍447/7÷70＝91.22%实现估算，全面验收未完成。E9 raw signature余额保留，H6不实施，v10待办不阻断其余授权工作；未关闭goal或归档方案。
