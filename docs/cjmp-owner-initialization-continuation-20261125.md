# CJMP SOURCE 正式消费与 owner 初始化接续（2026-11-25）

## 14:25 后的 finally 与委托接续

### R26真实body initializer Return纠错与完整模块后续

**最新Return合同覆盖下方历史“initializer统一null”的错误概括。** 固定N ExprLoader.LoadFuncBody:488–509从真实F.body开始Walker，跳过nested FuncBody，经过Block.d→VarDecl.initializer→If→Return；原`returnInsideAnIfInitializerKeepsItsFunctionDomainAndVariableContext`的function/context/Nothing类型期待正确。`r26-local-initializer-return-domain-authority.md`已独立复核固定N Walker/Sema/docs、原XML和原测试。主线程已让首次initializer与后续Initializer root均继承原d.scope.returnFunction；真实body局部initializer保留原F，global/member/default/annotation来源自然保留null，nested lambda/body仍自有F。没有删原Return、改变量context或降低正确原测试期待。

最初58请求窗口c实际58/58，但不能据此认证错域新测试的语义。完整d模块实际432/421/11（45原XML、9844输入及本轮46 SDK文件前后同SHA），独立`r26-full-serialization-failure-partition.md`把11项精确分成SourceParent/RHS前置4、旧eager header/index4、Return域1、missing target异常期待1、VArray raw-save1；它们不是同一根因。SourceParent四项在原手写status/alias前置就失败，尚未进入copy/rebuild或save-order的目标门，不可把更早异常当预期拒绝通过。旧eager入口触发未选坏body/语境冲突，必须由完整B1/B2消费迁移修复，不放松union/上下文守卫。

按固定N纠正新增测试的错误null-domain期待：独立global/default/annotation负例保留，官方y/observed原initializer Return身份、body内If initializer Return与lambda增量正例保留原F。原测试未修改。最新`r26-return-domain-20261126-e-results.json`实际61/61（新52+原8+原正确Return回归1），0failure/error/skipped，三个原XML全文gzip/SHA及逐case保存。9844输入前后同SHA `40f51a8401b261ac9174a6c150151e34e93db6461fa1a6c53c9034f2787f9666`，本轮SDK46前后同SHA。e production/test及受影响依赖编译成功，136任务7executed，2m51s；不用旧c的错域绿替代这批新字节。

G1–G4与H1–H3已继续修订：actual enclosing-loop/lexical-F、d/P definition slot、VarPattern/accessor种类、Custom-only target与native annotation argument角色、未选annotation授权先于getValue、owning union早期校验、Array/VArray/CPointer真实type结构及canonical条件消费。`r26-demand-read-boundary-final-gatekeeper.md`已局部撤销H1–H3的旧源码原因，不授完整R26/Owner6或消费者迁移。普通annotation reader和旧header binder的Custom条件也同步。SDK采样仅当前46 cjo/cjd选择域，不冒称历史84全SDK。

当前新完整模块窗口f（`r26-serialization-20261126-f`，exec session60930）已真实退出1：434实际424通过10失败，0error/skipped，45原XML/逐case/原终端已封存，9844输入与本轮SDK46前后同SHA。输入摘要仍 `40f51a8401b261ac9174a6c150151e34e93db6461fa1a6c53c9034f2787f9666`，和e是同字节；原Return回归及新52+原8在f也全部通过。余10保持SourceParent/RHS前置4、旧eager header/index4、missing target期待1、VArray raw-save1，不把原11减一推测代替本轮实际结果。所有本轮Gradle已终态，无活动本轮Gradle；writer只读准备Source fixture正规supertypes/RHS生命周期候选文档，尚未改这些原测试。D/E的reference absence与VArray unused raw槽合同也已交本轮open authority核证，禁止靠改正确期待或绑定ignored target换绿。

下一关键仍为正式B1/B2：header registry/current typed需求与原binder连接，停用eager sourceOwnershipIndex和双重borrowed roots扩张；真实boundContexts/原F.body/d-content槽、type/cache前授权、TP inverse metadata索引消费、default/annotation Published、selected/aliases同根原子发布与provider/全部callers。原70/P0–P10、原完整140/120/20的owner/alias/诊断余额全部保留；四正确parser负例的共享leftValue/leftAux mapper已补，不能改正确消息/span。H6排除余额留分母、v10不阻断，目标持续active。

### R26 typed需求、实际保存槽与G1–G4修订接续

已读取两份原R26拒绝、最新context-registry复审及固定K/N的r26-type-demand-authority.md、r26-typed-demand-kotlin-mapper.md、r26-typed-demand-revision-gatekeeper.md。主线程继续实施CjoSourceContentRequest/OwnershipPlan：typed type/FullId slot需求、d/e/F/P/constraints/instTys/matchedParentTy/alias/泛型最终bounds、genericDecl、实际policy条件common dependencies、可达零基value pool及其type/field需求。零type保留真实slot，不读allTypes[-1]、不补Unit；typeArgs按实际kind消费，VArray不消费initFunc。完整classifier成员/accessor header及TP semantic-type metadata来源显式进入同一冻结需求，不能由后续binder再隐式全包发现。

MAP/REF_LOOP/SELECTOR实际source/target occurrence、kind/type/原F/selector证明在typed固定点前分类；borrowed metadata闭合但仍要求后续原record/graph完成证明，不把borrowed target认领为owned。root lambda F的类型从已登记occurrence标量进入需求。initializer首次/增量统一null Return域；e真实字段/ordinal独立于record identity，registry合并先清内容mask再distinct，原四字段身份检查保留。

新增CjoSourceContentRequestTest最初36方法与原8例联合：`r26-typed-request-20261126-a-results.json`实际44项43通过1失败，0error/skipped；9844选定输入事前/事后完整manifest同SHA `d350a569d551256aaa276a748d3ecf7ccd5245c22899d0a70adf8dd77d2572e7`，两份原XML全文gzip/SHA、逐case及原终端独立保存。此manifest选择当前一方代码/schema/test/resource文件，不等同历史10800输入或SDK84整套采样。唯一失败在新测试第143行要求create纯拥有阶段也拒绝TYPE/P.type坏槽；前面的typed request严格拒绝已通过。writer已按阶段合同保留create原坏槽与request拒绝，未改原8例或正确SOURCE期待；两份官方y/observed原CJO增量initializer及其它43方法本窗口已实际通过。

新Gate仍REJECT首组件并列G1–G4，主线程已经继续：REF_LOOP保留Return域检查、核原lexical F与所有实际拥有路径中的enclosing loop，null内容域不一概禁止；d/P新增实际definition kind/field/ordinal，不同定义槽明确冲突，GLOBAL helper package查询根与参数真实拥有槽重归属保留；VarPattern恰1 VarDecl、get/set槽必须FuncDecl；annotation仅Custom消费target，非AnnoKind.Annotation选中arg必须literal。annotation target的canonical decoder、普通声明reader、旧header binder同步真实kind条件；union info按实际type/expr kind校验。上述G1–G4后一批生产compile70981实际DONE0（35s、29任务1executed），该编译发生在最后普通annotation reader/header binder同步之前，不冒称全部最新字节已编译/行为通过。新的definition-slots-annotation gate和附加null-domain/d/P/annotation负例正在本轮open代理中准备；不能沿a窗口或旧43995f90授新字节通过。

**正式接入仍未完成：**旧context.sourceOwnershipIndex、TP inverse binder、provider/e API、decoder boundContexts/selected F.body、default/annotation Published及selected/aliases原子目录尚未迁移。下一按typed mapper和最新slot gate继续修复、冻结新字节实跑；再接全部正式consumer，禁止loader miss扩大域或父类/名字补owner。原完整行为仍337f29df的140/120/20，Owner6、alias8及原70/P0–P10全部保持OPEN。独立r26-receiver-assignment-parser-authority.md已复核四个conditional赋值负例及原parser消息/操作符span正确，根因共用parser leftValue/leftAux角色缺失；不得改正确负例，后续共享parser修复按固定K/N mapper实施。

当前没有活动Gradle（a窗口已终态，生产70981已退出）；新增测试与最新源码复审在准备，完成后启动新的独立窗口。保留全部历史未提交修改；H6排除余额留分母、v10不阻断，目标未关闭。

### R26首组件拒绝修订：context/occurrence/registry继续实施

已读取r26-content-request-gatekeeper.md：R1 typed type/header引用需求、R2typed map/loop/selector与完成前置、R3metadata/occurrence、R4owning/lexical环、R5helper越权body、R6typed签名槽、R7增量registry、R8广测试均拒绝首组件放行。默认/annotation Published尚未开始迁移，先按拒绝项继续，不以原7/7覆盖未验范围。

当前已修context身份比较四项（declaration/expression/function/return），内容mask不参加身份等式；d/F/P metadata完整比对与e全部occurrence登记，selectedBodies由真实Function owning occurrence证明。owning祖先与F lexical/outer分别DFS验证根路径/环，正常F/parameter签名互引和map环不误当owning环。TP/parameter/helper按真实d kind和原signature slot唯一性检查，desugars长度/ordinal与owner保留；nested named/lambda的helper metadata不因includeBodies=true被自动授正文。

新增registeredHeaders结果：根事务后可发布的新不可变header registry，旧registry不变。真实下行localD/F/P与e祖先metadata（不存Expr内容）合并后供下一typedroot复用；actualPackage身份一致，冲突按原parent/scope/token拒绝，Header.exprs/occurrences仍空，content选择独立。新回归先选constructor.body发现readOther，再从新registry显式选该F.body，保留outerCtor context而不读取兄弟/外层body。

43995f90实际DONE0（1m56s、82任务7executed75up-to-date），8个用例全过，原XML/daemon/逐case在43995f90-context-registry-results.json；无独立beforeManifest，不授全树同输入/组件通过。ef50946c中间7例属于更早窗口，R4期间代码继续修订，不能用该窗口单独认证后续R4字节。原request reviewer已接续只读复审，目标报告r26-context-registry-revision-gatekeeper.md，不能沿旧SHA授新修订通过。

未完成R1/R2/R8：所有允许metadata中的type/FullId需求及annotation target、bounds/constraint/P tail/transitive classifier；typed map/refLoop/selector originating occurrence/F/地址/完成证明、Return非body/跨F；所有新增坏输入/未选body/default/annotation/helper读范围反例及新字节manifest。R3–R7也须新审查确认完整消除及潜在新问题，不由8例绿授通过。normal context/provider/decoder/Published内容与原子alias目录仍未迁移，Owner6和原20失败不计修复。

目前无活动Gradle；下一先补R1/R2/R8和新拒绝，再进行正式boundContexts/decoder/F.body、default/annotation Published及所有caller。最后完整行为仍337f29df140/120/20，原112绿不关闭原70/P0–P10，H6排除分母和v10边界保留。

### R26 request三域与直接下行构建已实施

新增CjoSourceContentRequest.kt：真实F.body/variable initializer/parameter default/annotation argument保存槽为显式root；内容、borrowed header contexts、e binding需求与实际FullId declaration绑定分别冻结。CjoSourceOwnershipPlan.request以纯header registry为seed，仅从本次root读取e下行，不进入全包top-level正文循环后再过滤；所有header/context保持同一实际Package/metadata identity。CjoSourceBorrowedContexts从真实d/F/P parent、scope、own TP、参数及desugar/helper链闭合，metadata声明绑定不成为内容，未选signature default/annotation不从body声明路径读e。

root F被显式保存到selectedBodies，rootClass和真正参考的class/constructor/function headers只进入context域；兄弟constructor body不选择。保存槽0保留真实缺失与宿主context，不生成空expr/body。旧create/forDeclarationContent仍为明确CONTENT路径，header registry不能selectExpressions或进入canonical decoder。

f9df0f0a实际DONE0（2m35s、82任务7executed75up-to-date），7个真实官方CJO用例全部通过，0fail/error/skipped。原XML（全文gzip/SHA）、case身份及daemon原流在f9df0f0a-content-request-results.json；没有独立事前manifest，不称全树同输入验收。前三个pure-header用例和新四个request用例保留各轮原XML，不重复累加为总数。新只读审查r26-content-request-gatekeeper.md正在准备，不沿bf41旧SHA授新产品整项通过。

**重要未接入：**当前normal context.sourceOwnershipIndex/provider/BodyDeserializer仍使用旧全包索引与e API，decoder尚未接boundContexts三域，SourceGraph.Builder ancestry/selected目录及CompletedGraph统一alias换代尚未完成，default/annotation Published按需能力仍待生成器/树模型与caller一起迁移。新request是完整R26第二组件，不等同修复六个owner失败或二十失败全绿。type/header binding需求、e借用完成证明、环/冲突与坏未选正文负例还须据新审查补齐，不能增加scope/getValue fallback。

下一步按R26 mapper继续header-only绑定读范围和Actual boundContexts产品，再decoder/F body请求/目录alias原子发布、provider与所有caller、default/annotation Published及测试。最后完整行为仍337f29df140/120/20，原112/SOURCE32绿不关闭原70/P0–P10。当前没有活动Gradle，所有历史未提交修改/正确语言与表数量期待保留；H6排除余额保留分母，v10不阻断。

### R26 首阶段：纯 header registry 能力已实施

当前CjoSourceOwnershipPlan新增显式HEADER_REGISTRY/CONTENT能力区分；纯headers()从真实top-level/member/TP/parameter/desugar/P-binding结构槽登记d/F/P，不读取任一e、default/annotation/initializer或F.body内容。真实F.body非零只保存其header槽存在性，不取得内容；selectExpressions和canonical decoder显式拒绝header-only registry，不能以scope布尔推断正文授权。原create/forDeclarationContent仍保持既有CONTENT调用语义。

新增CjoSourceHeaderRegistryTest和三份已封存官方CJO资源，覆盖probe局部x/y不进入header域、constructor不认领readOther、finalizer原F身份保留但不认领readParent/observed。bf41c934已DONE0（2m08s、136任务9executed127up-to-date）：3实际case全部通过，原XML及daemon完整原流保存在bf41c934-header-registry-results.json；没有独立事前manifest，不声称全树同输入验收。没有改原正确SOURCE/语言期待。

此阶段是完整R26的首组件，**旧context.sourceOwnershipIndex仍是全包内容索引，正常header/body consumer尚未迁移**；六个root F/owner缺项没有据三例绿关闭。下一实施必须引入显式request三域和header ancestry registry证明、selected body构建读范围，不把祖先加入owned.functions读取未选body；继而Loader/Decoder/CompletedGraph目录及统一alias发布、provider/callers与default/annotation Published按需能力整体接通。当前SourceGraph.Body.getValue不加fallback，全包扫描/名字恢复禁止。

目前无活动Gradle；最后完整140行为仍是337f29df的120通过20失败，原112/SOURCE32全通过。R26、S1/ND/F/parser/CJO roots和原70/P0–P10保持OPEN，H6排除保留分母、v10不阻断后续授权工作。

### 2026-11-26 01:42 mandatory F/签名修订与选中上下文新合同

337f29df已真实DONE/exit1（15m54s）：140实际120通过20失败、0errors/skipped、16完整原XML，原112与SOURCE32全部通过；新增28为8通过20失败。`337f29df-results.json`封存逐case/原终端/10800输入及SDK84前后同SHA；`337f29df-frontend-partial.json`是早期106/86/20分段，独立保留且不重复合计，不再按RUNNING轮询PID16072。

20失败的实际新分类：8个canonical graph alias视图对象身份断言，6个选中正文缺真实root F/owner上下文（Finalizer的functionContexts、constructor的declarationOwners），4个parser非法conditional赋值/compound诊断缺失，2个this消息缺引号。finalizer函数/属性双诊断旧失败已改善，不能把sourceBodyOf成功或包graph读取授全部消费闭合。

`r25-finalizer-production-gatekeeper.md`仍REJECT完整验收：normal finalizer缺失FuncBody会经空参数/返回类型路径被接受；FuncTy参数与F参数布局未闭合；source文件/位置及其他完整余额未验。本轮终态后已补FuncDecl公共入口、return type与参数reader、ownership plan的真实FuncBody不变量；正常finalizer必须完整零参数Unit FuncTy、真实单一空参数列表，真正UNREACHABLE/decl.type=0的partial-empty F仍按既有严格协议保留，不虚构F或body。52026f49编译失败仅为局部wire body变量遮蔽builder.body，已更名wireBody并封存失败；后续6cb9207c已DONE0（1m36s、104任务4executed100up-to-date），原daemon快照在同名compilation.json，行为0。

this消息源码本已有单引号，但MessageFormat把单引号当语法；已改为双单引号转义 `''this''`，不改变任何正确诊断期待。该修订的checker编译随新窗口已通过，实际输出仍待新行为验证。

`r26-source-graph-materialization-view-authority.md`独立核证：新增ordinary origins时不可变alias视图应换对象，但全部canonical maps/F/E/record/type/header/owner与body身份必须保持，无新alias的重复查询仍同对象。此前强制材料化前后graph对象assertSame的结构期待错误，不能简单删除共享检查或改assertNotSame；完整独立wire oracle、旧视图不变及新alias绑定检验还须实施。

`r26-selected-source-owner-context-mapper.md`已落盘完整下一合同。实际路径Body reader通过`index.selectExpressions`只拥有e下行闭包，OwnedDeclarations已绑定root header，但Decoder只为selected.functions/declarations发布context，导致rootFinalizer/ctor的getValue失败。必须建立显式ownedContent、borrowedContexts、borrowedBindings三域及header-only ancestry registry；root F/body请求、selected产品目录、统一alias原子完成代、provider/callers和default/annotation按需内容能力一并接。不能给Reader加全包graph/名字/缺输入兜底，不能把借用祖先塞入functions后读它们未选body，不能只特判Finalizer/ctor。新产品仍待main完整实施，当前未修这6个失败。

目前没有活动Gradle验证；原R24取消窗口和337完整结果均已终态封存。下一步从R26 request/header registry与读范围约束实施，然后loader/decoder分域与F body能力、目录/aliases、provider及真实caller/测试，再冻结输入验证；同时保留parser、S1正式执行表/ND/F/B5–B7/CJO root与原70/P0–P10余额。6cb编译绿不能替代任一完整整项验收；H6排除分母与v10边界不变。

### 2026-11-26 01:18 finalizer正式header/语义实施与新联合

已重新核对R24实际外部状态：PID41788/63540/daemon12868均不存在，daemon原日志在2026-11-25 20:02:38明确client disconnection/cancel，BUILD FAILED in13m11s；queue1c620477仍滞后RUNNING，未改写。新XML一份未产生，磁盘19:21/19:27 XML均属于87a00341，不计作R24。`1c620477-cancelled-results.json` 保存完整daemon与终端、10800输入/SDK84前后无漂移，complete=false、XML0/cases0；取消窗口不冒称140执行终态，不重复轮询旧进程。

本轮已实施FINALIZER正式身份位和首次header：CjoSourceFormatInputs对真实CfirFinalizer写bit33；CfirDeclDeserializer检查wire `~init`与FINALIZER一致、排除三个constructor位、真实class owner/static/local/generic/value-param不变量，首次构建CfirFinalizerSymbol（内部SpecialNames.END_INIT为`<~init>`）与CfirFinalizer。Unit或真正未导出签名的ConeInvalid标记原样保留，不填Unit兜底；annotations/原F/body通过既有发布接缝续接，不在decoder造第二header。`c59619bc` 已DONE0（5m34s、104任务6executed98up-to-date），编译证据在同名compilation.json，行为0。

finalizer语义已从class级call-only扫描迁至CommonExpressionCheckers的正式qualified access checker；旧scan已移除。裸this basic checker只处理显式普通值与最外层真实finalizer，排除implicit/成员receiver结构边，使用正确THIS_AS_EXPRESSION及一字符范围。新的CfirFinalizerMemberAccessChecker从错误/正常引用的真实绑定归一化accessor/override到原function/property，按实际this/super物理boundSymbol和声明owner type做两个独立诊断；other、字段、static/local不误授当前对象角色。A保留声明own TP方向subtype与error/stub=false，B独立capture，不通过A的false或已有诊断消除；两项源锚点分别selector/access起点，不用distinct去重。

`r25-finalizer-access-owner-authority.md`及build/r25-finalizer-access-probes原cjc证据细化：泛型self this成员双诊断，泛型/具体child访问generic父成员仅capture；other同类/异类均不触发两项，裸implicit SDK观察与固定N capture入口的边界独立保留，不能alpha/name/ClassId处理。extend用声明真实extendedType，不以receiver实例化目标owner。`825f86ad` 编译仅失败于nullable extend type/诊断Name参数，原证据已封存并按真实可用性/Name修复；`43c5962b` DONE0（1m49s、50任务4executed46up-to-date），行为0。

`r25-binding-source-owner-test-authority.md` 独立核证此前x→probe断言对SOURCE binding view错误、对plain CJO正确；已按入口分开：SOURCE x→真实容器→probe并核canonical Block第0 Dslot为同一binding，plain CJO仍直接probe且普通VarDecl，不运行时OR兜底。BODY从真实probe.body第0声明选择容器，并核实际输入file/path/parser snapshot与完整0..textLen范围；全部正确零错误/UBI/finalizer期待保持。跨wire begin/end/file/identifierPos与原CJO同Dslot位置的完整补验仍OPEN，不凭这些核心断言关闭来源验收。`a3cb30a7` DONE0（46s、104任务1executed103up-to-date），编译快照与43共同在 `r25-finalizer-compilations.json`。

新10800输入SHA `aab99515019e1af5a7df870121759c5383194642cc3c184b10125b9354262d55` 与SDK84已冻结，文件 `r25-finalizer-before.json`。hidden pwsh PID16072（01:18实核存活）、queue337f29df正在完整原112+新28联合140；日志 `r25-finalizer-20261126-terminal.log`，终态 `r25-finalizer-20261126-process-result.json`。当前有个别新测试失败输出，尚无完整XML终态，不改冻结production/tests/resources、不重启。新production gate `r25-finalizer-production-gatekeeper.md`只读复审正在准备，不授整体通过。

下一步核验337真实终态并封存140原XML/逐case/10800输入/SDK，从实际失败补完整finalizer/source位置/decoder生命周期及parser非法conditional赋值；随后正式S1执行表/ND-A/V/T/O/S、B5/B6/B7、F1/F2/F6、CJO roots和原70/P0–P10继续。S1精准mapper及原cjc准备已有build入口。最后完整已执行行为仍是87a00341的140/114/26，原112绿不替代所有余额。H6排除余额保留分母，v10不阻断，目标保持OPEN。

### 19:50 完整140封存与 R24 新窗口

`87a00341` 已DONE/exit1（12m04s）：140实际114通过26失败，0 errors/skipped，16份完整原XML及逐case、原终端、10800输入/SDK84前后同SHA全部保存在 `87a00341-results.json`。**原112全部通过，SOURCE32恢复32/32**；新增receiver28为2通过26失败。前述106的frontend-partial单独保留，不与全140重复合计，不再按RUNNING轮询PID45200/queue87。

26个新失败的根因与正确期待分别考证：`r24-receiver-nominal-identity-mapper.md` 证明Source显式typeref使用owner.symbol自有Impl tag，constructorOwnerType却私造Fixed tag，引发同名Cell的RETURN/ARGUMENT mismatch。`r24-finalizer-source-role-authority.md` 核证finalizer裸this/函数/属性的正确factory、消息、双诊断和不同一字符位置；实际物理dispatch被basic checker当裸this、class级scan漏属性/capture、SOURCE写侧未存FINALIZER位及reader未建Finalizer header均为生产余额。非法conditional写目标的parser诊断也仍缺，不改正确错误期待。

87终态后已实施两项生产根修订：constructorOwnerType使用 `declarationSelfType(symbol)`，不在消费者另造tag；metadata fixed owner证明、CJO预留Fixed策略和所有严格类型比较保持。SOURCE access/callee按实际explicit语法选择Member，裸Ref要求pure implicit this或真正非实例角色，纯隐式对象不额外写E operand，实际BODY dispatch/函数值callee/builtin副作用保持。相应新输入合同见 `r24-implicit-instance-source-reference-contract.md`。

新测试中原blanket RealSourceKind断言经固定K/N及真实容器/绑定视图独立核证为错误结构断言；已加强为唯一pattern.bindingOccurrence/variable identity、真实parent/name源、精确PatternBindingVariable derived kind、name-span范围与同PSI/LightTree锚点及realElement身份。全部正确零错误/UBI/finalizer语言期待保持不变。authority续审接受核心derived身份验证，但仍要求补真实probe/文件/parent完整范围与本仓CJO x的VarDecl begin/end/file/identifierPos/owning D slot对应；这些验收余额不按新断言编译绿关闭。

`d2edf3a9` DONE1（3m08s）：resolve新生产编译成功，测试的LightTree CharSequence返回String接口错误；原daemon和终态已封存并修为显式toString。后续 `0ec85909` DONE0（1m27s，104任务1executed103up-to-date），测试编译成功，行为0；其原daemon文件快照存同名compilation.json，仍不称compilation-only日志或同输入行为验收。

新10800路径SHA `53a34b3e00f3658a5bd3d1499c6bc32446a3fe90d438064fa9772e61182a9164` 与SDK84已冻结，见 `r24-source-nominal-before.json`。当前hidden pwsh PID41788（19:50已实核存活）、queue **1c620477** 通过原runner再次执行完整140，原stdout `r24-source-nominal-terminal.log`，终态过程记录 `r24-source-nominal-process-result.json`；未有新行为结果。先核进程和实际queue，不重复启动，终态前不改冻结production/tests/resources。

下一生产任务是完整finalizer F-Sema/F-Header：最外层finalizer中的显式裸this使用THIS_AS_EXPRESSION而非instance-function；纯implicit结构边/成员receiver不重复诊断。实例function/property按实际角色和owner/subtype规则分别报告禁止与capture，属性accessor归回真实property，不用class级scan或distinct过滤替代正式access消费。CJO按官方FINALIZER bit33及~init身份首次预留/建立唯一CfirFinalizerSymbol/CfirFinalizer，写出/读取/原F/default/owning/lexical/body共用该实体；不在SOURCE decoder补第二header、不冒充constructor、保留Unit/annotations/actual内容。当前这些生产/header尚未实施，不能授finalizer全绿。

S1正式执行表后续精准mapper已落盘 `s1-formal-call-execution-current-mapper.md`，覆盖最末稳定writer点、formal/default/prefix value槽、C extras/pack、函数值callee、owned-copy及SOURCE编码；该产品仍是待建合同。新 `s1-formal-argument-preparation` 真实cjc/运行准备仅写build，不改冻结输入。完整S1/ND-V/O/S/A/T、B5/B6/B7、F1/F2/F6、CJO roots与原70/P0–P10继续；H6排除余额保留分母，v10不阻断，不以原112恢复绿关闭SOURCE。

### 19:27 当前140联合与真实SOURCE新余额

`05da706e` 已DONE0（12m25s，50任务21executed29up-to-date）；原daemon文件快照和实际终态在 `05da706e-compilation.json`，行为0。其后shared expected/source update、scanner独立dispatch新修订的首次 `dc0708e1` 已DONE1（3m31s）：仅resolve的stub包名和不可变smartcast替换接口编译错误，已通过现有impl类与完整copy builder修复，失败原字节在同名compilation.json。`e1374390` 已DONE1（2m29s），生产checker/resolve成功而新测试源码装配失败；测试改用公开symbol查询与Variable.symbol.name，原失败保留。`bb4edc27` 最后实际DONE0（2m41s，104任务4executed100up-to-date），生产与28项真实测试源码均编译成功，原daemon快照保存，行为0；不把编译升级为测试通过。

当前SOURCE receiver合同修订：成功class/extend完成使用正式expected，失败extend按固定K不以失败目标类型强制定型；第二次writer读已发布receiver而不回读raw candidate。Candidate过滤ExpressionStub，并一次更新实际implicit this/inaccessible/smartcast/context leaf来源，局部identity memo保留D/E/context共享与overflow；common invoke内层值candidate在引用产品建立前独立更新。四scanner仅跳过真正已求值lhs，保留独立dispatch与其callable子图。`r23-selected-receiver-gatekeeper.md` 最新19:16节撤销这些旧接缝拒绝，完整S1/ND/F/source与view-session仍OPEN。

新 `CfirReceiverSourceLifecycleTest.kt` 为28个两parser回归，已应用到compiler/frontend/test；21份真实CJ源与七个原CJO的base64/SHA位于testResources/receiverSourceLifecycle。普通BODY源和合法@Frozen的独立选中源分开，各官方被测F/body非零；Runtime实际顺序和完整原cjc诊断/F/E/SHA在r23b-receiver-tests/generation-2/report.md及原流。没有修改已有正确fixture或期待，也没有用手造Info替代正式decoder。

10800选定输入SHA `43ac672350b1c54999d50bd2eed4e7a043faf9b54f295401bff090faee8c998e` 和SDK84已冻结，见r23-selected-receiver-before.json。hidden pwsh PID45200、queue `87a00341` 正在通过原runner联合原112+新增28，原日志r23-selected-receiver-terminal.log，终态进程记录同label。19:27核验PID存活，队列已进入analysis-tests；未修改冻结production/tests/resources，不能重复启动。

frontend已完成分段原XML独立保存在 `87a00341-frontend-partial.json`：106实际/80通过/26失败、4原XML，complete=false，不能计作全140终态。原Parent36/Native16/finally26共78通过，新receiver28为2通过26失败。真正新失败包含同名Cell的RETURN/ARGUMENT类型身份不一致、finalizer种类/重复/缺capture及合法super.field被禁、native finalizer材料化角色缺失、parser非法conditional写目标诊断遗漏。PatternBindingVariable的derived-source断言与canonical Member数量断言还须按固定框架/原CJO独立核证；不能把测试结果直接当修改正确期待的许可。

已按固定N RefExpr/MemberAccess writer和真实nested_other原CJO核证：current canonical access把隐式this dispatch合并成Member，改变裸Ref角色，见 `r24-implicit-instance-source-reference-contract.md`。终态后应在SOURCE投影验证裸Ref的纯implicit对象合同，不能撤掉真实BODY dispatch或吞独立副作用。新只读r24名义身份与finalizer/source-role报告正在准备；S1正式形参序/default前缀值槽当前源码mapper与真实cjc准备也在build进行，不写冻结输入。

下一步等87a00341真实终态，完整封存140原XML/逐case/10800输入/SDK，再从原诊断及固定报告修复类型身份、finalizer、canonical Ref/Member和真实源角色；接续完整S1执行表、ND-V/O/S/A/T、B5/B6/B7、F1/F2/F6、CJO roots及原70/P0–P10。H6排除余额保留分母，v10不阻断；不以原112或新28的任何局部绿关闭SOURCE/目标。

### 18:34 编译终态封存与 R23 后续修订

`87a05620` 已 DONE/exit0（10m26s、104构建任务22executed82up-to-date），generateTree及checkers/serialization/resolve/raw/entrypoint/frontend测试源码编译全部成功，行为0。终态与完整daemon文件原字节gzip在 `build/cjmp-invisible-peer-20261125/87a05620-compilation.json`；该daemon快照含早期构建，不冒称为仅本次stdout，亦没有独立事前manifest。

`r23-selected-receiver-gatekeeper.md` 仍REJECT完整合同，但撤销G1–G5旧具体原因及主analyzer compound/source等同identity的旧原因。新增确定拒绝是extend property函数值外层common invoke被U按内层symbol误投影为extend对象；default tree转换在替换节点后才比较identity；四scanners的compound仍重复地址；终止后的独立receiver/普通children仍被遍历。期望类型、Candidate source更新stub、正式S1/native call/boolean/default步骤表及ND/F/CJO余额继续。

87终态后已修上述四项：现成 `callInfo.candidateForCommonInvokeReceiver` 独立保存外层函数值D；生成器先保存三个原receiver槽，再以原identity选择同一次转换结果；四scanners按lhs→synthetic target/args→write选择同一occurrence，写回不再访问receiver；analyzeExpression/receiverfold在terminated后仅登记不可达assignment，smartcast this求值保持中性。这些新字节不能沿87编译成功授通过。

当前queue `05da706e`、exec47257、wrapper PID55624 正在执行同一generateTree及完整相关编译，尚无终态；先核实际session/queue，不重复启动或修改冻结生产。Dirac正在复审新字节，测试Writer Hypatia仅在 `build/cjmp-invisible-peer-20261125/r23b-receiver-tests` 准备真实CJ/cjc原证据及候选测试，尚未应用为一方测试，不能计作行为执行。旧不可访问代理不联系/恢复。

下一步：等05真实终态并修编译错误，按当前SHA复审；应用审查通过且真实语义已确认的receiver回归候选；重新冻结生成后输入，再通过hidden runner复验原112及新增用例，完整保存原XML/输入/SDK。没有新的SOURCE行为绿；832的112/90/22仍是最后实际行为窗口。正式receiver期望类型/source更新/S1执行表、ND-V/O/S/A/T、B5/B6/B7、F1/F2/F6、CJO roots与原70/P0–P10继续，不关闭目标。

### 18:18 新联合失败封存与 selected receiver 修订

`832fc280` 已 DONE/exit1（11m49s）：实际112项90通过22失败，0 errors/skipped，15份原XML、完整终端、逐case及前后输入封存于 `build/cjmp-invisible-peer-20261125/832fc280-results.json`。10763选定输入前后同SHA `ba8043ea8fa0ea6c3d8c673fc371d0c2cf1d3ce9298e0ef3721649d91c897b49`，SDK84前后同SHA；Parent36/Native16/finally26与LLT2全部通过，SOURCE32为10通过22失败。不能把此前四个失败消失授为完整修复。

原XML证实 `limit/value/input` 函数形参被错误投影为Member，导致E表增加、Reference角色失真：ordinary通用补写层以callableId判断实例角色不成立。原正确SOURCE源码、E/D表数及结构期待全部保留。旧窗口不得被覆盖或改成通过。

终态后已删除ordinary两入口的 `prepareImplicitInstanceDispatch`。固定K `FirCallResolver.kt:556–569` 的unique候选发布合同已核证；本地resolver、completion writer的两个写回点现在共同从selected candidate发布实际对象。ResolveUtils保存class的dispatch、真正extend成员的chosenExtension与函数值invoke的外层dispatch角色，并在成功显式访问中同步同一完成后的对象。candidate pair可能同时有lookup宿主D与实际对象E，禁止全局 `dispatch ?: chosenExtension`。SOURCE reader的成员角色明确排除参数，不以参数callableId授实例权限。

同批修订保留finalizer实际FINALIZER inaccessible物理对象与同源super selector；G4字段非法捕获先核验当前对象，static独立诊断保存实际owner。四状态入口及四扫描消费者按explicit→独立dispatch共享identity遍历；复合赋值先左值读取/地址、再右值、最后写回，写回不重跑已求值地址，删除以相同source替代identity的判定。生成器按真实explicit槽处理super dispatch，生成文件由正式generateTree刷新。

当前queue `87a05620` 正在执行generateTree及checkers/serialization/resolve/frontend测试源码编译，exec82962；尚无编译或新行为结论，先核实实际queue/会话后接续，不重复启动。新只读gate为Dirac（本轮新代理），旧工具句柄已不可访问，不联系或恢复停止的旧代理。当前SHA复审报告路径 `build/cjmp-invisible-peer-20261125/r23-selected-receiver-gatekeeper.md`；旧r22审查及candidate pair附录原样保留。

完整receiver回归还需真实PSI/LT BODY与SOURCE材料化的副作用、other-object、compound和finalizer身份用例。新代码未经行为验证，不授G1–G5整体闭合。ND-V1正式COMMON/default资格、ND-O2-D1 default继承native末边界、ND-S1-R1 unavailable角色/metadata、ND-A1完整输入代际、S1执行表/native短路、F1/F2/F6、B5/B6/B7及CJO root消费继续OPEN；原70/P0–P10未关闭。

### 17:51 当前字节编译与实例环境联合验证

`3e4c8218` 已 DONE/exit1：实际112项108通过4失败，0 errors/skipped，15份原XML与逐case账在 `build/cjmp-invisible-peer-20261125/3e4c8218-results.json`；10763选定路径及SDK84前后无漂移。Parent36、Native16、finally26全部通过；LLT2失败，SOURCE32为30通过2失败。两parser的 `ClassInitDefaultConstructor4` 与 `referenceInstantiation` 均暴露裸实例字段赋值没有物理 dispatch 的产品缺口，原正确源码与期待保留。此前 `74b74ef7` 是86项68通过18失败，其筛选没有执行finally26；不能沿用112预计数。

该终态后已修订四生产文件：ordinary super 从一次选中的真实 receiver 同时取得物理对象与selector owner，并发布super表达式及type ref；已绑定裸成员正式准备dispatch。BodyResolveContext在实际static function/property/field入口选择STATIC_MEMBER，普通局部函数继承定义位置tower，nested错误树入口选择隔离base，缺失slice不再代入regular。SOURCE reader统一THIS/SUPER与bare reference的物理环境，先检查完整owning链的static/header/finalizer限制再沿lexical outer，保留extend宿主身份及inaccessible角色。初始化读写和函数/属性非法访问共用实际receiver身份谓词，对象facts资格置于独立static/访问规则之后。

`a731939a` 已 DONE/exit0（5m23s，104构建任务：6 executed、1 from cache、97 up-to-date），checkers/serialization/resolve与frontend测试源码编译成功，行为0。其事前独立manifest未保存，不将该编译冒称同输入行为验收。旧exec88993已结束，不重复启动。

新 `instance-environment-r22-before.json` 已冻结10763选定输入SHA `ba8043ea8fa0ea6c3d8c673fc371d0c2cf1d3ce9298e0ef3721649d91c897b49` 和SDK84；该有限清单不含docs，不称全工作树冻结。隐藏pwsh进程56120已启动queue `832fc280`，通过原runner联合Parent36、Native16、finally26、SOURCE32与LLT2；原日志 `instance-environment-r22-terminal.log`，终态产物 `instance-environment-r22-process-result.json`。实际数量与通过结论等待终态逐case封存，期间不修改冻结生产/测试。

新只读复审核验当前四文件SHA与G1–G5；另独立固定K/N核证receiver求值顺序及identity去重。不得把旧gate的SHA或局部编译绿授为完整receiver/SOURCE闭合。S1完整执行表、F1/F2/F6、ND-A/V/T、default inherited、B5/B6/B7和正式CJO root消费及原70/P0–P10仍继续，H6排除余额保留分母，v10不阻断授权工作。

### 16:55 最新接续：阶段与 super 接收者

`74b74ef7` 已DONE1（隐藏进程22416终态），19m34s；实际86项68通过18失败、0errors/skipped、14份原XML，完整结果见 `74b74ef7-results.json`。10763路径及SDK84前后同SHA，无输入漂移。原SOURCE32与LLT2全部通过；Parent36为34/2，early_return已通过，两失败换为child_writes_parent_field的child额外继承字段错误；新Native16全部先卡真实T238 `canonical.flow.initializerFields.isEmpty()`。此前把238解释为native role断言不正确，native role在249且也是独立缺口。该轮筛选未命中finally26，不计作它们的新复验。

终态后正式Flow增加必填initializerStage，区分construction执行after、native已静态prepared-before、this-none与unavailable；native非this初始化owner角色保持true，元数据保留，consumer不重复运行静态已准备的initializer。执行字段从完整ordered域中投影实际initializer或既有common默认输入，不把无initializer存储声明伪装成执行步骤；tracked/own/residual全集仍保留。runner已显式补finally26。

super接收者候选已按固定K/N与 `r21-super-physical-receiver-shared-consumer-mapper.md` 接入：ordinary resolver从实际implicitValueStorage的lastDispatchReceiver.receiverExpression写super.dispatch；SOURCE Reader沿record.context、functionContexts与declarationOwners实际拥有/词法边恢复this，Parent selector不改；ClassLike与独立Extend层次分开，别名/static/无实例/循环环境严格拒绝。初始化读写共用物理receiver筛选，super沿dispatch取真实this，receiver缺失不默认授当前对象权限。共享读取筛选在checker第一次编译后才写入，必须由后续完整测试重新编译验证。

`aad92c87` 已DONE0，8m35s，104任务13executed91up-to-date，shared/checkers/serialization/resolve/frontend测试源码编译通过，行为0；不把编译成功授为SOURCE或receiver完整通过。新 `super-stage-consumption-before.json` 冻结10763输入SHA `AF4862A08EB0F05BF3E5C482AB6654BD70A0EE3C49160F099DAD2010EB4758A6` 和SDK84；当前隐藏pwsh进程55236正在执行修正筛选后的相关联合（计划112，实际数待逐case封存）。原stdout在 `super-stage-consumption-terminal.log`，终态写 `super-stage-consumption-process-result.json`。先验证实际进程及queue，不恢复已结束的exec69221或重复启动。

早期四角色在中断后已无法访问，不联系/恢复停止chat。当前新只读gate为Faraday（agent `01a211e9-2d36-7f11-8204-34287b2ff7f8`），审查新的receiver闭环/环境，不写冻结源码。所有S1完整实参表、F1/F2/F6、ND-A/V/T、default inherited、B5/B6/B7、CJO root消费与原70/P0–P10余额继续；H6排除分母/v10待办/正确期待保留。当前SOURCE Reader static/nested/helper/accessor的完整环境与实际consumer需独立复审及正式回归，未授全闭环。

### 15:43 最新状态：取消封存与双产品候选

`48009ee8` 已停止，不再轮询exec59780：daemon60896在14:55明确记录client disconnection、analysis-tests取消及BUILD FAILED in31m14s，wrapper52664已不存在、daemon已idle。原queue仍滞后RUNNING，没有人为篡改成DONE。`48009ee8-cancelled-results.json` 保存两份原前端XML（62实际/60通过2原early_return失败）、原daemon日志/binary/HTML及10762输入/SDK84前后同SHA；analysis HTML仅为取消时7项/1skipped的局部观察，不计完整SOURCE32或LLT2通过。前端partial单独文件保留，不与54窗口重复合计。

取消后S2-R1的混合诊断+termination guard已修复；独立源码审查撤销该原因。双purpose的native静态产品在semantics.api与checker落盘，联合一次发布：nativector静态flow/endFields/firstReturn来源、orderedowner边界/own与parent残余义务，construction正常出口及export字段仍独立。直接qualified field UBI在report之前比较原storage的实际physical声明owner，保留capture/initializer独立规则；不按ClassId名称造关系。显式inherited obligation消费native ctor静态end，own则按已核证finally真实正常出口；正常完成initializer的own字段从初始候选中剔除，default候选同步。

`2d2e3f56` DONE1，6m47s，行为0；semantics.api成功，checker仅staticInitializerFieldInfos的owner参数迁移遗漏3886失败。其daemon原日志和终态保存在 `2d2e3f56-compilation.json`，没有独立事前manifest，不声称同输入编译验收。终态后已修传入actual owner。

新 `CfirNativeInitializationDiagnosticsSourceLifecycleTest.kt` 为8真实cjc场景×两parser，16项；原36/26未改，不手造Info。Source/SDK与新产品10763选定路径事前SHA `AB9D4CBA5DC4A28090F9701B794F855E6F37426590F662828B76E69A38EDE633` 在 `native-static-diagnostic-before.json`。当前隐藏pwsh进程22416通过 `run-owner-validation.ps1` 执行完整相关联合，原日志持续写 `native-static-diagnostic-terminal.log`，终态会写 `native-static-diagnostic-process-result.json`，尚无行为通过结论。先查该实际进程和队列，不因工具session缺失重复启动。新runner仅生成UTF-8运行证据，不修改源码；终态前不改冻结10763路径。

独立ND-A1输入代际、ND-V1 ctor资格、ND-T1 this委托失败来源仍OPEN，ND-O1/O2的最新修订待复审与实跑。own正常出口使用已获Authority核证的finally适配，不能为对齐N旧Sema首return缺陷修改正确26项期待。新Authority见 `r20-native-static-boundary-finally-own-default-prefix-slots-authority.md`；S1完整writer/default/prefix/owned-copy/ordinary-native映射见 `r20-s1-completion-writer-owned-copy-mapper.md`。正式执行表、F1/F2/F6、B5/B6/B7、CJO消费根及原70/P0–P10其它余额继续实施。

`54dd6b31` 已 DONE/exit1：四份完整原 XML、实际64项62通过2失败、0 errors/skipped。finally新26全部通过、父初始化36为34通过2个原early_return级联失败、原LLT2全部通过。10762选定路径前后同SHA `05D15DD5718BD65F04EA4490B6729AAB92E75BDEE7E3EDC3B245D632860EC4F4`；正确aarch64 SDK的84个CJO/cj.d前后同SHA `7BBBBA79757F4588EBE280B79B57C86D50740264AA090DE853311DD0B07C2F6B`（本轮有序JSON算法，不能与旧不同算法摘要直接等同）。完整逐case账和原gzip XML见 `build/cjmp-invisible-peer-20261125/54dd6b31-results.json`。

先前 `71617bd1` 的命令漏掉SDK路径的build-tools层：生产及测试编译通过，实际64项2通过62前端前置失败。其四原XML/逐case/同10762输入已单独封存在 `71617bd1-results.json`，不计作正确SDK下的代码回归，不与54窗口合并。

54终态后已修改S2不可达tail/positional分类、F5-R1实际do-while终止condition字段状态，以及S3共用tree `CfirExpressionUtil.delegatedConstructorCallOrNull`。resolve与checker仅穿过Named/Inout语法角色，Optional/OptionalChain不穿透，保留原body/call身份；两端旧私有wrapper规则退休。独立审查撤销F5-R1/S3旧原因，S2的混合diagnostic+termination break仍为S2-R1待修，不能授完整S2/B3/R20。`48009ee8` 正在联合finally26、parent36、SOURCE原集合及LLT2验证新字节，exec session59780；事前10762路径SHA `C9DB6F332325F6201A719A184D8B7BC649221726993D355C02F1A8AEF4B78770`，见 `staged-terminal-s3-before.json`。终态前不改冻结生产/测试，不取旧XML计新结果。

下一框架输入已经由固定K/N独立考证：`r20-catch-exception-resources-handlers-framework.md`、`try-resources-handlers-f1-f2-f6-authority.md` 和 `r20-early-return-parent-obligation-cascade-authority.md`。early_return不采用parent-error统一抑制或super(false)特化：直接super字段UBI有跨actual owner资格；child继承completeness域来自父NativeStaticDiagnostics边界。需与ConstructionCompletion正常出口数据独立保存，parent canonical definite=false/possible=true与hasNormalExit=true均保持。F1/F2真实catch alternatives、异常产生点/类型域/顺序路由及ordinary/native共用consumer，F6资源cleanup/handler effect完成产品继续实施；静态诊断所有body的检查责任不能冒充真实构造出口。

S1新增固定证据 `r20-s1-named-argument-execution-framework.md` 纠正旧“named求值顺序未规定/统一放弃分析”注释：N成功声明调用的desugarArgs由形参序生成，SOURCE也保存该执行序。当前CFIR resolved arguments/mapping按K合同保持源码序，不应改其含义；须在成功completion writer后另冻结正式有序execution steps，包含显式/Inout、default helper及前缀值槽、packed vararg/C-extra、applied target/ordinal/epoch。SOURCE writer与ordinary/native调用分析共用它，default不能重跑前缀源码表达式，named不能只返回原入口状态。该输入也是下一NativeStaticDiagnostics与F2调用异常消费的前提；错误/未准备表不授正常返回。所有原正确无诊断/phase/source期待保持，不能仅在checker按名字排序而留下SOURCE旧序。

当前 `48009ee8` 在14:43后仍RUNNING：tree/checkers/resolve及两raw producer实际编译已通过，frontend测试编译阶段尚未给新行为结果；exec59780继续轮询。不要把54窗口的finally26绿或本轮compile成功写成96项行为通过。终态后先保存此轮完整XML/逐case/after manifest，再修已明确S2-R1的`countChanged && !terminated`短路，使同一实参诊断＋终止后的剩余分类先完成；接着实施上述正式执行表与独立诊断产品，保持全部其它余额。

main b44边界现已确定：本ordinary explicit-common peer固定N NeedCheck/NeedRecheck与imported nominal target资格均false，宿主1.1.3同explicit-common实际六条与fixedN规则有差异。原expected6保留；完整消费须另有明示的宿主行为合同及完整正负root选择验证，不伪造flags、不无条件扫描Library。详见 `b44e7836-fixed-n-root-selection-authority.md` 和更新的 `imported-peer-recheck-authority.md`。原70/P0–P10、H6排除余额分母、v10待办与全部历史修改保留，目标active。

唯一可写树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70/P0–P10、H6排除分母、v10待办和全部历史修改保留，不联系停止的chat，不归档、不提交、不关闭goal。固定K为 `3c7a5041f238102285764d370665f56f587f9dc4`，N为 `14276740ffec1262b81b6b5236b1d840af270b37`。

## 最新已封存行为

最新 `b44e7836` 已DONE1，12分8秒，20原XML/203实际case199通过4失败、0 errors/skipped，证据 `build/cjmp-invisible-peer-20261125/b44e7836-results.json`。10761选定输入前后同SHA `29F24C5EEAC1DD1C1F03EF1621E1C5EC734FB9AD2EBA9C8CE2CB9A9A465AC233`，无选定输入漂移。Source32全部通过、原LLT2通过；新Source父初始化36为34通过2失败；正式SOURCE/CJO16为14通过2失败；copy/S0/provider/peer其余选定回归通过。实际case数来自完整逐key ledger，不按计划估计相加。

四失败是两parser `early_return` 额外报child的CLASS_UNINITIALIZED_FIELD与USED_BEFORE_INITIALIZATION，正确宿主期望只有parent ctor字段错误；以及两parser CJO property仍缺peer三条binary错误。不能把parent initialized状态退化成最后表达式true，也不能靠删child/peer期待通过；需处理实际父义务/diagnostic cascade和正确root eligibility。R20 B1–B7仍保留；203/199不关闭完整目标。当前**没有活跃Gradle run**，下一工作从上述实际失败与独立拒绝继续。

main CJO属性证据已在b44结果的 `boundaryEvidence` 与原XML stdout封存：Box/peer raw COMMON/SPECIFIC/IMPORTED=false、FROM_COMMON_PART=true；实际Box相同，Object/Any COMMON/SPECIFIC=false、IMPORTED=true、FROM_COMMON_PART=false。实际fixed parent边Box→Object→Any、无missing。Authority已收到这两parser main数据，需给fixed N明确选择规则与普通CJO/explicit-common加载场景区分；原6 keys/notes/ranges未改，不无条件遍历Library。宿主两阶段六条与fixed N/load模式差异单独保留。

`21f22c13` DONE1，11分45秒，16原XML/98实际case94通过4失败，0 errors/skipped。SOURCE32全部通过，其中原14全部通过；providers27与peer facts21全部通过；正式frontend16为14通过2失败；LLT2均失败。10758选定输入前后同SHA `106F8BC428ED1E480A7991F5309A9A8C8A88BF8B7DC2024008AE0467DA90F929`，完整manifest、gzip原XML和逐classname+case在 `build/cjmp-invisible-peer-20261125/21f22c13-results.json`。不将这些限定结果计为原70/P8/E6/G1/S1–S7关闭。

SOURCE canonical六项旧Unit失败已从正常解析完成路径修复：委托callee第二次访问保留已完成结果，不把它改成Unit；首次调用也保留callCompleter成功/错误结果。pure arity分类包含原生WrongNumber/WrongArgumentCount，隐式super无参失败正确报NO_NON_PARAM并保持B名称锚点。R13 public self与internal parent lookup视图分离，moved两项和同名General roots的所有flags/actual parent测试本轮全部通过。R22 this分离receiver owner与wire declaration target，builtin复用精确Ty::IsBuiltin predicate；两parser CJO函数正文/身份/映射的六项通过。上述专项静态核准及限制见已有invisible-peer接续与build报告。

四失败为：两parser的CJO property还只报当前三条，缺原peer三条binary错误；两parser的ClassInitDefaultConstructor4仍额外报父字段a的CLASS_UNINITIALIZED_FIELD。正确期待全部保留。

## owner 完成数据候选

新增共享语义数据 `cfir/semantics.api/src/org/cangnova/cangjie/cfir/declarations/CfirClassInitializationInfo.kt`，tree仅有 `CfirClassInitializationInfoKey.kt` 管理key/owned-copy所有权。数据按实际owner和checker session保存，记录actual members、parent infos、actual ctor入口/正常出口/terminal状态/availability/this-super target/physical receiver与接受的common→specific fieldAliases。正常出口definite取交、possible取并；空正常出口没有制造全部initialized=true。

`CfirInitializationCheckers.kt` 复用完整流分析器计算父先完成与各ctor结果；表达式不再发布实例字段INITIALIZED，this调用不再blanket mark全部字段。Ctor completeness改为消费完成normal-exit数据；类默认字段检查消费owner最终数据。`CfirExportAnalysisInputSnapshot.kt` 分开冻结physical originalFacts与logical-owner初始化overlay；owned-copy按精确key清除旧分析产品。当前不是完整R20关闭。

`e20d3951` 5分5秒compile失败，行为0，仅CfirFunctionSymbol→CfirConstructorSymbol类型边界错误，已用显式constructor种类修复，记录 `e20d3951-compilation.json`。

`e078b9b2` 5分35秒，实际38项2通过36失败，3原XML封存 `e078b9b2-results.json`。**原LLT2已实际通过**；Source父初始化新36项全部卡在class数字phase>=BODY断言183。后面的字段/ctor/顺序/diagnostics细化断言没有因此获得通过。该轮没有独立before manifest，不声称前后同输入。

新测试 `CfirParentInitializationSourceLifecycleTest.kt` 是18场景×两parser，21个输入SHA按实际cjc保留；字段、source文件、真实body、返回/throw、多ctor、父后声明/跨文件/根顺序、private字段与child自身字段全部独立验证。语义与原JSON记录在 `r20-parent-initialization-source-writer-authority.md` 和 `r20-parent-initialization-source-writer-cjc.json`。

## class 正常完成发布

固定K CLI不承诺逐class数字resolvePhase；其class exit负责内容/CFG，LL负责真正phase publication。CFIR已采用逐声明完成根提交，所以新增actual whole-class完成事件接到既有 `publishCompletedBodyRootPhases`：仅full BODY、无候选/外部推断、真实bound root成功退出；partial/designation不授权whole class。事件在外层body活动时延迟，正常根visitor只消费有事件的class与其真实函数/default/property accessor子域。无自身body方法仅在完成class权限下遍历已完成default/helper，不伪造body/CFG；class CFG是否保留是独立策略。replaceResolvePhase保留InProcess/unlock协议，没有finally或无条件标phase。

映射/实现专项续审见 `class-body-phase-publication-framework.md`。原Source36 phase期待未改，后续必须真实走过全部产品断言。

## R20独立拒绝仍须继续修

`build/cjmp-invisible-peer-20261125/r20-owner-info-gatekeeper.md` 的B1–B7不能因LLT2绿提前关闭：

- B1：pre-delegation实参→target完成/终止→post-delegation body须同一分阶段流；当前post状态还提前用于整个body。
- B2：assignment classification仍有旧入口，须与读取/出口同一ctor计划。
- B3：return正常出口必须经过finally；finally throw须替换退出原因。
- B4：真正unavailable ctor不能被排除后仍给parent完整initialized证明；需区分合法common header与未完成/失败。
- B5：Source默认实现和per-ctor eligibility须正式语义产品，不读自己上轮INITIALIZED；imported header是另一合法来源。
- B6：当前身份guard未完整封存原位语义更新/epoch，需要sealed generation或完整输入指纹。
- B7：外Source模块不能写physical字段；已恢复moduleData写权限guard，但导出view会话选择仍须完整实现。

foreign/interop eligibility另为OPEN-PROOF，不猜诊断、不用初始化默认值兜底。copy的key清除、originalFacts/overlay双轨、父先完成方向和immutable嵌套数据均保留。

## CJO root资格证据

宿主cjc 1.1.3实际两阶段property：common exit0、specific exit1、六条errors；原argv/JSON在 `cjc/two-phase-property/`。fixed N对宿主payload＋规定explicit common load的静态推导为三条，差异保留，未运行fixed N binary。**宿主payload/SDK不能代替main actual classifier**。

`box-target-parent-boundary-authority.md` 要求main真实Box/peer raw attrs及Box自身与Object/Any actual parent闭包，最后才判NeedCheck/NeedRecheck/imported nominal资格。Writer已只给原CJO property失败description/stdout补 `CJO_PROPERTY_BOUNDARY_JSON`，保留全部原6 keys/notes/ranges、不写额外记录、不改生产。下一次正式frontend原XML将提供main实值，不能从源码没写common或父名Object推断flags=false。

## 当前正在运行

本节以下为b44开始时历史记录，已由顶部DONE结果替代。后续不要恢复已经退出的exec32406或重计旧XML。当前实现候选全留盘，待从顶部4失败和R20B1–B7继续修复/真实验证。

`b44e7836` 联合复验正在RUNNING；main exec session32406。事前10761输入封存 `owner-phase-before.json`，SHA `29F24C5EEAC1DD1C1F03EF1621E1C5EC734FB9AD2EBA9C8CE2CB9A9A465AC233`。包括新owner语义数据/key与Source父初始化测试，尚未计行为通过。Tasks含owned-copy、S0 snapshots/payload、provider scope/index、peer facts、Source36与正式CJO16、SOURCE32和LLT2。

待terminal后先封存原XML/逐case/after manifest，再改生产。若context接续时仍RUNNING，继续当前session/queue，不能启动重复窗口或拿旧XML计新结果。PowerShell7、UTF-8读取、apply_patch写文件、所有Gradle通过queue；环境沿已封存窗口，配置JAVA_HOME21.0.10，实际复用daemon21.0.9差异已注明，不冒称重新启动了daemon。
