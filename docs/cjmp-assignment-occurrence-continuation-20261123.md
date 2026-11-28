# CJMP 赋值事件与 mini 映射生命周期接续（2026-11-23）

唯一实现树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10、完整SOURCE→CJO→正式消费继续；本片不关闭完整F3/G3/SOURCE或任何原整项。H6排除余额保留分母，v10待办不阻断授权工作。历史未提交修改及失败/拒绝全部保留，没有提交、回退、清理或修改既有正确测试期望。

## 本片生产修改

`CfirAssignmentExecution`在原resolve/dfa owner保存完整源序目标与槽位路径，包含重复/嵌套/discard位置；`AssignmentOccurrence`以真实owner对象身份与持久化path比较。静态写事件不是runtime value generation，也没有临时AST/symbol。通用完成值visitor合同尚待完整consumer接入，不能把目标枚举当作运行时执行已完成。

mini逐实际目标登记写事件；`CfirDataFlowAnalyzer.recordAssignment`按同一目标路径投影完成RHS类型，精确填入对应声明/owner/path。错误tuple结构不补默认叶类型。normal的raw augmented→resolved assignment及optional success assignment显式重绑同一次事件；replay调用共同入口，借用reference经实际symbolMapper投影而不改写。

mini snapshot在全局索引、fork、scope及postponed集合中共享同一个本次Assignment克隆。源遍历访问真实事件owner与已填leaf type。首次非恒等snapshot安装本次mapper；已有Source→T1时组合T1→T2。声明查询、事件查询、根/函数/loop owner与稳定性查询统一到有效域，防止旧key查不到而误判stable。该验收只证明mini自己的域，不代表Flow/RealVariable/type的全换代。

实际行为暴露NamedFunction的generated dispatch直落visitElement，原visitFunction未运行；已按固定K显式接visitNamedFunction→既有helper/default与lexical fork owner。这是正常局部函数/default helper的生产入口修复，不把声明闭包当执行正文。没有盲补仓颉不支持的Kotlin局部nominal捕获。

## 实际独立窗口

| 队列 | 实际终态 | 边界 |
| --- | --- | --- |
| f26dd82c | DONE1，行为0 | PowerShell拆开未引用的JVM参数；不能算生产编译。 |
| 70ff03f9 / 52911032 | DONE1，行为0 | 新fullyExpandedType调用缺少真实resolve扩展import；529完整终端保存。 |
| 556961f2 / 35c23746 | DONE0，中间生产编译 | 之后仍有生产修订，不替代最新行为。 |
| f52f0565 | 58项49通过/9失败，DONE1 | 新9项共同暴露NamedFunction缺fork；8原XML全窗、同输入/SdkSHA。 |
| a328f982 | 59项58通过/1失败，DONE1 | Named/S2-1修后，仅本轮新第7负例错误地把M域内Source owner当非法owner；8原XML全窗、同SHA。 |
| 092c9286 | **415/415，0失败/错误/跳过，DONE0** | serialization369＋analysis28（SOURCE18/canonical8/unexported2）＋resolve18（新10/Context3/capability5）；53原XML全窗、同SHA。 |

窗口分别保留，不把58/59/415及之前405重复相加。最新10032选定输入SHA：`76245883089741799F5ED232744FFE0991C8E23CB79AF655BDF4A117732F7E99`；SDK实际前端84个`.cjo/.d`输入SHA：`527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`，均前后相等。

证据：[092c结果](../build/cjmp-dfa-lifecycle-20261123/092c9286-results.json)、[输入窗口](../build/cjmp-dfa-lifecycle-20261123/092c9286-input-window.json)、[原XML证明](../build/cjmp-dfa-lifecycle-20261123/092c9286-original-xml-proof.json)。每个原XML的gzip载荷可还原精确字节，长度/SHA逐项复算，classname#case及状态与ledger一致。两旧失败同样保存在各自results/window/terminal及[失败XML证明](../build/cjmp-dfa-lifecycle-20261123/assignment-failure-xml-proof.json)。没有用旧XML、UP-TO-DATE报告或编译终态补行为通过。

## 审查与新增测试修订

[第一源码门](../build/cjmp-dfa-lifecycle-20261123/assignment-occurrence-first-gate.md)与[第二源码门](../build/cjmp-dfa-lifecycle-20261123/assignment-occurrence-second-gate.md)保持原时点，第二门S2-1拒绝不回写。[第三门](../build/cjmp-dfa-lifecycle-20261123/assignment-occurrence-third-gate.md)接受Named/S2-1及mini声明/事件域；[独立415行为门](../build/cjmp-dfa-lifecycle-20261123/assignment-occurrence-behavior-gate.md)复算当前输入、全部原XML和ledger后限定ACCEPT。完整F3/fullSOURCE/原70不授绿，旧失败仍是独立窗口。

新10项通过公开recordAssignment/rebindAssignment、scope与SourceVisitor观察重复/嵌套、倒序报告、相同source不同owner、shared snapshot clone、Source→T1→T2及默认P=null首次换代；不读私有Assignment表，不手填Flow。本轮新第7输入确实修订：由mapper明确域内的Source owner改为真正未登记/未映射的同source foreigncopy，保留负向、重绑、leaf type与旧树隔离断言。真实a328失败和独立合同依据见[测试facts](../build/cjmp-dfa-lifecycle-20261123/assignment-occurrence-test-facts.md)及第三门；不声称新测试从未调整，也不改已有repository正确期望。

## 继续完整F3及原计划

[旧value候选](../build/cjmp-dfa-lifecycle-20261123/tuple-assignment-value-candidate.patch)仍未应用。[固定K核心复核](../build/cjmp-dfa-lifecycle-20261123/value-core-mapping-20261123.md)确认V1–V7：本次completion/staging归属、同一join事务的对齐identity、common/union真正writer语义、成员代表/assignment身份迁移、统一condition/effect/alias事实键域、单一事实owner以及完整失效事务。这些需整批兑现，不能只增加API后开放tuple readiness。

[目标语义裁定](../build/cjmp-dfa-lifecycle-20261123/value-target-authority-20261123.md)明确ordinary tuple根RHS一次→逐叶address/write，独立optional RHS留在成功支，compound/increment保留address/get/RHS/op/set真实DAG及异常/短路。mini与replay目前仍保留原普通visit顺序，normal目标准备也仍可能提前发DFA事件；本片没有把它们记为完成。随后与完整ValueBinding/SSA/projection及失效/合流/快照一起接入同一执行产品。

[正式typed setter全链坐标](../build/cjmp-dfa-lifecycle-20261123/typed-setter-argument-implementation-map.md)给出原ConeResolutionAtom/CallInfo/Candidate/mapping/checker/completion/诊断/Source owner的必要变化。现有resolvePlainArgumentType可复用，但CallInfo建atom、mapping重建atom及writer按expression合并会丢投影身份，不能只调该方法。固定N FuncArg单operand及mapExpr不直接编码opaque tuple path；需在SOURCE canonicalizer证明真实官方载荷的根一次完成/投影/前缀关系，不能以假CFIR AST、根type伪装或未版本化私有字段绕过。

native Some/None、模式payload/Binding、SourceCall捕获/完成、正式sparse replay与enum F、取消/epoch以及CLI/Common/ordinary/depends-on/LL继续原授权验收。CLI原独立92项/16失败与type-projection准备仍OPEN；不按ClassId放宽constructor、跳supertypes或按名字隐藏overload。

固定对象仍K=`3c7a5041f238102285764d370665f56f587f9dc4`、N=`14276740ffec1262b81b6b5236b1d840af270b37`、R=`4243106ba19d4885831a5868b1522bbdb757dee1`。PowerShell7/UTF8/apply_patch/gradlew-queue.bat；目标active。只读核对cjmp为ACTIVE、每10分钟，本轮未删除、暂停或修改定时任务。
