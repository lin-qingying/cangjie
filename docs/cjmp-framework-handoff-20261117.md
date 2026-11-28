# CJMP 实施交接记录（2026-11-17）

## Draft PR #17 与主分支集成（2026-11-28）

PR 分支起点为 `92928bd070fb00c06e956237e6c394efa7b9a0ee`，合入的主分支为 `200142275d824efcb4d24f5f835e0376b0a357ad`，共同基线为 `6b6573fd994b1f813796b3ff809ac26b745a216f`。初始 332 个冲突路径，含 317 个双方修改、6 个双方新增和 9 个删除与修改。85 个初始冲突路径为生成文件。全部初始冲突已处理，进入编译与行为验证。[Draft PR #17](https://github.com/lin-qingying/cangjie/pull/17)

| 冲突范围 | 初始路径 | 已解决 |
| --- | ---: | ---: |
| CFIR 树、生成器及生成文件 | 102 | 102 |
| CFIR 其他模块 | 128 | 128 |
| Analysis、低级 API 及反编译 | 55 | 55 |
| Compiler、PSI、CHIR 及其他模块 | 47 | 47 |
| 合计 | 332 | 332 |

Linux 本地验证环境为 Ubuntu 22.04.5、Gradle 9.4.0、Temurin 21.0.13+9、Java 工具链 Temurin 17.0.21.1+2、仓颉 SDK 1.1.0（Linux x86_64 cjnative）及 LLVM 14.0.0。SDK 实际 `std.core.cjo` 的 SHA-256 为 `b1d655e54e4dd27b88ffc3cd62394c36ea7afd222916b73227efa74e4503d452`。CI 发现了 20 个 Linux SDK 变体。首次推送 `21c16e5cb4997b6d92e896293c1fba2342a8b150` 后，获得授权规范化七个提交标题，更新为相同文件树的 `781891ab8db43d3f29c76d095a2170678ee569c5`。后者的 GitHub 提交标题检查、SDK 发现和 LLVM JNI 构建均通过。[Linux CI 运行](https://github.com/lin-qingying/cangjie/actions/runs/46134396153)

| 验证窗口 | 实际受测提交 | 已确认结果 |
| --- | --- | --- |
| Linux 定向第 4 轮 | `d23cc86fd6c6466b0c42ee7bb85c0d4237fd2ec3` | 已报告 185 项，124 通过、60 失败、1 跳过。Frontend worker 在宏 token 循环失去进度后被停止。CJO 受 Analysis API 编译阻塞 |
| Linux 定向第 6 轮 | `21c16e5cb4997b6d92e896293c1fba2342a8b150` | 执行 223 项，201 通过、22 失败。Providers 84/84，导出输入快照 64/64。PSI、Resolve 与 CJO 的测试编译进入下一轮 |
| Linux 定向第 7 轮 | `39950914612e1ee6bc19d16c893c296d1fa7d0d4` | 执行 550 项，501 通过、49 失败。Providers 84/84、Resolve 106/111、Frontend 57/69、PSI 254/286。CJO 测试编译卡在 FullId 夹具的断言函数名，已在后续提交修正 |
| Linux 定向第 8 轮 | `167f90ae7062190870a14f72f3caeb12f7360c07` | 执行 347 项，289 通过、58 失败。Frontend 48/48、Providers 76/76、Resolve 85/86、CJO 80/137。Frontend 与 CJO 本地测试 worker 使用 2 GiB 堆 |
| 本地主分支回归 | `02f270f4bc405a091fefaff852f045fa2592c92c` | 已启动 `test --continue`，使用相同的 Linux 工具链及真实 SDK。首次尝试在执行测试前结束，两个输入各有独立日志 |
| GitHub 主分支回归 | `39950914612e1ee6bc19d16c893c296d1fa7d0d4` | SDK 发现及 LLVM JNI 构建通过。首批 SDK 作业已进入 `Run main repository tests`，后续推送触发新矩阵。[运行记录](https://github.com/lin-qingying/cangjie/actions/runs/46134914684) |

第 7 轮实际验证了三项 SOURCE FullId、导入泛型正文、泛型 enum 配对及 payload 不匹配负例、两种前端的配对注解和源位置。后续补丁在隔离运行中通过 46 项宏构造与告警测试、49 项异常及 if CFG 测试，以及 73 项宏转义和词法测试。完整提交的 Gradle 复验继续进行。

第 8 轮的限定定义快照 11/11、SOURCE 包图 10/10、来源身份快照 8/8、异常及 if CFG 49/49。一次 JVM 类初始化循环影响了 22 个 CJO 测试。改为按需读取同一真实解析器后，单独复验了三种类加载顺序，初始化依赖 suite 通过 10/11。原 R41 Core 用例仍在 `metadata.imports.isEmpty()` 失败。FullId 的正常读取、隔离作用域、重入、精确路径及不同 manager 的八个原用例已通过补丁覆盖层的隔离复验。CJO SOURCE 保存的实际声明根与公开 wire binding 继续按各自身份验证。

合入前 `200142275d824efcb4d24f5f835e0376b0a357ad` 的 GitHub 矩阵 20 个 SDK 测试作业均失败。同版本 SDK 1.1.0 作业记录了 12 个失败的 Gradle 任务，已保存具体测试名及编译错误用于对照。[主分支基线](https://github.com/lin-qingying/cangjie/actions/runs/46109990741)

第 6 轮已复验 R42 alias 装配。R41 的 Core 定义输入失败仍有原测试入口 `CfirInitializationDependencySerializationTest.stdCoreProducerInjectsProviderBuiltinsAsPackageRoots`。该用例需要作用域内的真实当前定义产品。已记录的 CJMP 历史分歧包括普通有界 extend 的私有成员导出、Common 静态调用初始化器以及 Common const 和 tuple 的 CJO 槽。SDK 1.1.0 的独立消费端实际执行 Common 静态初始化器，且输出随 specific 对应函数更新。原始断言继续保留。

原有的 common extend 重载余额从 `CfirCjmpExtendKey.sameKeyAs` 的真实 nominal counterpart 和 `ResolveUtils.expectedExtensionReceiverType` 的逻辑接收者继续。隔离实验已经观察到两原例通过，并保留同名但无真实绑定的 fixed symbol 负控。完整双向分组、泛型头及低级 API 验证继续按 [11 月 24 日 Source Owner 接续](cjmp-source-owner-continuation-20261124.md) 实施。

本地原始日志在 `/workspace/work/cjmp-pr17/validation`。每轮保留实际 SHA、参数、工具链与 SDK 校验值、完整 Gradle 日志、退出码和该轮生成的测试 XML。隔离的枚举、注解、同签名冲突和导入正文复验另有日志。Analysis API 使用 `CaValueParameterSymbol.correspondingField: CaFieldSymbol?` 表达主构造器 `let`／`var` 产生的实际成员字段。原 main 的 `correspondingProperty: CaPropertySymbol?` 调用方已迁移。

原 70 项及 P0–P10 保持开启。沿下文的实施入口继续，重点是 P8/S1–S7 的 PSI 与 LightTree SOURCE → CJO → 正式消费端、限定定义快照、真实 Core 定义输入及作用域内 SDK 选择。保留 R41 Core 和 R42 alias session 的复验入口及原测试断言。

> R41最新完整终态：[证据记录](../build/r41-continuation-evidence-20261128.md)、[真实结果](../build/cjmp-invisible-peer-20261125/r41-final-source-publication-20261128-a-results.json)。fbc812ff完整529/528/1，Source18/providers52/原FullId1通过，全部新Work6/原context publication7和原图五失败通过；54原XML/流归档已解压与磁盘长度/SHA核验，输入/SDK无漂移。唯一原Core定义输入失败继续实施真实当前定义产品/scoped SDK选择，禁止fake Object/Any、包名current投影或body=0。原70/P0–P10及完整SOURCE/其它验收余额保持OPEN。

> R41最新实际接续见 [证据记录](../build/r41-continuation-evidence-20261128.md)：R1/R2 b166/166、原owner首次capture负控和Owned未选注解拒绝通过；原XML/流全部核验、限定v2 gate接受。完整final SOURCE publication/唯一Work.freeze生产已应用，编译ba6a8c6f退出0；新Work/graph回归与完整gate待核。R40 a原514/508/6全部保留，Core1与图五项不能按编译关闭；原70/P0–P10完整OPEN。

> R38最新[接续](cjmp-export-preparation-continuation-20261128.md)：g真实完整514/479/35，Source18/providers46/FullId1通过、全suite报告/无漂移；33未选header缺原context完成路径、1新增alias请求计数断言和Core1仍失败。e124只是已报告子scope，fSource18陈旧；验证脚本已修逐task rerun/选择器任务归属。下一R40 actualPublished content owner header能力，不以已完成产品重复请求或session-global provider兜底；原70/P0–P10完整保留。

> R38最新[接续](cjmp-export-preparation-continuation-20261128.md)：e真实124/124、Source18/FullId1/providers45/header60通过，input/SDK无漂移；静态v3/context接受。新增serialization7旧选择器错绑provider未执行，已修脚本；shared constant双session回归已接入，f完整serialization联合窗口待核。下一Core真实定义产品映射在build/r39-core-definition-product-mapping.md，原70/P0–P10仍OPEN，不以局部绿关闭。

> R38最新[接续](cjmp-export-preparation-continuation-20261128.md)：b真实120/118/2，原FullId通过，SOURCE两个operatorsConversions仍报APILevel缺准备类型；9851输入/SDK84同SHA、完整XML与流已核验。第一gate因混代类型R1拒绝；最终签名/消费域/完成后边重检和11项回归已实际继续修订，c新冻结与v2审查待核。原70/P0–P10及完整SOURCE范围保持OPEN。

> 最新[导出输入准备接续](cjmp-export-preparation-continuation-20261128.md)：原FullId在新冻结窗口真实61/60/1，Any的annotations组合比较失败，输入/SDK无漂移；共享header/type/canonical准备、接口分类和初始化依赖生产已前移，最新50d33f9d行为待核。原70/P0–P10完整范围及全部余额保持OPEN，不以首批编译成功关闭整项。

> 最新[parser/annotation真实实施与验收](cjmp-parser-annotation-continuation-20261127.md)：公共parser v2七文件已合入；r36-b完整1156/1127/29，SOURCE18、Receiver28全部通过，PSI668仍有28失败。flag-only annotation context与官方证明的ParserSeed缺体消息已修，r37-a475/474/1，原annotation2/2、Seed13/13通过；新增文法/错误事务22项修装配后r37-c464/463/1，22/22通过。全部Source/SDK无漂移、原XML/流已核gzip/SHA，唯一serialization余项为真实当前Core定义输入。当前无行为冻结；原70/P0–P10及其余PSI25/TwoPhase14余额不关闭。

> R35最新真实终态：完整alias/source窗口506/504/2、SOURCE18/copy46全通过，Source/SDK无漂移；alias定义环/有限泛型嵌套获限定ACCEPT，修新增this模型断言后原XML确认定向13/13、文档检查通过。公共assignment parser七文件61258字节候选已落盘未合入，worker因rateLimit停止、独立gate待核。[当前实际代码、证据和下一实施](cjmp-alias-parser-continuation-20261127.md)优先于下方旧RUNNING。原70/P0–P10及Core/TwoPhase余额保持OPEN。

> 最新[alias与赋值文法接续](cjmp-alias-parser-continuation-20261127.md)：r33-b真实完整646/624/22，frontend146全部报告、无worker异常，SOURCE18/copy46/finally26全通过；原22失败及Core定义输入余额保留。r35已实际修引用联合资格、被动Alias和定义环/有限泛型嵌套，编译22e7740a成功；exec53250新冻结behavior待核。公共assignment parser完整候选正在准备，未合入；原70/P0–P10不关闭。

> 2026-11-27完整性更正：r30-d原stderr也有frontend worker堆耗尽，505/466/39是已报告计数，不能指完整矩阵。当前编译JUnit方法92+28+26=146；r33-b显式2GiB窗口d0aa1d09/exec27076正在执行，Source9847/SDK84冻结。完整FullId/BODY/Primary修订已编译；新raw matrix有JUnit3断言重载问题，std.core还需真实当前Core定义输入，R-role需类型+真实target联合资格；具体报告及候选在[最新记录](cjmp-source-consumer-repair-20261127.md)，原70/P0–P10保持OPEN。

> 2026-11-27 r32-a真实544/522/22，SOURCE18含原14及复制46全通过，finally26因worker堆耗尽缺XML，完整矩阵未闭合。r33已应用完整FullId字段、Primary正式Object消费、Initialization真实缺失BODY setup、body0首F筛选和super constructor角色修订；82377cee编译/文档验证exit0，新行为待核。[最新实施与证据](cjmp-source-consumer-repair-20261127.md)优先于下方旧状态，原70/P0–P10保持OPEN。

> 2026-11-27最新磁盘核对和修订见[正式消费修订](cjmp-source-consumer-repair-20261127.md)：r30-d完整505/466/39已封存，r31测试源码编译成功；r32 typed selector/显式package/annotation owning修订限定接受，VArray原slot独立冻结与编码已接入、编译984a0421成功，行为待新冻结窗口。初始化完整候选尚未应用，实际eager成员阶段缺口继续按固定K考证；原70/P0–P10不关闭。下方“84a8d61d仍待核/新字节行为0”等均为历史时点。

> 最新[B1/B2正式消费接续](cjmp-formal-consumer-continuation-20261126.md)：pure registry/请求缓存前授权、队列原scope、BoundContexts与decoder选择、selected/aliases同根目录及normal header升级已接生产；单F provider、显式header/default/initializer/body frontend能力已改，三cold caller与两callback依固定authority迁移。G1/G2/G3有界旧拒绝已逐项修订，最新84a8d61d/exec54046编译仍待核；新字节行为0。下一核真实编译→冻结FormalConsumer+SOURCE+copy验证，再完整正式消费门；旧r28-d498/481/17不替新字节。原70/P0–P10保持OPEN。

> 2026-11-26最新[copy metadata接续](cjmp-copy-metadata-continuation-20261126.md)：d真实498/481/17，SOURCE18含原14与copy/Published46全部通过，完整serialization434/417/17；51原XML逐份gzip/SHA/字节数核验，两原终端流及9845输入/当前SDK84同SHA封存。C1/C2复制冻结与父Default静态环修订获有界PASS，resolve测试源码已编译；Iterator按SDK真实abstract class纠正新增错误断言，正式IMPORTS前置已接。余17为7个BODY driver/真实外包消费前置、4 eager header/4声明完成生命周期、missing-target与VArray raw各1。旧a/b/c与日志包装器恢复限制保留，无活动本轮Gradle。下一直接按r28完整mapper实施B1/B2正式consumer，不以局部绿关闭原70/S1–S7或Owner/alias/诊断余额。

> R26当前终态[接续](cjmp-owner-initialization-continuation-20261125.md)：e61/61；f完整serialization434/424/10、45原XML与9844输入/本轮SDK46前后同SHA已封存，exec60930已退出，不再按运行中轮询。正确body initializer Return已通过原回归，旧“一律null”已按固定N纠错。余10为4 SourceParent/RHS正规前置、4 eager header/index、1 missing-target期待、1 VArray raw-save；先读新Return authority、full-failure partition及read-boundary gate后直接继续B1/B2完整正式消费迁移，保留所有正确期望/原始失败。Source fixture生命周期与D/E原槽语义在本轮open代理只读准备，未改原测试；normal binder/provider/decoder boundContexts/Published/aliases仍OPEN，原70/P0–P10和140/120/20余额未关闭，无活动本轮Gradle。

> R26真实initializer Return最新[接续](cjmp-owner-initialization-continuation-20261125.md)：固定N已证body内local initializer保留原F，先前“一律null”概括错误；主线程生产已纠正并保留原正确回归，e新字节61/61、9844与本轮SDK46同SHA/三个原XML封存。完整d模块432/421/11已按4 SourceParent/RHS前置、4 eager读取、1 Return、1 missing-target期待、1 VArray raw-save分域，不改正确测试或弱化守卫。当前f完整serialization新冻结（exec60930）仍运行，终态待核，禁止改输入。B1/B2正式binder/provider/decoder boundContexts/Published/aliases全迁移仍OPEN；原70/P0–P10及140/120/20余额保留，不能按61局部绿关闭。下方历史initializer-null结论以本条固定N纠错为准。

> R26 typed/保存槽最新[接续](cjmp-owner-initialization-continuation-20261125.md)：type/FullId/common-policy/value需求、borrowed角色/原F与metadata固定点、TP最终bounds来源、完整classifier member headers、e/d/P实际slot及G1–G4修订已继续实施。a窗口真实44/43/1，9844输入前后同SHA与两份原XML封存；唯一新测试把typed错误要求到create拥有阶段，已按真实阶段修订，正确SOURCE期待不变。最新slot/annotation gate和附加回归仍准备中，全部新字节尚无行为通过；normal binder/provider/decoder boundContexts/Published/aliases尚未迁移。四parser负例已独立确认正确，不能改期待。当前无活动Gradle，原140/120/20及70/P0–P10保持OPEN；先新冻结验证与slot gate，再完整正式消费迁移。

> R26拒绝修订最新[接续](cjmp-owner-initialization-continuation-20261125.md)：完整context/occurrence比较、独立owning/lexical环与typed参数/helper/TP槽、helper正文隔离、不可变增量registeredHeaders已继续实施，43995f90实际8/8及原XML保存。R1/R2/R8typed需求/借用绑定/广测试、新字节复审及normal consumer/decoder/Published/alias原子迁移仍OPEN，不以8例关首组件或Owner6；无活动Gradle，下一按两份request拒绝报告继续，原70全范围保留。

> R26第二组件最新[接续](cjmp-owner-initialization-continuation-20261125.md)：真实保存槽request三域及纯header seed直接e下行构建已实施，f9df0f0a实际7/7，原XML/daemon保留。normal provider/旧全包ownership索引/decoder boundContexts/aliases目录与default-annotation Published仍未迁移，不能把7例绿计作Owner6或原20失败闭合。下一完整绑定/能力/发布/caller迁移按r26 mapper和新request审查继续；无活动Gradle，原70/P0–P10全范围及历史修改保留。

> R26最新首阶段见[接续](cjmp-owner-initialization-continuation-20261125.md)：纯HEADER_REGISTRY/CONTENT能力和不读e的header索引已实施，三个官方CJO能力隔离用例bf41c934实际3/3，原XML/daemon保存。正常消费者仍未迁移旧全包ownership索引，不用三例绿关闭R26/六个owner失败；下一完整request三域/header ancestry/读范围/alias原子发布/default-annotation能力按mapper实施。无活动Gradle，原140/120/20及全部70/S1/ND/F/parser/CJO余额保留。

> 2026-11-26 01:42最新[owner/R26接续](cjmp-owner-initialization-continuation-20261125.md)：337f29df完整140为120通过20失败，原112/SOURCE32全通过，16原XML/10800输入/SDK84同SHA封存。mandatory FuncBody、finalizer完整签名/参数布局及MessageFormat this引号已修，6cb9207c编译过，行为0。剩余选中正文root F/owner上下文与alias视图完整合同已考证到R26 mapper，下一main实施三域request/header registry/loader/decoder/原子aliases/provider与按需内容能力，禁止全包/名称兜底或root特殊修补。当前无活动Gradle，不重复旧DONE/CANCELLED窗口，原70/S1/ND/F/parser/CJO余额继续。

> 2026-11-26 01:18最新[owner/finalizer接续](cjmp-owner-initialization-continuation-20261125.md)：R24已确认取消并封存XML0/无输入漂移，不重复旧RUNNING；真实FINALIZER位/首次CJO Finalizer header、qualified双诊断/裸this及SOURCE binding拥有链修订已落盘，c596/43c596/a3cb编译成功，完整行为0。新queue337f29df/PID16072在10800输入/SDK84冻结下再验140，等待真实终态封存，不能改冻结源码。source位置、parser/S1/ND/F/CJO/原70余额继续，原112绿不关闭全链。

> 2026-11-25 19:50最新[owner/SOURCE接续](cjmp-owner-initialization-continuation-20261125.md)：87a00341完整140为114通过26失败，原112/SOURCE32全部通过，16原XML及10800输入/SDK84同SHA封存。终态后已修constructor重复tag身份与canonical裸Ref/Member语法投影，并加强错误binding-source断言而保留所有正确语言期待；0ec85909编译过。新queue1c620477/PID41788正在新冻结窗口再验140，不重复启动或改冻结源码。下一完整finalizer语义/首次CJO header、source位置验收与S1执行表/ND/F/CJO/原70余额按接续实施，目标不关闭。

> 2026-11-25 19:27最新[owner/SOURCE接续](cjmp-owner-initialization-continuation-20261125.md)：bb4edc27生产与新增28真实receiver测试编译成功，后续queue87a00341正在联合140（PID45200），10800输入/SDK84已冻结。前端分段原XML为106/80通过26失败，不是全140终态；新失败和SRC Ref/Member投影、名义身份、finalizer及source-kind核证继续，原正确期待保留。87的完整原XML/输入封存需等实际终态，不重复启动；最新进程/文件入口及全部原70/S1/ND/F/CJO余额按该接续继续。

> 2026-11-25 18:18最新[owner/receiver接续](cjmp-owner-initialization-continuation-20261125.md)：832fc280真实112项90通过22失败，原15XML与同10763输入/SDK84封存；Parent36/Native16/finally26/LLT2全通过，SOURCE32为10/32，形参被错误附加Member的回归保持正确期待。终态后已删除ordinary最近receiver补写，resolver unique与writer共同发布selected class/extend对象，统一槽遍历、compound地址一次求值及finalizer物理边，generator已正式刷新。87a05620正在完整相关编译，exec82962；先核实际终态，不重复启动。新源码复审及真实receiver回归候选独立准备，完整SOURCE/70/P0–P10保持OPEN，ND/default/输入代次与CJO消费余额继续。

> 2026-11-25最新[invisible peer接续](cjmp-invisible-peer-continuation-20261125.md)：完整声明输入、四表/own前步骤、父header prepare捕获、独立返回上界、eligible序列及正式requirements候选已应用。旧e37ab397 scope15为14/1，正确RAW期望保留并修；2c9adade生产编译过，bf022ade正在新8/scope15/facts21实跑。冻结gate新增R12–R14修订未复授；R9最终表/旧extend消费退休/正式诊断、原SOURCE32和frontend92继续OPEN，不按窄绿关闭原70。

> 2026-11-25最新[peer事实接续](cjmp-peer-facts-continuation-20261125.md)：Core不可变关系facts、默认/显式同prepare路径、source-aware normalizer与纯裁决已接，facts slice源码APPROVE。06f540f9实跑60/60（facts21、Index24、scope15），450af98d实跑92为91/1新fixture失败（SOURCE32全过/LL编译过）及全部原XML独立保存，修正后新21实际通过。完整invisible peer逐声明检查/非函数MemberMap/原92和70继续OPEN，不用局部绿关闭SOURCE。

> 2026-11-25最新[父图与消费接续](cjmp-source-supertype-continuation-20261125.md)：symbol完成产品、断环前真实Object、结构化缺core诊断、Source/explicit/CJO来源及退休phantom根已落盘。52fbb643测试编译失败保留；dcd7bae0实际111项108/3失败，resolve父图64与SOURCE32全通过，scope三失败已修并由0a7ab4b7独立15/15复验。extend scope新增冻结匹配/receiver/member/peer候选，编译与cold/hot/alias生命周期有证据，但完整门仍REJECT peer分类输入域与constraint-invisible表；下一共享facts/纯裁决及独立检查表继续。所有旧失败独立保存，原SOURCE/70/P0–P10保持OPEN。

> 2026-11-24最新[原symbol文件归属接续](cjmp-source-owner-continuation-20261124.md)：provider全集精确symbol查询和SOURCE真实文件调用已实施，限定源码PASS。正常SDK4f723f48真实169项159通过/原10失败，完整前端92为82通过/10失败，SOURCE32含原SOURCE14/StoredMember四项全部通过；15原XML、9991选定输入同SHA与SDK84观察证据保存。早期未配置SDK的57/57及140项95/45窗口保留，不混计或冒称正确SDK；未删继承guard。generateTree本轮32a34022 DONE0。下一完整父图产品/结构化缺Object诊断/退休phantom根及完整classifier batch继续，原70/SOURCE不关闭、不改原正确断言。

> 最新[nominal域与物理root接续](cjmp-nominal-root-domain-continuation-20261124.md)：原prepared group/snapshot已记录实际声明域，Source文件槽／CJO原header顶层位提供root证明，明确来源分流、真实seed/包/模块/kind/role/TP守卫已实施。f6102f0b真实71/71，10036输入/SDK84同SHA、十原XML独立保存；85a0c53e实际serialization370与raw2通过、完整前端92为82通过/10失败，模型编译失败窗口与输入漂移及614的一例装配失败全部保留。根记录不替代尚未接入的actualizer、完整type closure/cache或原SOURCE/70；未改定时任务，继续原授权工作。

> 最新[classifier／约束接续](cjmp-classifier-constraint-continuation-20261124.md)：公共childSubstitutor必填API与全部caller已迁移，原三个测试保留、新五回归实际执行；aefdd421为23/23，10034输入/SDK84前后同SHA、三原XML真实窗口，de9e91e0的checkers/LL生产编译成功。CJMP当前仍显式Empty，完整owner/classifier证明和原10项失败继续OPEN；旧完整92的82通过/10失败和SOURCE28不代替新API整套验收。未关闭70/SOURCE，未改定时任务。

> SOURCE补验 `7f9572d4` 已28/28：两parser SOURCE14、canonical8、未导出签名负例2、冻结类型4，八原XML、10034输入/SDK84前后同SHA独立保存。完整前端92仍以 `6a4e4995` 的82通过/10失败为准，不重复累计。后续公共泛型约束childSubstitutor必填API正在实施/编译，旧绿不能代替新代码验收；[前端接续](cjmp-frontend-integration-continuation-20261123.md)保留全部失败。

> 最新[两段式前端接续](cjmp-frontend-integration-continuation-20261123.md)：原annotation binary来源判断已修，定向9025d837为8/8；完整6a4e4995的前端92项82通过/10失败，联合canonical8与PSI SOURCE7共107项97通过/10失败。同10034输入/SDK84两端SHA、四原XML独立保存。两parser完整SOURCE矩阵另补验，不能把PSI7冒称全部；10项前端失败和统一classifier比较输入继续实施，完整CLI程序验收才后置。定时任务保持ACTIVE/10分钟，未修改。

> 2026-11-23范围纠正：`compiler/frontend` 的 `CjmpTwoPhaseCompilationTest` 是直接调用前端流水线的 common SOURCE→CJO→specific 集成门，92项全部继续当前验收；仅完整CLI驱动、命令行程序包装及发布验收后置。最新 `65a24d29` 完整实跑92项81通过/11失败，0错误/跳过；10034输入与SDK84前后同SHA，原XML及逐key账见 `build/cjmp-frontend-integration-20261123/65a24d29-{results,input-window,terminal}.json`。旧泛型key碰撞、HMPP身份、block local与header五项本轮通过，但扩展配对/private成员、初始化/const值、annotation、enum仍失败。完整SOURCE与原70项保持OPEN；定时任务cjmp只读核验ACTIVE/10分钟，没有修改。以下历史“CLI独立92项”称呼不作为后置这套前端门的依据。

> 2026-11-23最新Value/receiver已真实联合 `2936e09b` 449/449，随后端点有序投影新字节 `2b6b7a44` 定向86/86，0失败/错误/跳过；两窗口分别同10034输入/SDK84 SHA，55/11份原XML独立保存。第八receiver gate限定源码ACCEPT，不授完整初始化授权/Source生命周期或原整项关闭。新实现、所有失败/八轮拒绝、程序点修正和接下来的正式初始化发布/replay/CLI/LL/typed setter/pure canonical D余额见 [最新tuple/Value接续](cjmp-tuple-value-continuation-20261123.md)。目标active，定时任务未修改；下方旧字节按时点保留。

> 2026-11-23包装/调用调度补齐后最新178fcc37实跑49/49，9e05b53f SOURCE联合405/405；10030输入/SDK84同SHA，52原XML无损核验。Named/Inout透明边界、Optional保留真实事件、call-enter后deferred body及原7case真实差分/新增6覆盖见[最新生命周期接续](cjmp-dfa-lifecycle-continuation-20261123.md)。原43/399和全部旧失败独立保留；完整F3/value/payload/G3/原70仍OPEN，未改定时任务。

> 2026-11-23公共DFA生命周期基础已实跑43/43，SOURCE联合ca20506f真实399/399；10030输入/SDK84同SHA，52原XML无损逐SHA核验。Flow/Context所有权、default/finally/闭包/loop实际路径及semantic-ready产品门已应用；全部失败与下一完整tuple/value-generation/payload及正式消费余额见[最新接续](cjmp-dfa-lifecycle-continuation-20261123.md)。完整G3/R5/SOURCE/原70/P0–P10仍OPEN，未改定时任务。

> 2026-11-23最新G2/flow基础已实跑48/48；SOURCE联合ab20f5a2真实377/377（SOURCE18、canonical8、负例2、SDK5均实跑）。10028输入/SDK84同SHA、48原XML无损逐文件核验；全部旧失败、三轮primitive拒绝、限定ACCEPT及下一analyzer/正式消费边界见[新接续](cjmp-g3-flow-variable-continuation-20261123.md)。G3 NOT READY，完整G2/SOURCE/原70/P0–P10仍OPEN；定时任务仅只读确认ACTIVE，没有改动。

> 2026-11-22控制目标批次已实跑397/397（SOURCE18/SDK5含在内），解析边界另21/21；两窗口输入/SDK均同SHA，47+3原XML无损核验。b57fe0a1的退出1是旧resolve测试API编译失败，未执行用例不计；后续API迁移与真实lambda completion driver已复验。G1/G4与target可重绑仅有限ACCEPT，完整G2/G3/enum F/readiness和原70/P0–P10仍OPEN。[最新接续](cjmp-source-target-continuation-20261122.md)含全部失败和下一真实阶段合同；旧状态按时点保留。

> 2026-11-22统一控制目标已正式生成并应用；36a30a36的LL与测试源码编译通过，7e088ff6真实397项395通过/2失败，原SOURCE18与SDK5实跑通过。10022与SDK84前后同SHA，47原XML无损核验。G1/G4与target可重绑合同获限定ACCEPT，专用For节点/共享死边与原冻结target失败修订正在df7a45ad编译；完整G2/G3、enum F/readiness仍OPEN。[控制目标接续](cjmp-source-target-continuation-20261122.md)保存每次失败及完整余额。下方旧候选状态不覆盖新磁盘，完整SOURCE/70/P0–P10保持OPEN。

> 2026-11-22 最新实际252ed380为395/395、无失败/错误/跳过，原SOURCE18/ordinary12/SDK5均实跑；10033与SDK84前后同SHA，46原XML无损归档核验。[正文接续](cjmp-source-body-continuation-20261122.md)顶部保存完整结果、所有旧失败及新的正式body/initializer API。完整SOURCE/70/P0–P10仍OPEN：LL编译卡共同Pattern的decompiler消费；统一原Jump/target/for八路径候选尚未应用，raw/DFA/CFG/copy/CLI/LL/readiness和逐ID余额继续。

> 2026-11-22 当前共同 SOURCE33 ordinary reader 与 Pattern 合并源已应用；a5afc993 正式生成完成但 tree 编译失败，旧 383/11 保留为最后行为窗口。[正文接续](cjmp-source-body-continuation-20261122.md)列出真实终态、被撤掉的非原子发布、全部 P/raw/CFG 同步及新5项未验测试。完整 SOURCE/70/P0–P10 不按局部源码改动关闭。

> 2026-11-21最新06ac310a真实373/3失败，原SOURCE18/新consumer2/canonical8/snapshot40/copy21通过；9917同SHA、135924字节原XML同窗口。[省略签名接续](cjmp-source-signature-continuation-20261121.md)保留所有失败与限定ACCEPT。完整annotation及PRE组件/FINAL PSI输入仍有三真实失败，旧generated binding owner冲突未按本轮绿关闭；原70项/P0–P10和完整跨模式/LL/CLI验收仍OPEN。

> 最后真实窗口841b519b为361/8失败，SOURCE18/snapshot40/copy21通过；8e9ae11b编译通过、原XML与同SHA证据齐全。正式annotation完整表达式材料化、UNREACHABLE type0 header加载等仍OPEN，[最新接续与下一具体步骤](cjmp-source-unified-encoder-continuation-20261120.md)按此继续，不重新套用旧候选。目标active，未提交/归档/关闭原70或SOURCE整项。

> 最新361/8失败（6c0fcd16），SOURCE18/snapshot40/copy21通过，13新边界已执行；annotation/partial正式消费及新源例仍有实际缺口，第三源码gate仍REJECT。分支合流、分类只读和依赖重复vector新修订正在第七编译。[当前接续/原XML](cjmp-source-unified-encoder-continuation-20261120.md)为最新代码与完整余额入口；原SOURCE绿色和局部ACCEPT不关闭完整目标。

> 最新统一encoder联合9bcefba2实际348/3失败，原SOURCE18已18/18、snapshot40/copy21通过；第二gate仍REJECT Q1–Q3，新修订及13边界测试正在第五编译。[接续与完整原XML](cjmp-source-unified-encoder-continuation-20261120.md)保存每次失败与新源码门，旧266不替代本批；完整SOURCE/70/P0–P10保持OPEN。

> 当前统一 SOURCE/imported encoder 与保存计划已实施，但955eeb4a实际287项9失败、原SOURCE16/18；第一修复gate仍REJECT R3/R4/R6/R9。后续修订正在编译与独立复审，[最新接续与原XML](cjmp-source-unified-encoder-continuation-20261120.md)保留真实窗口及完整余额。下方SDK266/SOURCE18均是旧字节历史，不覆盖本批。原70项/P0–P10及目标未关闭。

> 最新统一值/精确nominal绑定已SDK联合266/266：VarInfo/Pattern/v池同context、binary16、对象共享/循环、字段实例化/入口兼容、CJO初建fixed symbol→SOURCE传播、wire kind及标准异常root全部实际验证；source18再次通过、配置SDK5无skip。9908输入/84SDK同SHA、34真窗口XML104652字节。[新接续](cjmp-source-values-continuation-20261120.md)保留全部失败/gate及下一统一encoder/正式跨模式/LL余额；原70项/P0–P10未关闭。

> 2026-11-20拥有闭包/F首次绑定批次已联合99/99：取消all-table函数创建、补global/member initializer、header-first local/TP/helper/匿名参数、真实body0 F、Return独立F域及SOURCE类型owning屏障；第三gate有限ACCEPT。9907同SHA、10原XML22115字节，全部失败/审查与下一统一值产品见[新接续](cjmp-source-owned-closure-continuation-20261120.md)。完整S5/S6及原70项/P0–P10不按此切片关闭。

> 2026-11-20公开canonical载荷批次已联合47/47：named/body0 header、header-only坏body边界、四字符串形态的native错误恢复、selector非拥有回边及原SOURCE18均通过；9905同SHA、七真窗口原XML。[新接续](cjmp-source-public-payload-continuation-20261120.md)保留前轮失败、固定JString/cjc证据和未覆盖parent/发布/完整S5/S6门。原70项不增整项分数。

> 最新SOURCE18正式consumer已18/18，含原14+4ref；G3生产standalone文件owner子要求7/7并保存同代原XML证据。当前实际生产、所有失败、完整S5/G3余额及按原ID集中交付入口见[decoder接续](cjmp-source-decoder-continuation-20261119.md)、[G3接续](cjmp-g3-ownership-continuation-20261119.md)、[交付板](cjmp-source-delivery-board-20261119.md)。SOURCE18/G3子要求不替代完整H1/H9/S5/S6/G3或原70项，目标保持active。

> 11月19日S5类型loader已实施Type(28)、真实TP reserve→bounds、原constraints独立产品、CURRENT/imported canonical与依赖选择、完成缓存原子发布。a26341c6真实73项70通过/3原有跳过，原SOURCE14/14、新类型图13/13，9894输入同SHA，十一原XML真实窗口；[最新decoder接续](cjmp-source-decoder-continuation-20261119.md)保存失败、有限gate与完整S5/S6余额。未关闭原70项/P0–P10或完整SOURCE链。

> 11月19日当前410451a5真实48/48，原SOURCE14及扩展18再次通过；Alias/ABI、TypeUse与C extras含deferredUnit已通过，catch真实TypeRef尾槽已接入。完整证据、编译终态、Inherited H1–H3与D1/D3/K01/Generic/strictCapture/S5/S6未闭项见[最新类型使用图接续](cjmp-source-type-use-continuation-20261119.md)。旧“首轮验证未终态/2SOURCE通过”均为历史，不作为新字节证据；原70前端/P0–P10未关闭。

> 11月19日最新源码与恢复的a39e14e2真实Alias两失败见[类型使用图接续](cjmp-source-type-use-continuation-20261119.md)。TypeRef来源/Paren/公共原型和类型使用图已推进；当前来源G2/G7仍REJECT、TypeUse首轮验证尚未终态。历史184/29不证明新字节；完整S4/S5/S6与原前端/P0–P10继续OPEN。

> 冻结类型encoder新实施及fc048caa真实184/184、后续d4288e8a真实29/29见[新接续](cjmp-source-frozen-encoder-continuation-20261118.md)。两轮均6849同SHA、原XML真实窗口，原SOURCE14再次通过；F4重复TP语义等值获得有限ACCEPT。旧104内存失败/漂移保留。完整Generic/Alias使用槽/严格capture、S5/S6及原前端余额仍OPEN，不按局部通过关闭S4或目标。

> 更新重启后的最后磁盘验收为5167314c真实176/176，原SOURCE14仍14/14，6845同SHA、17原XML37302字节真实窗口。[ordered type接续](cjmp-source-ordered-types-continuation-20261118.md)说明完整有序输入/身份验证/self type编码和仍REJECT的统一类型encoder、Generic/Alias/RawArray语义产品。下一继续S4 payload消费与固定身份投影，再S5/S6正式内容消费，原70前端/P0–P10不缩减。

> 最新loaded-snapshot身份实施见[接续](cjmp-source-snapshot-continuation-20261118.md)：5a657814实际173/173，原SOURCE14仍14/14；6843同SHA、17原XML36710字节在真实窗口，身份域获得限定源码ACCEPT。[完整证据](cjmp-source-helper-20261117/source-snapshot-joint-173-results.json)。冻结类型/统一encoder退休legacy、decoder/正式正文消费及原前端/P0–P10仍OPEN，不把本轮通过计作整个P8完成。

> 2ab24d31进一步修复native参数/Generic上界的present-empty格式，实际43/43（writer25、SOURCE18）通过，原SOURCE14在新字节下再次14/14；[真实终态](cjmp-source-helper-20261117/source-type-empty-vectors-results.json)。这是f90e054a联合101之后的新修订/新窗口，未把两轮拼成一次。完整S4/S5/S6与原前端/P0–P10仍OPEN。

> 2026-11-18最新完整窗口f90e054a联合101/101，原SOURCE14实际14/14、新四官方引用4/4，6841同SHA、完整原XML全部在窗口；[最终证据](cjmp-source-helper-20261117/source-pattern-reference-joint-101-final-results.json)。[codec接续](cjmp-source-codec-continuation-20261118.md)保留真实实现与仍开放的S3/S4、统一decoder/正式消费、原前端余额。14/14只是必要条件，不据此关闭P8/E6/G1或目标。

> 最新生产/证据见[SOURCE codec接续](cjmp-source-codec-continuation-20261118.md)：正式writer与三池已接入，原SOURCE14已12/14，新111条官方引用四例4/4，联合101实际99/101。局部VarWithPattern声明产品已接入，最后空identifier规则刚修，正在两原例复验。S3/S4完整、decoder/正式消费及原前端余额仍OPEN；以下旧未接入和4/14描述是历史。

> 新canonical SOURCE候选与完整真实终态见[canonical接续](cjmp-source-canonical-continuation-20261117.md)：最新联合97为87通过/10原SOURCE失败，canonical8/8、copy19/19、metadata36/36、helper18/18、原负例2/2及SOURCE4/14，6834同SHA、12真窗口XML。随后SOURCE生成origin修订已通过tree/providers/resolve编译，尚未接入S0/encoder；S3完整、统一codec和正式消费仍OPEN，未以局部检查关闭原项。

> 最新默认参数/lambda定向终态已从磁盘核验：queue `d6c71a37` 在2026-11-17T10:36:07.347Z至10:38:50.700Z为DONE/退出0，复制19、metadata36、生命周期18、原负例2共75/75，无失败/错误/跳过。六原XML共15,334字节全部在真实窗口，6826输入SHA `BE1412F21B6D7368542D2A6DF00587E82F5A7F5B44DB0F33D8E376641B84CB81` 与事前冻结一致；[完整逐case/压缩原XML证据](cjmp-source-helper-20261117/front-default-final-75-results.json)。旧进程54359已不可读取，未伪造其终端。该轮不包含SOURCE14，不关闭原条目的完整验收；以下新四例仍失败描述是旧时点。

> 最新用户目标限定为原CJMP70项/P0–P10方案的前端部分，排除全部后端，不泛化全编译器前端。先读 [前端子集账](cjmp-frontend-subset-audit-20261117.md)、原计划和滚动审计。原前端CJO/SOURCE要求逐项保留；R5按实际consumer/必要依赖，不自动扩全框架。下方旧SOURCE优先与91.22保留历史，不能作为最新任务排序/完成度。当前default/lambda修复继续，14生命周期旧通过、新4late/OBL/PCLA仍失败。

> 最新 S1/helper生命周期与 R5实际候选见 [生命周期接续](cjmp-source-helper-lifecycle-20261117.md)：原SOURCE已4/14；三根因修复后两parser十四生命周期14/14，原负例2/2，原十个完整正文失败保留。R5完整审查仍REJECT，canonical/codec/正式消费继续OPEN。以下旧时点记录不覆盖新证据，原70项/P0–P10不减范围、不增整项分值。

> 后续实施已修复生成器并通过R1–R4限定源码门；最终联合69项为复制19/19、S0 36/36、原SOURCE2/14，57通过/12失败。6815输入前后SHA一致，全部七XML属真实窗口。最新实际修改、证据、边界和生产接续入口见 [复制层接续](cjmp-source-copy-repair-20261117.md)。以下内容保留为原交接时点证据，不用其“生成器仍失败”描述覆盖后续成功；完整S2、R5/R6、S1–S7仍未关闭。

用户要求先记录当前工作，再在新会话继续实施。本记录依据当前工作树、已有验证终态、独立审查和实际生成器输出写入；本轮没有修复生产代码或重跑行为测试。新会话应核对磁盘与固定版本源码，记录中的历史通过不能代替最新候选的验收。

## 工作树与用户约束

- 唯一实现树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
- 分支：`codex/cjmp-framework-implementation`；本次读取 HEAD：`b4dd742d589e4b5ee6592d454634c44f634df08a`。
- `D:/code/intellij/cangjie` 主仓库和其中 `external/` 参考树只读。复用指定工作树，不另建空工作树，不把默认 cwd 当实现树。
- 01:50:10（Asia/Shanghai）的 `git status --porcelain=v1 --untracked-files=all` 为 3,734 个路径：` M` 1,462、` D` 37、`??` 2,231、`MM` 4。包括历史与其它已有工作，不代表全部是本轮修改；有已暂存和未暂存并存的路径。不要 reset、clean、stash、回退、批量覆盖或擅自提交。
- 用户已经停止其它会话。不要再给“继续 cjmp 实现”或“总结 CJMP 实现进度”等会话发送消息，不等待它们释放窗口或批准；只专注当前工作。保留其已落盘修改。
- PowerShell 使用 `pwsh`（PowerShell 7），`Get-Content` 明确 `-Encoding UTF8`；文件只通过 `apply_patch` 编写。
- 以整个项目和完整生命周期实现；禁止最小实现、简化、兜底、退化、fixture 补丁、删除正确正文、伪造 `body=0`、跳过正确失败或用空 CFG 冒充分析产品。接口和框架关键代码保留中文注释。
- 原 70 项、P0–P10 全部保留。当前优先完成完整 SOURCE→CJO→正式消费端（P8/S1–S7，关联 E6/G1），不能持续补基础合同而不交付生产链路。
- H6 用户排除的后端余额保留分母、不实施、不计完成；v10 逐 key 最终验收继续待办，暂缺不阻断其它授权实现。

## 先读取的方案与证据

下列路径相对于唯一实现树；无需为了接续重新阅读旧会话。

1. [原70项计划](cjmp-framework-review-and-fix-plan-20260928.md)。
2. [滚动实现审计](cjmp-progress-audit-20261008.md)，顶部最新记录优先于下面历史状态。
3. [完整SOURCE交付与关闭清单](cjmp-source-chain-delivery-20261116.md)：S0–S7 各层输入、输出、生产接入及完整关闭条件。
4. `build/source-expression-codec-audit-v113/source-copy-production-gate-20261117.md`：最新 S2 独立审查，结论 **REJECT 完整 S2**。
5. `build/source-expression-codec-audit-v113/source-copy-analysis-limit-20261117.md`：复制完成与分析就绪、CFG/phase 边界。
6. `build/source-expression-codec-audit-v113/source-graph-native-contract-20261116.md`、`source-graph-kotlin-map-20261116.md`、`source-canonical-semantic-flow-20261116.md`、`source-codec-graph-context-map-20261116.md`。
7. 同目录的 `default-helper-chain-v113.md`、`default-helper-preparation-authority-20261115.md`、`audit.md`，以及原 SOURCE fixture 的 `FIXTURE.md` 和官方产物。

前端实施继续遵循 `kotlin-cangjie-frontend-parity` 技能：Kotlin 的框架职责和仓颉的真实语义分别考证。技能文件为 `D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md`。固定 Kotlin 为 `3c7a5041f238102285764d370665f56f587f9dc4`，参考树 `D:/code/intellij/cangjie/external/kotlin`；固定官方仓颉 v113 为 `14276740ffec1262b81b6b5236b1d840af270b37`，参考树 `D:/code/intellij/cangjie/external/cangjie_compiler`。官方 checkout 不等于固定对象，读取固定提交，不能冒称当前 checkout 就是 v113。

本轮路径、长度、SHA-256、状态统计、生成器队列终态及历史结果摘要保存在 [状态快照](cjmp-handoff-20261117/state.json)。文件采样只是列出的 13 文件在对应时间的观察，不是全工作树前后冻结或无漂移证明。

## 已交付并实际验证的产品

| 产品 | 已核验范围 | 实际验证及证据 |
| --- | --- | --- |
| 共享 scope/storage 修订 | raw/declared/substitution/static/upper-bound/alias 产品进入 owned storage；精确退役、Epoch 集合、真实来源和 canonical subtype 依赖；alias metadata、extend 来源/peer 与提前准备不适用 owner 修订 | 91906 / queue `6a374215`：54/54，tree9/providers18/resolve22/LL5；4,104选定输入无漂移，7原XML12,895字节。`psi/build/cjmp-verification/20261116-cjmp-heavy-scope-cache/resume-scope-selection-{inputs,terminal,verification}.json` |
| S0 SOURCE 同代冻结协议 | 当前 CFIR typed payload、Block d/e 槽、有序 resolved binding、applied function-value 角色、span/literal、控制目标、patterns/annotation/receivers；opaque identity memo+BFS 常量 records 保留共享/cycle，防御复制嵌套 annotation entries 与 match missing texts；Library 未绑定状态与 SOURCE 严格准入分别表达 | 32522 / queue `4c1ab0bd`：providers36/36（snapshot30+payload6），原SOURCE2/14；联合50项38通过/12失败，0 errors/skipped；4,107输入无漂移，6原XML59,812字节。`psi/build/cjmp-verification/20261116-cjmp-source-graph-codec/binding-closure-{inputs,terminal}.json` |

S0 核心文件位于 providers 的 `resolve/providers/`：`CfirExportSourcePayload.kt`、`CfirExportSourceElement.kt`、`CfirExportConstantGraph.kt`、`CfirExportAnalysisInputSnapshot.kt`，对应 `CfirExportSourcePayloadTest.kt`。共享 tree 的 ThisReceiver/SmartCast 已补来源拥有边。S0 是已有限定验收的输入产品，完整 native canonical 图、helper、codec 和正式消费没有因此完成。

原 SOURCE 14 项仍保持正确源码、SHA、表数量与严格结构期望。两 parser 各四个 SourceExpression 正文及一个 default facts，共10项；两 parser 各两个 StoredMember 方法，共4项。当前两 facts 通过；八正文失败于 walk/calculate/evaluate/recover 的 Block 不可表示，两个 DEFAULT value 正文同类失败，两个 helper 独立声明期望5/实际4。十二失败没有消除。

原70项仍为历史57整项、12部分、C3零，`447/7÷70＝91.22%` 是实现估算，全面验收未完成；本轮交接不增加整项分数。其它原余额仍保留，不能把 SOURCE14 绿或复制器单测绿等同 P8、E6/G1 或总目标关闭。

## S2 当前候选已应用，尚未通过

候选来源：`build/source-expression-codec-audit-v113/source-copy-implementation.patch.txt`，172,072字节。已核对旧文件内容及九路径，仅在 tree 范围应用。不要重新盲目应用整份候选；当前工作区就是继续修订的起点。

九个实际修改/新增文件：

- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopy.kt`
- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirDeepCopySymbolRemapper.kt`
- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyVisitor.kt`
- `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/copy/CfirDeepCopyOwnedGraphVisitor.kt`
- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationAttributes.kt`
- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirAbstractArgumentList.kt`
- `cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/Main.kt`
- `cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/printer/OwnedGraphCopyPrinter.kt`
- `cfir/cfir-tree/test/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyTest.kt`

现有 API 是 `deepCopyOwnedGraphs(roots, typeRemapper, attributeRemapper, symbolRebindings, targetRebindings)`，以及单 root 入口、`CfirOwnedGraphCopyResult.getCopied/getCopiedSymbol`。类型 remapper 和属性 remapper 是显式必填协议。候选先收齐 owned 域/fresh symbols，再分配壳、填字段、绑定 targets/metadata；helper metadata 是拥有边，域内借用引用重定位，域外身份保留，CFG 不共享。

独立审查限定认可 generated concrete 覆盖：112个生成 concrete 中出现109个，108个 override；两个 lazy 实现明确拒绝，ArgumentList 手写。这不证明手写 concrete、全部字段、分析就绪或生产接线完整。六个新增测试是手工造树测试，尚未执行成功，不能记作通过。

### 六项已考证拒绝与修复合同

| 项 | 当前缺口 | 必须实现的合同 |
| --- | --- | --- |
| R1 | RAW ThisReceiver/InaccessibleReceiver、SmartCast、ConstantValueExpression 没有实际复制分支；collector 收到后 allocate 失败 | 每个现有手写 concrete 分配和回填完整字段；常量值图保留共享/cycle并显式重映射类型、字段/constructor符号；不删this、剥smartcast或降stub |
| R2 | 属性按运行时 CfirElement 一律吞入 owned；correspondingField/ValueParameter 双向关联会意外扩大复制域，调用者无法纠正 | 按注册 key/owner 声明所有权。helper/Evaluated.result 明确 owned，参数/字段关联只是借用；域内重绑、域外保留；未知 key 由显式协议分类 |
| R3 | Candidate 派生引用被复制成 generated base，丢 Candidate、诊断和真实回边，改变 concrete kind | 明确 RAW/完整解析与中间候选态边界。完成入口证明候选已正规完成，或收集时拒绝不支持态；若支持则 resolve owner 提供完整产品协议，不能降基类 |
| R4 | 任意 symbol/target map 加 unchecked generic cast 没有槽位种类/绑定不变量；nullable this boundSymbol 生成调用不合法；empty singleton 被规范化但承诺返回窄 concrete | typed symbol种类/fresh/unbound/唯一owner校验；target种类、目的owner、deferred bind及交付前内部回边校验；nullable槽正确生成；singleton或typed查询的身份/kind承诺明确，Simple Cone subtype约束明确 |
| R5 | 保留 BODY phase 与正文类型、清空 CFG；正式 BODY 路径按 coneType 判已解析并跳过，不会重建图 | 结果携带真实分析就绪/待重建owners，正式入口检查；RAW helper 正常 enter/visit/exit 构建CFG；已解析copy由resolve层正式完整重建控制流/嵌套函数/loop/capture/dataflow产品后发布。不可回退phase、删类型、空图或只再请求BODY |
| R6 | 没有生产消费调用复制 API；目前只有六个手工 tree 用例 | S1 绑定前准备owner把 assignment、函数自有泛型/bounds、参数typeRefs和前序参数重绑放进一次 grouped域，helper走正常provider/TYPES/STATUS/BODY与正式SOURCE入口 |

完整具体坐标和修复后的不可缩减验收门见独立审查，审查只读生产并没有跑 Gradle/cjc。copy worker 为 `01a1e54b-1e63-7123-a1c7-ab4e91435012`；reviewer `01a1e56f-f0a7-76b2-a8db-7787235bd4a2` 已完成 REJECT 报告，只新增该报告。新会话以磁盘产物接续，不依赖旧子代理句柄仍可用，也不必给已停止会话发消息。

## 最后一次生成器的真实失败

- queue：`0de51aab`，2026-11-17 01:42:42.742 至 01:49:58.714（Asia/Shanghai），已 `DONE`，退出码1。
- 01:53:13再次读取终态，queue PID 22828 已不存在；不能再按摘要中的“状态未知/正在运行”处理。本结论只针对这次队列任务，不表示机器上无其它 Gradle。
- 实际失败任务：`:cfir:cfir-tree:tree-generator:compileKotlin`；`generateTree` 没有完成。
- `OwnedGraphCopyPrinter.kt` 的 71、72、84、87、88、98、99、121、124 行报告 `Unresolved reference 'typeName'`，110行报告 `typeRef` 是 open/custom getter，不能 smart cast 为 `Importable`。这是实际编译日志，不是猜测。
- 完整终端文本已归档为 [generator-terminal.txt](cjmp-handoff-20261117/generator-terminal.txt)；命令执行标识 `exec-ba5d7422-8fd2-4f33-826f-9734a3964c24`。状态快照包含全部编译错误。
- 实际 shell 命令前缀包含 `undefined&`，产生了 BackgroundJob；queue wrapper 实际使用 Corretto21.0.9，日志也报告 GRADLE_RO_DEP_CACHE 环境变化。这轮不能冒称使用了历史正确环境。新会话显式设置环境，不复用缺失的会话变量或缓存命令前缀。
- 生成器失败后九文件的采样 SHA 与当前候选一致；不能把预先落盘的 generated visitor 当作正式生成器已成功产物。先修 printer 的真实生成 API/类型，再正式生成、编译和验收；不要只手改 generated 文件隐藏问题。

## 下一实施顺序与真实接入

依赖链：S2完整owned-copy → S1绑定前helper → 正常TYPES/STATUS/BODY → S3 native canonical语义图 → 复用S0冻结 → S4统一encoder → S5 decoder/materialization → S6正式生产消费 → S7原阶段/条目/全量验收。S1–S7均 OPEN。

1. 先修生成器编译和 R1–R6 实际缺口，按完整生成树/字段所有权证明复制域、typed重绑、控制目标和分析就绪；用真实原SOURCE/default/helper生产入口验证，不能以六个手工测试替代整层交付。
2. S1 helper 在 binding/provider 注册前生产。创建 fresh helper、全部前序参数、helper自有泛型、独立assignment和显式Return；grouped roots覆盖 assignment、自有typeParameters/bounds、前序参数typeRefs和当前参数typeRef。外部映射原owner→helper、原前序参数→fresh helper params；helper参数没有default、非named/member，不能递归带入以前helper。
3. `defaultValueMetadata` 的 hasInitial/function 与 `cjmpDefaultParameterState` 发布最终关系；provider 当前显式 `recordDeclaration`，不是深visitor，必须真实登记helper文件/outer，不能只加metadata边假装完成生命周期。TYPES header只处理参数/返回/注解；helper正文需正常独立词法域，不能借原函数作用域放过future/self非法引用。
4. S3按固定native顺序生产完整canonical图。SOURCE30另有FuncArg、MatchCase、MatchCaseOther；FuncArg是InvalidExpr+FuncArgInfo。Block mixed d/e，isExpr长度为operands+1，末位unsafe。loop先reserve，Jump.mapExpr仅回指已有loop；Return.ty是Nothing、result独立type，tail包装和追加Unit有精确顺序。++/compound保留原生AssignInfo(AddAssign,true)。range forin有真实iter/stop声明与更新/控制图；is/as有真实typed Match/pattern/Some/None及runtime flags，不能只猜Bool/Option类型。
5. S4编码完成产品，同一上下文独立d/e/t池、身份memo、reserve→递归→fill、明确完成状态；source/imported snapshot同一产品域，不能从InvalidExpr推断完成，不读live/重新解析/造helper。path-only loaded snapshot memo和冲突去重仍是缺口，不得混代。
6. S5先symbol/node壳，再填owner/body/type和非拥有回边；S6走真实两parser、写新CJO、ordinary/common/depends-on加载和正式frontend/CLI/LL消费。writer当前 `expressionOrNull` 仍仅有限四类，Block正文仍拒绝，尚未改成完整codec。
7. 每个实质产品或生产接入完成后，运行对应模块回归和原SOURCE14，核验正文非零、真实decl closure、table/edge/FullId/Position/shared identity；以 classname+testcase-key 记录结果和失败原因。原14通过只满足必要条件，继续按P8/E6/G1及原70项完整验收。

## 已停止会话 E9 的遗留边界

保留已落盘的 `CfirSupertypesResolution.kt` extend collector 使用同一 computation supplier 的修改，不回退。E9原基线41368 / queue8bc615ee：34项30通过/4失败，4,107输入无漂移；证据为 `build/cjmp-e9-20261116/baseline-{inputs,terminal}.json`。四失败是两parser的PrivateTypeSyntax/NonInterfaceAlias在SUPER_TYPES重入合同失败；这不是修改后复验通过。

本次已搜索tree/raw/resolve/serialization源码：未找到 `rawCallableSignature` 或 `rawExtendSignature` 的生产实现，`mangledBeforeSema` 命中已有CJO字段/reader-writer路径；不能把曾讨论的 declaration-facts→raw→serializer/deserializer 全链当作已经应用。E9原始签名余额继续 OPEN，当前先交付P8 SOURCE链。S2普通复制应保留实际既有原始事实；S1新helper必须有独立raw身份，不能沿用原函数mangling。不要再与停止的会话协商。

## 构建环境与验收操作

所有 Gradle 从指定工作树根通过 `gradlew-queue.bat` 调用。已实际验证的历史环境为：

```powershell
$env:JAVA_HOME = 'C:/Users/lin17/.jdks/corretto-21.0.10'
$env:GRADLE_RO_DEP_CACHE = 'C:/Users/lin17/.gradle/caches'
$env:GRADLE_OPTS = '-Dorg.gradle.jvmargs="-Xmx1536m -Xms64m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=192m -XX:ActiveProcessorCount=2 -XX:CICompilerCount=2 -Dfile.encoding=UTF-8"'
$env:JAVA_TOOL_OPTIONS = '-Xms64m -XX:ActiveProcessorCount=2 -XX:CICompilerCount=2 -XX:ReservedCodeCacheSize=192m'
& .\gradlew-queue.bat --gradle-user-home .gradle/cjmp-export-home :cfir:cfir-tree:generateTree --no-build-cache --console=plain --max-workers=1
```

这是历史环境和下一次调用参考，不表示本轮重新运行成功。Gradle launcher用JDK21，项目toolchain仍17。对所选task使用独立 `--rerun`，不要全仓 `--rerun-tasks`。先核对当前队列，不杀其它任务。

原14测试过滤器（`:cfir:analysis-tests:test`）：

```text
org.cangnova.cangjie.cfir.analysis.tests.CfirSourceExpressionExport*Test
org.cangnova.cangjie.cfir.analysis.tests.CfirCjmpStoredMemberInstantiationCjo*Test.publicInterfaceDefaultBodyPromotesReferencedPrivateFunction
org.cangnova.cangjie.cfir.analysis.tests.CfirCjmpStoredMemberInstantiationCjo*Test.referencedPrivateDefaultHelperIsCollectedOutsideClassBody
```

冻结输入要在生成器成功和实际代码修订完成后取样，再执行行为测试并做终态核验。保存真实窗口、原XML与逐case ledger；不能用旧XML、UP-TO-DATE报告或不同轮次重复计数冒称本轮验收。交接中的54/54、36/36、2/14和30/34都是各自历史运行，最新S2尚无行为通过证据。

## 本次交接完成状态

本次新增交接正文、机器状态快照和完整生成器终端，给原计划与滚动审计补最新入口。实现候选与所有历史修改保留；未提交、未归档方案、未声明goal完成或原条目关闭。这里只执行文档/状态核对，不为新S2补记未执行的测试。后续从上述真实失败与交付链继续实施。

