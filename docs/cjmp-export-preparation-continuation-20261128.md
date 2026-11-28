# CJMP 导出输入准备接续（2026-11-28）

> 最新g/6534c67a真实完整514/479/35，逐Test task --rerun、Source18/provider46/FullId1完整报告，无worker异常、input/SDK同SHA；原XML/流全部核验。f为496/451/45且Source18陈旧未报，不能指完整矩阵。Complete已完成产品复用、canonical owner/file共用、Lazy前置守卫/正规库provider已修并获限定静态接受，g仍33未选header需其实际context完成（session-global provider缺）、1新增alias请求次数错误断言、原Core定义1。下一R40落实原Published content owner显式header能力，getter保持passive，继续全scope。原70/P0–P10保持OPEN。

> e已终态124/124、native3eebd818 exit0：原SOURCE18、FullId1、providers45、serialization header60均通过，input/SDK同SHA、十一份原XML/流已核验。旧c/d/e的新增serialization选择器实际绑定到了providers任务，7项新增serialization未被选择；不能称全131已验。验证脚本已按任务归属修正，新增shared constant双session回归已apply_patch接入，f改为完整serialization+SOURCE+providers+FullId的新冻结，真实终态待核；原70/P0–P10和Core定义余额保持OPEN。

> 最新79781ecf生产/serialization测试源码编译exit0；context增量静态gate限定ACCEPT。d已报告64/62/2且缺serialization（新测试缺import）；Source错误现明确为Result尚未prepared的APILevel类型。主线程已按每个真实element/declaration context收集类型域、限定Source owner分类并补import，e完整原SOURCE/FullId/11回归新冻结已启动，终态待核；新共享语境候选仅在build准备。原70/P0–P10及Core等余额保持OPEN。

> v3独立静态gate已ACCEPT共享全decl/type inventory和公开API迁移，解除R1/V2-R2；不代表新字节行为通过。f1e264d7仍编译中，d新冻结窗口已排队；原SOURCE两个APILevel失败和所有旧窗口保留。后续核真实编译/d全矩阵及no-drift，再继续Core定义产品，不关闭原70/P0–P10。

> c真实终态为已报告64/62/2，native988b3b9a DONE/exit1；serialization因新增测试跨模块访问internal Snapshot编译失败，全部serialization选中suite缺失/陈旧，不能称完整矩阵。原FullId和providers45通过、SOURCE16/18仍缺APILevel捕获类型键，输入/SDK同SHA。v2已解除R1但拒绝共用capture漏中间inventory；主线程已补齐decl/type完整inventory并将新增serialization回归迁到公开Analysis API，Snapshot保持internal，最新f1e264d7编译/v3复审待核。原70/P0–P10保持OPEN。

> 最新实际终态：b/50d33f9d为120/118/2，原FullId已通过，Source两个operatorsConversions仍失败于未准备的ohos/labels/APILevel类型键；9851输入和SDK84前后同SHA，全部选中suite报告、无worker异常。R1同代签名/消费域及最终边重检已继续实施，11项回归已应用；c新冻结窗口正在验证，新字节和复审尚未终态。下方b“尚待终态”保留为旧时点。

唯一实现树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10保持完整范围；H6排除余额保留分母，v10待办不阻断其它授权工作。本文记录实际代码和运行证据，不将局部通过计为S1–S7/P8/E6/G1或整个目标完成。

## 原 FullId 失败取证

原 `CjmpTwoPhaseCompilationTest.source CJO preserves imported class declaration FullId` 的源程序、依赖装配和全部FullId断言未修改。

- `59b47395`：frontend测试源码编译，DONE/exit0。
- `65f65078`：`r38-imported-capture-20261128-a`，真实61项、60通过、1失败、无error/skipped；serialization header/content request 60项通过，原frontend用例失败。
- native终态、完整选中XML均存在；worker无异常。源码与SDK前后manifest相同。
- 失败为 `CfirInterfaceSymbol(Any), field=annotations`。该版标签同时比较 `annotations` 与 `hasGenericNode`，只能证明该组合比较失败，不能据此声称唯一变化字段或唯一写入者已经实证。随后主线程已拆分全部组合标签，保留每一个原条件和短路顺序。
- 三原XML和两原终端流共五个gzip归档均已解压，核验长度与SHA256。

原始证据位于 `build/cjmp-invisible-peer-20261125/r38-imported-capture-20261128-a-results.json` 及同名前后输入/SDK清单、terminal/stderr日志；不覆盖旧r33–r37窗口。

## 固定源码与实际生产修改

Kotlin固定对象：`3c7a5041f238102285764d370665f56f587f9dc4`；官方仓颉固定对象：`14276740ffec1262b81b6b5236b1d840af270b37`。外部源码只从Git对象读取。准备权限和遗漏边依据：

- `build/r38-imported-capture-readiness-mapper.md`。
- `build/r38-cjo-save-imported-input-authority.md`。
- `build/r38-export-preparation-closure-authority.md`。

新增 `CfirExportAnalysisInputPreparation`：沿真实声明、类型、sourceChildren和canonical来源边排空准备队列；Published声明显式调用原provider完成normal annotation/default header，并核对真正的Complete注解产品；Owned已材料化节点使用自己的实际字段，不按保留的CJO provenance向旧provider认领内容。

type lookup、abbreviated/expanded alias、真正nominal target和父/参数/原generic身份在准备端确定。Result保留类型依赖及声明类型；捕获端不再调用provider lookup或fullyExpandedType。源接口默认实现、class override、extend requirement与property accessor分类前移至原scope/extend服务；捕获端的末尾scope构建已删除，消费准备产品。

新增 `CfirExportSourceDependencies`，准备和捕获共同遍历canonical图的context/owner、header参数与desugared参数、return/lambda/array/spawn/resource target、pattern、annotation target、实例化类型、matched parent、常量与保存typeRef。常量字段storage类型及真实field/enum symbol保留原域。源码实例字段初始化依赖的生产移入准备端，`CjoInitializationDependencyInputs.capture`只冻结完成状态，不再启动收集。

`CfirCjoPackageMetadataProducer.Preparation.prepare`先完成定义自身依赖，再获取定义的正规SOURCE保存产品；补入其图/保存类型/初始化inventory后完成最终准备，随后冻结并交既有analysis/plan/encoder。borrowed capture与wire输出计划仍分开，IMPORTED、CJO来源、Any身份和全部no-drift条件保留；准备没有调用imported body/initializer能力。

当前实改文件：

- `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputPreparation.kt`。
- `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourceDependencies.kt`。
- 同目录 `CfirExportAnalysisInputSnapshot.kt`、`CfirExportAnalysis.kt`。
- `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt`、`CjoInitializationDependencyInputs.kt`。

## 当前验证和剩余工作

### c 不完整行为窗口与实际接续

`r38-prepared-input-20261128-c` 的Source18为16/18、providers45与原FullId1/1通过，合计已报告64/62/2；serialization67项没有执行，原XML缺失/陈旧已在results中单列。native终态不代表完整选择矩阵。原错误是新增测试引用providers内部Snapshot，并非生产编译或worker堆失败；没有将这些未执行测试计作通过。

九份原XML/终端流gzip均已解压核验长度/SHA，源码与SDK前后manifest一致。两Source转换例仍在SAVE_CJO报APILevel缺键，尚未将其猜测为唯一根因。

`build/r38-export-preparation-v2-gate.md` 接受最终签名、消费域、owner/file和alias输入复用；V2-R2仍拒绝普通capture只收调用者额外声明，遗漏已准备的初始化中间target。主线程已在共享capture入口加入全部prepared declaration和type inventory，维持输出plan与borrowed捕获分域；没有将缺项查询转发给准备产品兜底。未知捕获类型增加`prepared`状态诊断，仍严格拒绝。

新增serialization回归已迁到现有公开 `CfirExportAnalysis.analyze` 与只读接口，保持Snapshot internal，不添加friendPaths或反射。CJO原provider/null→materialized身份及旧输入失效校验保留；borrowed内容访问继续受SOURCE拥有域限制。四个providers负回归保持内部Snapshot直接验收。

最新生产/回归编译为queue `f1e264d7`、exec `54145`；v3复审和下一新冻结行为均待真实结果。下一步先核编译，再完整复验11新增/原SOURCE/FullId，继续APILevel和Core真实定义产品余额。`build/r39-core-definition-product-mapping.md` 已完成下一个Core产品的具体owner/API/完整迁移映射，未作为已实施或已验收产物。

### b 完整终态与新修订

`r38-prepared-input-20261128-b` 的原FullId 1/1、providers输入/载荷41/41、serialization header/content request60/60通过，SOURCE18为16/18。两parser operatorsConversions在正式SAVE_CJO报 `Key ohos/labels/APILevel is missing in the map`；原消息/数量断言未改。native DONE/exit1、120/118/2、无error/skipped/worker异常，完整选中suite报告。

9851输入前后SHA `3f104142908c8e37f342dfa550911203f91488b0b21aeb2491dd0e8330a3c4fc`；SDK84前后SHA `7bbbba79757f4588ebe280b79b57c86d50740264aa090de853311dd0b07c2f6b`。原XML和两终端流的gzip、长度、SHA均已逐份核验。

第一独立gate `build/r38-export-preparation-gate.md` 为REJECT：先前Result保存的声明类型与捕获时当前签名没有同代关联。该拒绝与b的APILevel错误分别保留，不将窄FullId通过当成gate获批。

主线程已新增 `CfirExportDeclarationTypeInput`；完整header/type边完成、接口分类和新父header闭包结束后才构造声明类型，冻结真实有序参数/ref/Cone/ABI/TP/alias/self输入。Result消费入口核对files/roots/member/canonical身份域及额外输入资格；首次capture和后续validateUnchanged均校验签名，未知类型继续严格拒绝。owner/file查询也前移到准备产品。

准备阶段增加完成后header/element/constant/type的完整边重检。无缓存alias展开记录实际读取的每个alias输入，仅在真实输入改变时重建尚未发布的准备产品，避免反复分配等价expanded Cone导致不收敛；没有次数上限、body补造或变化后重抓已冻结基准。是否修复b的APILevel缺边需c实测，不能把静态方向写成已证根因。

`build/r38-export-preparation-tests.patch` 的11项候选已通过apply_patch接入，依据见同名前缀的tests-authority报告。包含正式metadata→normal header同identity、annotation/default和body能力隔离、alias/递归泛型bounds、constant字段storage类型、canonical隐藏引用、非planned初始化中间target，以及旧Result配新ref/roots/member域的拒绝。正确历史期望不变；新增测试尚未完成编译/行为验收。

当前最新窗口为 `r38-prepared-input-20261128-c`，队列与真实终态需核原日志；它选择上述回归和原SOURCE/FullId。`build/r38-export-preparation-v2-gate.md` 的复审也尚需核终态。

`58de257e` 的首批production/frontend/providers测试源码编译DONE/exit0；后续Published/Owned分支和Complete守卫有新字节，不能用该编译证明最新字节通过。

`50d33f9d` / `r38-prepared-input-20261128-b` 为最新冻结窗口，选中原SOURCE18、providers输入/载荷回归、serialization header/content request及原FullId用例。尚需核对真实终态、原XML和输入无漂移；运行中不修改源码或正确期望。独立gate和新增有意义回归仍需完成；若拒绝或出现新失败，继续按实际问题修复，不弱化校验或重抓变化基准。

Core定义输入、其余TwoPhase/PSI余额、49版本、CFIR/LL/API/IDE及原70/P0–P10最终逐项验收仍开放。原SOURCE绿不关闭这些要求。

用户另授权每10分钟向本会话发送继续指令；已通过Codex automation工具创建heartbeat `cjmp`，ACTIVE，关联本会话。它不替代本轮进程的持续核验，也不改变实现目标。
