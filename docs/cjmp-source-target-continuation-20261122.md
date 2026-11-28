# CJMP 统一控制目标正式消费接续（2026-11-22）

> 后续2026-11-23的G2与Flow变量基础已验证48/48、SOURCE联合377/377；三轮factory拒绝已修订、两个CME实际失败已复验，原XML/同SHA与完整余额见[新接续](cjmp-g3-flow-variable-continuation-20261123.md)。真实analyzer/replay、完整SOURCE正式消费及原70仍OPEN。

## 非 DIRECT For 与真实 CLI 消费续验

在生成的 BODY dispatcher 中接入保存的 Call、Unary、Binary 与 FunctionArgument 专用入口。RANGE 外围执行保存的比较，内部条件读取相同 index/bound。三种非 DIRECT For 在正常 BODY 先建立真实初始化声明，成功取得元素后再执行绑定与 guard。enum F 的共同 CFG 节点和复制分类保留原 owner，Library 正文仍需独立材料化和分析就绪。

队列 `1f1d17f9` 通过 53/53，覆盖 For 正常 BODY、序列化、LL CJO 模块归属和原 Specific 重导出失败。队列 `810d4cb6` 在补齐参数遍历及分离 `GLOBAL` 与输出 `isTopLevel` 后通过 87/87，含原 SOURCE 18 项。这两个窗口先于最新缺省父类发布修订。 [53 项原账](../build/cjmp-formal-consumer-20261122/body-attributes-1f1d17f9-results.json) · [87 项原账](../build/cjmp-formal-consumer-20261122/global-body-810d4cb6-results.json)

原 CLI/LL 联合 `350521ce` 实跑 164 项，116 通过、48 失败。CLI 的 47 个失败中有 43 个缺少准备好的 `std.core.Object`。使用真实 1.1.3 SDK 的 `58f8ef2f` 单独实跑 CLI 92 项，63 通过、29 失败，其中 17 项失败文本包含父类型不匹配。固定 native 预检查先发布隐式 `Object`，匹配和保存读取同一引用。最新源码在断环后发布该父类型，并在类型描述中保留隐式来源，SOURCE 读取发布后的引用。 [原 CLI/LL 账](../build/cjmp-formal-consumer-20261122/p8-350521ce-results.json) · [真实 SDK 原账](../build/cjmp-formal-consumer-20261122/real-core-58f8ef2f-results.json)

官方 1.1.3 探针的三个变量都保存两字属性向量。两个普通全局 `let` 的 initializer 为 0，公开 `const` 有非零 initializer。第二个探针确认跨包 const 调用保存 CallExpr 和携带原 FullId 的 RefExpr。对应 CLI 测试保留普通 `let` 输入并按官方产物检查缺失槽，同时用同文件中具有保存资格的 const 初始化式保留原 FullId 断言。完整 SOURCE、R5 的 DFA 分析就绪、跨根借用、库 enum F 分析及原 70 项仍待完整验收。H6 排除余额保留分母，v10 待办保留。 [官方探针](../build/cjmp-formal-consumer-20261122/native-variable-eligibility/consumer.cj)

`b6fe7544` 使用真实 SDK，CLI 76/92、超类型发布 6/6。`e2b220bb` 在后续断环和隐式边来源修改后得到 CLI 78/92、超类型相关 19/19。后者新增通过共同字段初始化依赖重导出及跨包表达式 FullId，原 HMPP 多词法 owner、common-only 名义成员的格式头归属仍失败。最新源码将物理成员遍历与输出成员表分离，并按语义 binder 保留共享包字节，待本轮定向验证。 [首次父类型发布](../build/cjmp-formal-consumer-20261122/first-object-b6fe7544-results.json) · [H9 前次行为](../build/cjmp-formal-consumer-20261122/h9-e2b220bb-results.json)

`4cc13ac0` 定向 CLI 3/5，providers 35/35。HMPP 重复 CJO 合并、共同字段依赖与跨包表达式 FullId 通过。泛型正文重导出的剩余失败进入实际 F 保存资格，common-only 名义成员的剩余失败为 U/T 输出键冲突。固定原生 @Frozen 探针编译通过，种子用该真实资格保留原 block/local/泛型断言。类型保存计划随后接入同一 family 泛型映射，最新行为另验。 [H9 定向原账](../build/cjmp-formal-consumer-20261122/h9-owners-4cc13ac0-results.json)

`6b192fc2` 定向 CLI 4/5，泛型正文重导出通过，原 U/T 冲突消除。common-only 名义成员剩余失败是重复登记已配对的 common `Box`。最新保存计划在预留 CURRENT 声明引用时，根据实际 CJO 来源和已选 counterpart 复用 specific 声明。固定 1.1.3 双阶段探针确认 `@Frozen identity` 在 common 和 specific CJO 中均有非零正文。 [CLI 原账](../build/cjmp-formal-consumer-20261122/h9-generic-6b192fc2-results.json) · [固定编译器探针](../build/cjmp-formal-consumer-20261122/native-variable-eligibility/generic_const.cj)

`3c42fb22` 的 SOURCE 与 canonical 28/28，序列化行为因旧测试的合流 API 编译错误未执行。迁移旧调用后的 `c16e30f2` 中，统一 SOURCE encoder 与快照 22/22，含两个实际 binder 共享同一 CJO 字节的新用例。Flow 引用测试另为 12/16。普通 initializer 的额外批准类型事实在 `0af8f905` 修正并复验 24/24，后续存储陈述可见性与递归失效测试另验。 [SOURCE 原账](../build/cjmp-formal-consumer-20261122/h9-source-3c42fb22-results.json) · [序列化与 Flow 原账](../build/cjmp-formal-consumer-20261122/h9-flow-c16e30f2-results.json) · [Context 原账](../build/cjmp-formal-consumer-20261122/value-context-0af8f905-results.json)

> 本批最终行为：b57fe0a1实际397/397、0失败/错误/跳过，SOURCE18与SDK5再次通过；该队列退出1是resolve测试源码的旧API编译失败，未计未执行的resolve用例。[完整397机器账](../build/cjmp-formal-consumer-20261122/joint-third-results.json)保留这一差别。9294d4af修订编译DONE0后，a3a956ca真实21/20/1暴露lambda用例调用了不负责header完成的近似器；原Int8期望不改，driver改走真实completion factory与CFG，d41e202b编译DONE0，ab639a77最终21/21。两个成功行为窗口分别记录，不冒称一次418项联合。

> 397窗口10022输入2AE1D49D…E7F5F、SDK84同SHA，47原XML ZIP 6F64D539…6F5165逐字节SHA核验；21窗口10022输入54416DEE…A2767、SDK84同SHA，3原XML ZIP BBB935E6…BB1DC核验。所有旧失败与API迁移、[lambda职责考证](../build/cjmp-formal-consumer-20261122/kotlin-lambda-approximation-map.md)、[native语义](../build/cjmp-formal-consumer-20261122/native-lambda-result-authority.md)、[driver有限gate](../build/cjmp-formal-consumer-20261122/resolve-lambda-writer-driver-gate.md)完整保留。G1/G4有限ACCEPT；G2/G3、enum F/readiness、CLI/LL完整模式矩阵及原70/P0–P10继续OPEN，目标active。

> 最新实际7e088ff6为397项395通过/2失败，0错误/跳过；原SOURCE18与SDK5实际通过。10022输入前后同D841E223…59374，SDK84同527487DA…D2B，47原XML无损ZIP已逐SHA核验。[完整机器账](../build/cjmp-formal-consumer-20261122/joint-second-results.json)保留新guard死边与原冻结Return重绑失败。G4已[限定ACCEPT](../build/cjmp-formal-consumer-20261122/target-consumer-second-gate.md)，G1及target可重绑合同已[限定ACCEPT](../build/cjmp-formal-consumer-20261122/target-consumer-third-gate.md)。G1专用For节点/共同continue索引/visitor/copy/snapshot/finally及共享死路径传播已实际修订，df7a45ad已DONE0/15m01s，resolve/LL及三个测试模块源码编译通过。两target新增的禁止活动owner重绑检查因无固定源码依据已移除，原冻结测试期望未改。新联合验证正在执行，事前输入2AE1D49D…E7F5F；原失败不按编译绿删除。G2/G3、enum F/readiness及完整SOURCE/原70/P0–P10仍OPEN。

唯一实现树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10、完整S0–S7保持原范围；H6排除余额保留分母、不实施，v10待办不阻断其它授权工作。历史未提交修改全部保留，没有联系停止会话。

## 磁盘与实际编译

恢复时磁盘比上一份[正文接续](cjmp-source-body-continuation-20261122.md)更靠后：3488520a的decompiler定向测试已DONE0，86871e0a完成统一target正式生成；a639ae4a已推进至providers的旧For字段及窄target类型失败。没有重新应用旧模型补丁。

本次真实队列：

- 70f44cb8：DONE1；providers与serialization生产编译完成，resolve仍有旧For字段、while生命周期及目标类型错误。行为测试0。
- d5becce5：DONE1；同步后的resolve只余Return的窄扩展调用，checker余可空pattern类型，raw余FunctionBodyOwner导入。行为测试0。
- c51461f3：DONE1；resolve/checkers生产编译通过，LL失败于旧bindFunctionTarget签名，tree/serialization测试编译失败于已删除Native Return/Break和ForInIteration API。行为测试0。

后续编译及行为终态必须按新队列独立记录，不用旧252ed380的395/395证明本次字节。实际设置JAVA_HOME为21.0.10；观察到Gradle复用了Corretto21.0.9 daemon，不能声称daemon为21.0.10。项目toolchain仍17；所有调用经gradlew-queue.bat。

36a30a36已DONE0，LL生产及tree/serialization/analysis-tests测试源码编译通过。0e8e3417的JDK参数被PowerShell拆成两项，DONE1/行为0；没有把旧XML计作该窗口。修正带引号参数后7e088ff6确实使用21.0.10 daemon执行联合，397项两个失败如上。没有修改正确源码、表数、body/target/位置/identity期望来消除失败。

## 本次实际生产修改

For冻结载荷直接保留pattern/iterable/guard/body/kind，Return与Jump采用FunctionBodyOwner/IterationOwner实际身份。normal resolver直接解析模式与绑定，guard使用Bool期望；不再制造For.condition或wrapper variable。For专用CFG保留序列一次求值、迭代成功/终止、guard失败回下一轮、body完成回边，以及统一原Jump的break/continue/finally目标合同。mini赋值流将pattern/guard/body置于重复域，checker消费同一pattern与真实guard。

SOURCE canonical producer以原iteration owner登记循环；非DIRECT已完成For保持其真实Block、pattern、body、guard和kind，避免再次调用iterator或丢失完成阶段。LL nonlocal raw builder重绑定域扩展至真实FunctionBodyOwner，包括enum F，具体accessor角色和PSI身份规则保留。

[测试迁移候选](../build/cjmp-formal-consumer-20261122/test-target-migration.patch)已由主线程经apply_patch整合：原identity/body/target/position/表数期望和源码保留，只迁移已删除类型/字段API；For与Loop的类型区分仍被断言。新增真实For外部/内部目标复制与guard/CFG边界用例。此记录不将候选检查或测试编译当作行为通过。

## 固定源码与未闭合要求

Kotlin固定 `3c7a5041f238102285764d370665f56f587f9dc4`，官方仓颉固定 `14276740ffec1262b81b6b5236b1d840af270b37`。本轮mapper因实际token rate limit失败，主线程直接git show读取固定FunctionTarget与CFG loop/Jump实现；CangjieSemanticsAuthority已交付[正式目标考证](../build/cjmp-formal-consumer-20261122/native-target-authority.md)，测试writer与独立gate采用同一固定版本。

明确仍未闭合：非DIRECT iterable Block中初始化一次与每轮latch/condition的完整resolver/rebuilder/mini消费；enum F的正常BODY与完整CFG生命周期；真实DFA flow/版本/别名/循环/capture/finally重建与分析就绪；共享occurrence、借用header跨根、失败/取消/epoch；CLI/LL/Common/ordinary/depends-on模式矩阵与原70项逐ID/P0/P10余额。当前raw For CFG不证明全部native kind已分析就绪。完整S2/R5/S5/S6及目标保持OPEN。

G2下一实施已有[固定执行阶段考证](../build/cjmp-formal-consumer-20261122/native-for-execution-authority.md)：RANGE外围检查及内部检查均在首个update前，后续P initializer/guard/body后才latch；STRING的真实index/end读取及index写回没有独立AST e，不新增假condition/assignment；ITERATOR的Q无case，只持有真实next selector，Option判定后才每轮绑定Some内原P。正常resolver、mini及resolved rebuilder必须消费同一身份/阶段合同，不能按变量名重建或再次lowering。
