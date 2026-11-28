# CJMP 普通 SOURCE 正文接续（2026-11-22）

本记录接续 [省略签名](cjmp-source-signature-continuation-20261121.md)。原70项/P0–P10、H6排除余额保留分母、v10待办均不变，完整 SOURCE/S2/S5/S6 和 goal 继续 OPEN。唯一修改树仍为 cjmp-framework-implementation/cangjie；全部历史未提交修改保留。

## 真实状态

2026-11-22 最新联合为 **252ed380 / DONE0：395项395通过，0失败/错误/跳过**。完整机器账见 [joint-sixth-results.json](../build/cjmp-body-reader-20261122/joint-sixth-results.json)：serialization300、SOURCE/consumer/canonical28、providers40、owned-copy27，包含 ordinary正文12项、原SOURCE18、真实SDK配置5项。10033输入前后同 `3C75056A490A99CA026627176C1EFD6967618D2ABF3ADAE0A7FD56A64146D4F7`，SDK84文件前后同 `527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`。46原XML132340字节均属实际窗口；三段无损ZIP的SHA为 `C075B4D51097EBAD1D020CDC6A21D3228549143613FB25D6CB18A4CBCE41878F`，内存解压逐文件SHA核验一致。未将编译结果或旧窗口拼入本次计数。

本批实际修复：已有ordinary e在canonical reserve阶段登记来源；具名/enum F首次header登记与匿名F先登记后读参数；Package/null的直接owning创建及独立local身份；声明内容只选真实default/annotation/initializer根，局部内容完成恢复当时owning plan；模型header构造器本体一次冻结；完成Graph证明下复用同一个e/P record且不覆盖首次池；实际selector一次规范化；独立borrowed绑定域及map环/冷loop/selector的明确准备域。普通body/initializer仍经同次完成快照发表，getter不解码，未更改旧节点后回滚。

正式 `CfirImportedSourceGraphProvider` 已新增 `sourceBodyOf(FunctionBodyOwner)` / `sourceInitializerOf(Variable)`，依本provider已发表的真实owner身份选原context，匿名F不造d/provenance；CfirDeclDeserializer提供显式单e入口。无F enum与实际body0分别保持真实缺失。LL binary-origin工厂已注册同一SOURCE能力，但 **LL编译仍未通过**：最近实际失败在 CallableCjoStubBuilder.kt 的旧TypePattern.bindingName/bindingVariable及未穷尽Catch/Command；序列化生产/测试编译已过。未把LL注册或SOURCE图可读当作CFG/分析就绪。

本批所有失败独立保留：3d3e56c4、bb030462为编译失败，actualBehaviorTests=0；7e0102f1为388/383/5、036de6f9为392/389/3、8d6b7a73为395/392/3，原XML与前后摘要均在新build目录。7be8e699的private context访问已改为持有对象的ownsDeclaration/sourceHeaderOf方法；后续LL依赖失败仍保留，不伪称通过。

测试技术修正均见 [test-contract-review.md](../build/cjmp-body-reader-20261122/test-contract-review.md)：value-class装箱改为真实列表存储/元素身份；人工完成seed补P自身类型；人工无source inner与真实parser inner分开（**两个raw的真实innerSource没有修改**）；graph-first initializer通过正式显式请求升级内容；三cold tests显式调用普通reader，Boolean值用false而非wire字符串。原SOURCE七份源码/SHA、表数、所有正确body/target/position/identity期望保留。

**完整SOURCE/70/P0–P10保持OPEN，目标active。** 下一先按 [decompiler-pattern-authority.md](../build/cjmp-body-reader-20261122/decompiler-pattern-authority.md) 同步声明stub合法域，重跑decompiler/LL；随后顺序整合 [unified-target-model.patch](../build/cjmp-body-reader-20261122/unified-target-model.patch) 的八路径候选并正式生成，完整迁移raw、BODY、DFA/CFG、copy、source/checker消费。该候选尚未应用，原Native Jump/两target/ForInIteration重复层仍存在，不能关闭对应拒绝。普通shared occurrence、受限RootHeader跨根borrow、CLI/LL模式矩阵、失败/取消/epoch、正式分析readiness和原70逐ID/P0/P10余额仍须验收；H6排除余额保留分母，v10待办不阻断其它授权工作。

本轮新mapper/authority/gates均在 `build/cjmp-body-reader-20261122/`：kotlin-publication-map、native-owner-target-authority、return-view-contract-review、second-record-borrowed-gate、formal-provider-content-gate、unified-target-framework-map/model-notes、formal-body-consumer-authority。返回旧不可变视图的内部generic T不改写；过宽的publication-return/cold preparatory-origin P1已独立撤回，实际record/selector/binding/readiness拒绝未删除。恢复工作先核磁盘与队列终态，不沿用旧运行句柄、不联系停止会话。

最新联合24fb33c0的真实 [机器账](../build/cjmp-body-reader-20261121/joint-results.json)已经落盘并复核无损ZIP：388项309通过/74失败/5skip；46原XML407018字节全部实际窗口，8段ZIP base64合并的SHA为 E05B23D13AA470D7B9F5063510805BB06FF121F24B8CD17E334F856C0D4C6929。所有原XML逐文件SHA核验通过，10026输入前后同158995B0…50EB。copy27/providers40通过；原SOURCE/ordinary5、graph身份及owner等失败不能被局部绿覆盖。

本轮已完成共同P的ResolveType/Catch/Enum/Command入口、resolution snapshot和CFG-rebuilder消费同步，resolve/checkers及三个tests源码编译已推进通过；新阶段无新增origin时的图复制已修。缺口仍为 F header首次登记、annotation owning declaration header重入、普通辅助节点origin加入同一完成图、SDK配置5skip补实际运行、统一Jump/target/for及完整分析生命周期/CLI/LL。当前最新失败窗口仍以上方74失败为准，后续生产修改尚未行为复验。

本轮最新行为为 [24fb33c0](../build/cjmp-body-reader-20261121/joint-results.json)：388项309通过/74失败/5skip，原 SOURCE 仍有12实际失败，新ordinary5全部失败。10026 selected输入前后同158995B0DDA8327F7972008F70990FCB561FCF5717F45E3641C486284EED50EB；46原XML共407018字节在实际窗口，以8段ZIP base64原字节归档。失败机器账与原XML SHA完整保留，不用旧373/383代替本轮。scope remains OPEN。

共同 Catch/Command、Type/Enum、resolution snapshot、模式绑定、canonical producer及CFG-rebuilder消费者源码已继续同步，resolve和checker生产编译已通过。da37d942三compileTestKotlin实际DONE0。此编译绿不替代本轮行为失败；74失败集中在每次根事务无条件withOriginAliases复制完成图、F header首次未登记、annotation d重入、P类型/origin发布和新test header列表不变边界。已修没有新增origin时的无条件复制，后续尚未重跑。完整 Jump/target/for合并拒绝、正常CFG/readiness、正式CLI/LL及原70余额保持。

最新24fb33c0实际联合行为已归档 [case/XML机器证据](../build/cjmp-body-reader-20261121/joint-results.json)：serialization293项62失败/5skip、analysis28项12失败、providers40/copy27通过；新ordinary5项全部实际失败。10026 selected（含生成器）前后同158995B0…50EB，ZIP原XML分8段base64无损保存，合并解码后按逐文件SHA验证。旧373/383不替代本轮，所有原失败保留，不授予整项通过。

首轮实际根因为每次事务无条件withOriginAliases复制完成图，导致owner身份冲突/graph同一实例断言失败；另有F header未登记、annotation拥有d重入StackOverflow、辅助ordinary e origin遗漏和未发布P类型。已先修无新增origin时的重复图复制；剩余按真实堆栈继续修。SDK配置类本轮5skip需补实际配置后实跑，不能计入通过。

2026-11-22当前继续的编译与源码证据：serialization 已通过，PSI/light-tree 已通过；69566cb9 整体失败仅在 resolve，共同 P 的旧字段消费、快照、canonical producer 和 CFG仍待同步。本批已进一步改 Type 内部绑定、原生 runtime 位与 constructor expression 的正常解析入口，后续修改尚未重跑。source producer 和 Catch/Command完整解析仍 OPEN。没有新的行为/XML窗口，不能将编译或限定源码门当作 SOURCE5测试通过。

当前五项 ordinary body integration tests 已落盘但未执行；下一先修 resolve 剩余实际坐标（CfirResolutionSnapshot、CfirExpressionsResolveTransformer、CfirPatternBindingScope、CfirResolvedGraphAnalysisRebuilder、CfirSourceGraphProducer/CfirSourceTryCanonicalizer、CfirTypeResolveTransformer），再编译tests并冻结输入执行原SOURCE18、SDK与新ordinary测试，保留真实失败。

本次继续：providers 与 serialization 生产编译已实际执行通过，PSI 与 light-tree 生产编译也已推进通过；联合队列整体仍 DONE1，失败在 resolve 的旧 Type/Enum/Catch API、resolution snapshot、source producer、分析重建等同步缺项，未执行行为测试。69566cb9 最后明确失败窗口为 resolve（不是新的 SOURCE 测试失败）；该窗口前所有历史失败仍保留。当前新源码仍不据编译通过关闭 SOURCE、P0–P10或原70项。

Header P 已改为真实 tablePosition/token 入口，完整分派八种模式、恢复自身类型/位置/values；Var 绑定只取实际 d 槽、实际 VarInfo/status/type/泛型，不再用缺失模式/未知 kind/名称/父类型补绑定。旧 Enum header 也读取完整 constructor e。该路径与普通 body 的共同模式产品仍须行为核对，尤其构造前 e 的 owner 重入与失败重试。

新 published-content-gate.md 六项限定静态 ACCEPT 覆盖被动快照 getter、首次绑定、同代 copy、冻结/发布隔离；不授予 SOURCE 行为 ACCEPT。当前共同 Type 绑定解析已改为真实 inner，原生两位按正常匹配结果发布，UNKNOWN 保留未完成状态。剩余 Catch/Command resolver、source producer、CFG/analysis、统一 Jump/target/for 和跨模式正式消费继续实施；新5项测试尚未运行。

2026-11-22后续：已应用 [被动内容数据源](../build/cjmp-body-reader-20261121/published-content-source.md)的13个一方源码候选，并完成第二次正式生成。普通 body/initializer getter 使用首次构造绑定的 Published 数据源，仅查询 completedGraph.bodies；复制一次捕获同代 snapshot 并生成 Owned 新树。原终态 setter 已撤除，没有修改旧节点后回滚。当前源码仍待完整独立复审与行为证明。

1581fa2b 实际 DONE1/tree 编译失败（open receiver private setter 与 catch renderer）；两项已修。79d4f6a2 实际 DONE1/tree 编译失败（renderer 缺 CfirCatchPattern 导入）；已修。b7482fdf 实际 DONE1，tree 已编译通过，providers 仍有共同 Pattern frozen payload 的10项实际 API错误。该 payload 已迁移到真实 inner/constructor/flags/semanticType，尚未重新编译验证。所有八个 Native P 的 gen 已由正式生成移除；Native Jump/target/for 拒绝仍 OPEN。

PSI/light-tree 的 Type/Enum 与 Catch/Command raw 构造已同步真实 inner 和 constructor expression；不再把 enum receiver/类型参数拼成一个名字，不再以缺失名字伪造 catch wildcard。对应 matcher/resolver/source producer/CFG、旧 Header P reader 及 frozen-payload分析重建仍须整体同步。新5项 tests 仍未编译运行，最后行为仍383/11。下方旧“尚缺 content source”保留为候选应用前时点，以上方当前状态为准。

- 3d117ff2：generateTree 与 serialization 生产编译实际 DONE0，发生于本次正文/共同 Pattern 改动之前。
- 263ed075：三个 compileTestKotlin 实际 DONE1。daemon-18708 的真实错误为 copy test :394 的 bindingVariable 旧 API，以及 :670/:890/:891/:945/:946/:960/:961/:976 的 buildBlock 缺 init。已修对应构造调用与身份断言的 API，未修改正确期望。
- a5afc993：本次正式树生成完成，tree 生产编译实际 DONE1；serialization 未完成。错误为三个手写表达式类缺 map/overflow 契约（inaccessible receiver 继承 this receiver），以及 PatternBindingUtils/MutableState/Renderer 尚未接共同 Pattern。没有新行为验收或 SDK 通过证据。
- 最近真实行为仍为 [88aec81a](../build/cjmp-declaration-completion-20261121/joint-first-results.json)：383项、11失败、无跳过，9949 selected 输入前后同 SHA。旧373/3与其它窗口均不能证明本次新代码。

## 已应用的生产改动与未验边界

完整 CfirBodyDeserializer 已实现 SOURCE33 分派和 F/P、mixed Block、真实 initializer、loop/Return target；annotation/default/initializer 已替换旧四表达式入口，旧 literal/reference/unary 小 reader 已删除。正文 helper 持有同一 declaration owner，借用 header policy 捕获后排队，避免在 policy 恢复后扩张初始化闭包。

e/P token、记录及 ordinary 节点/target 工作集进入包 context 的同次完成快照；canonical finish 后登记最终 selector/P 记录，普通 e origin 加入完成图。新 SOURCE map/overflow、P.values/context、Assign.op/isCompound、Lambda.supportMock 字段已进入生成源；copy 对借用回边只重绑已拥有节点，常量列表用同一值复制域。当前冻结记录复用、共享拥有 occurrence、任意子树的借用 target header 仍需按源码门继续修正和复验。

[新 gate](../build/cjmp-body-reader-20261121/model-loader-gate.md)明确 REJECT：按 raw/native 来源重复 Pattern、Return/Jump/targets 和 for 模型没有依据。NativePattern 根及全部八个 Native P concrete 已从生成源移除，共用原 Wildcard/Const/Binding/Tuple/Type/Enum/Catch/Command；Wildcard/root 的第一次重生成已移除旧 gen，剩七 P 合并应用在 a5afc993 后，尚未重生成。必须继续同步全部 raw/resolver/source/checker/CFG 消费；不得追加空 else 或恢复旧平行层。

[字段合并合同及调用清单](../build/cjmp-body-reader-20261121/pattern-merge.md)列出完整同步入口。当前 body reader、BindingUtils、renderer、共同 MutableState 和 copy fixture 已开始同步；原 raw builders、resolution snapshot、Type/Enum/Catch/Command resolver、source producer 和完整 checker/CFG 尚未完成，不能按源码定义合并关闭该项。

## 原子正文发布与下一实施

[Kotlin mapping](../build/cjmp-body-reader-20261121/kotlin-mapping.md)第3节证明真正旧函数的显式正文请求及本任务更强的原子合同：ordinary getter/visitor 必须被动读取同一个完成快照，getter 不解码，tree 接口不泄露 context。先写旧 fn.body 再切快照的候选已撤掉。当前 public deserializeFunctionBody(index) 有真实非零内容入口；旧 header 尚缺被动 tree content source，会结构化拒绝该不完整发布路径，不能以 side map 或局部解析冒充正式消费。

新测试 [CjoBodyDeserializerIntegrationTest.kt](../cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoBodyDeserializerIntegrationTest.kt)有5项：header-first、坏正文/新context重试、default/annotation mixed Block+真实 SDK String 四态、ordinary/full graph 身份/target。仅静态准备，未编译或运行。

继续顺序：应用已考证被动 content source → 同步共同 P 的全部 raw/resolver/source/CFG → 统一原 target/Jump/for（不造 condition）→ 正式生成并生产/测试编译 → 冻结输入后 SDK serialization、SOURCE18/consumer2/canonical8、snapshot/copy/lifecycle/new body 合批复验，保存原 XML/逐case窗口 → 原方案完整跨模式/CLI/LL/P0/P10及逐ID余额。

本次新角色只读依据与候选分别为 native-reader-contract.md、kotlin-mapping.md、model-loader-gate.md、pattern-merge.patch。不联系已停止或缺失句柄的旧会话；实现以本树与实际终态为准。
