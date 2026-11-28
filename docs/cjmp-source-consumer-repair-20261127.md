# CJMP SOURCE 正式消费修订（2026-11-27）

唯一实现树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10、S1–S7/P8/E6/G1、H6排除余额分母和v10待办保留；局部源码接受与测试通过不授整项关闭。全部历史未提交修改和失败窗口保留。

## r32-a 真实终态及 r33 当前字节

追加完整性更正：已重新读取r30-d原stderr，存在`Could not complete execution for Gradle Test Executor 6`及`Java heap space: failed reallocation of scalar replaced objects`。因此下方“上一完整窗口”的旧叙述仅能指native终态/已报告505项，不能指完整frontend矩阵；原JSON和两流不改写。当前三份编译class的RuntimeVisible JUnit Test注解分别为CjmpTwoPhase92、Receiver28、ConstructorFinally26，共146；旧71/45计数是worker失败后的已报告范围。r33-b显式2GiB资源窗口d0aa1d09正在执行真实全量，输入9847文件已冻结；不得按旧部分Case数量或旧skip统计授完整验收。

`build/cjmp-invisible-peer-20261125/r32-full-source-formal-20261127-a-results.json` 已封存：native queue736eaafa DONE/exit1，exec87141已返回；实际544/522通过/22失败、0错误/跳过。serialization435为428/7，copy/Published46和SOURCE18（含原14）全部通过。frontend仅45项为30/15；finally26因worker Java heap space未产生XML，不能把JSON的complete=true（native执行结束）当作完整71或全矩阵通过。53份gzip原XML逐份解压核长度/SHA成功，9846输入前后SHA均为c4259f81ff598f68e1cded6abf78d9dcf33e957801b12d1e33d5bda43aa4b96f，SDK84前后同SHA。

r33已应用完整FullId标量冻结与v3 raw matrix，保留decl的null/空/opaque、原index及真实binder域，CURRENT只重映射输出index，IMPORTED重映射包编号；ArrayLit的bool两态按schema保存，native为false，空factory写FullId(-1,空字符串)。旧R1/T1/T2 REJECT与v1/v2候选保留，v3及生产候选限定静态ACCEPT见build/r32-fullid-slot-tests-v3-gate.md；行为待新窗口。

Primary已走原Library工厂/Manager/provider绑定Object父类，两个原无参testkey及全部原flags/数量/link/accessor断言保留。Initialization在首次super/status前固定最终provider，仅用原registry真实BODY processor补缺失工作，保留原树/初始phase、不重放TYPES、不降阶段；核真实class/file CFG、owner完成、原符号/来源/provider及checker异常。lazy与缺Any负例保留，BODY/Primary限定源码ACCEPT见build/r33-body-primary-fixture-gate.md。RAW全链候选未应用，其eager缺口独立保留，不再无条件阻断上述五例BODY setup。

Finalizer首F现在按原header.bodyAvailable选择所存非零BODY，body0 constructor不伪造正文。真实NestedOther归档栈是Reader.reference的SUPER kind检查，而不是匿名BODY缺入口；固定官方字节明确super()的callee是函数类型与Object constructor FullId，finalizer super则是Class类型与Parent ClassDecl。decoder按实际RefExpr/MemberAccess、完成函数类型和拼写生产角色，实际目标仍由原binder绑定并保留；Reader守卫未降级。考证见build/r33-receiver-reference-kind-authority.md；新行为与角色复审待核。

compile e97fb80a因新增测试继承assertNotNull返回Unit/helper缺opt-in失败；修正后facaa584测试源码编译exit0。82377cee的后续角色编译和validateDocumentation也exit0；编译期间修订不替代新冻结行为。验证runner新增独立FinallyOnly、显式FrontendHeap2G以及missingSelectedXml/nativeWorkerFailure/selectedSuitesReported；2GiB只给指定frontend Test资源，不改变测试选择/断言/算法，原r32-a结果不改写。下一新窗口验证当前字节和缺失分区，原70/S1–S7余额继续OPEN。

## 已核对的上一完整窗口

`build/cjmp-invisible-peer-20261125/r30-full-formal-frontend-20261127-d-results.json` 为真实终态：505项、466通过、39失败、48原XML，输入和SDK前后同SHA。范围为serialization434与选定frontend71，不能替代历史frontend92或完整140。原queue `c2d3648b` DONE/exit1。另一个独立窗口r30-c为正式consumer111/111，不与d重复计数；r30-b的SOURCE18和copy/Published46通过也不被冒称为d已执行。

r31的DefaultMetadata、FullId及Receiver caller候选已应用，queue `df2d634e` 的serialization/frontend测试源码编译成功（exec61872终态已核），但尚未据这次编译补记行为通过。它们修正完整ClassInfo、helper真实desugar owning槽和显式package查询，保留原结构/语言期待。

## 当前实际修订和审查

- MatchCase绑定计划统一核验所选case的全部已登记occurrences，包含此前成功发现的metadata上下文；只读原scalar proof，不读未选Match正文。不同selector身份提前报明确状态错误；immutable `matchCaseSelectors` 交给原decoder，删除晚期`single()`冲突检测。共享Graph Builder接受同一Match中幂等重复case槽，仍拒不同Match owner。新增同selector共享record正例，原跨selector负例及拒绝后header身份检查保留。`build/r32-selector-package-gate.md` 的G1历史REJECT保留，`build/r32-selector-package-regate.md` 已限定源码ACCEPT。
- PublicPayload整包oracle显式请求packageSourceGraphOf；先取单F产品再核package复用body/record/header身份，不用先全包预载来替代单F消费。
- Annotation人工seed的两个`19`分别归参数default与annotation Block，现分为E8/E2独立拥有节点；local initializer E3、所有原值、损坏未选正文和package initializer期待保留。固定N ownership考证与该迁移限定接受见 `build/r32-selector-varray-authority.md` / selector regate；不放宽sameContextIdentity。
- VArray保存字段与语义factory分开：共享payload携带格式owner提供的不可变saved-reference；decoder捕获原FullId来源和binder身份，不绑定ignored target。最后输入边界按原snapshot/binder及声明kind冻结CURRENT header定位，save-order在args拥有孩子后预留保存引用，encoder按TYPED_SCHEMA保留原slot并重映射、NATIVE_SOURCE按实际语义factory写空目标INVALID。测试保留两次再导出raw CURRENT及真实VArray类型/body排除，纠正无官方依据的VArray语义factory绑定期待。固定N读取路径确实跳过TYPE_VARRAY；独立源码审查与行为仍待核。

queue `5afb778c` 为selector初稿测试源码编译成功；后续queue `984a0421` 为共享saved-reference及consumer测试源码编译成功（exec90367已返回）。编译期间另有G1最后修订，行为必须从最终磁盘字节重新冻结/编译；两次compile无独立全输入行为manifest，不替代验收。

## 初始化生命周期和剩余工作

`build/r31-post-body-scope-phase-{mapper,authority}.md` 确认五个初始化用例的Box仅到SUPER_TYPES；保存前必须由真实processor完成owner/member产品，producer不得设置假currentPhase、降低阶段或重推断源码。

`build/r32-initialization-lifecycle-candidate.patch` 已落盘、git apply --check通过但尚未应用/编译/执行。它让新fixture从正常RAW构造并通过原registry驱动实际before/process/after，保留五case和lazy/external负例；严格检查揭露普通eager pipeline的EXTENSIONS/IMPLICIT_TYPES/BODY成员推进缺口。该缺口不能靠phase标记解决，正按固定K对应processor/transformer继续核对。原负例拒绝来源、PrimaryConstructor正式跨包Object消费、frontend四个正确conditional assignment/compound诊断及TwoPhase五例仍须当前字节验收，不因SOURCE14曾通过关闭。

下一行为窗口使用原runner完整serialization并加入PublishedTree、SourceChain和FrontendConsumers，事前冻结输入/SDK、独立原流与逐case ledger；真实终态后按原XML继续修订。不要把本文的源码状态当作该未来窗口PASS，也不要覆盖r29未完整窗口或r30-d全部原失败。
