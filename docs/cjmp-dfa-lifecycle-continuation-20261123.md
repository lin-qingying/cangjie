# CJMP 公共 DFA 生命周期接续（2026-11-23）

> 最新[tuple求值接续](cjmp-tuple-value-continuation-20261123.md)：真实tuple enter/逐元素/aggregate节点、normal/replay/Source/snapshot/Void接口已接入；056实际23/23、2ed SOURCE独立28/28、bc481实际24/24，各自同SHA，不累加。完整Value core、执行顺序、typed setter与pure canonical D正式链继续实施，tuple语义就绪门未开放；旧415属于前一字节历史。

> 最新[赋值事件接续](cjmp-assignment-occurrence-continuation-20261123.md)：092c9286实际415/415，同10032输入/84SDK SHA；精确owner/path、mini共享克隆、首次/二次映射与真实NamedFunction/helper fork已接入。f52的58/49/9、a328的59/58/1及本轮新fixture错误输入修订保留。完整F3/value/执行顺序/正式消费与原70继续OPEN，旧405不替代新窗口。

> 包装/调度后续已新增并实跑6项：178fcc37实际49/49，9e05b53f SOURCE联合405/405，0失败/错误/跳过。10030输入SHA `B655CC7889248A40BACC18E891943E07C64FDACEABC095A76CD55B54E199CAF1`、SDK84同SHA；52原XML无损ZIP SHA `E734FE9790B02BDA37B323B3F79464FB49C88F7EC28C53D23A2D193AF189164F` 已逐entry复算，[当前结果](../build/cjmp-dfa-lifecycle-20261123/9e05b53f-results.json)/[窗口](../build/cjmp-dfa-lifecycle-20261123/9e05b53f-input-window.json)/[证明](../build/cjmp-dfa-lifecycle-20261123/wrapper-original-xml-proof.json)。下文43/399均保留原历史独立窗口，不累加、不替代新字节。

## 包装/调度增量及覆盖边界

L1：resolved replay 的透明实参递归只接受实际 NamedArgument/InoutArgument，Optional/OptionalChain保持原 payload 和语义能力门；L2：driver按 exitArgs→enterFunctionCall→deferred body→exitFunctionCall，自然保持所有节点/flow/source身份。原[lambda拒绝](../build/cjmp-dfa-lifecycle-20261123/lambda-scheduling-gate.md)与[第二源码gate](../build/cjmp-dfa-lifecycle-20261123/lambda-scheduling-second-gate.md)不回写。Source读取/消费的完整未ready产品不因测试放宽。

[真实before快照](../build/cjmp-dfa-lifecycle-20261123/wrapper-tests-before.json)在新增case前保存当前T全文与SHA；[writer差分事实](../build/cjmp-dfa-lifecycle-20261123/lambda-wrapper-test-facts.md)逐方法核验原7 case及共享assert/helper保持。该证据不虚构L1/L2更早修改前的整文件，早期只据实际工具执行patch与合同核对。新的声明级header bootstrap使用production StatusResolver，fixture中的Source status确已推导才进入正式copy/rebuilder。新exact消息由真实node消费门决定，图级无节点pending门的旧新测试期待不正确，已按[实际消息证明](../build/cjmp-dfa-lifecycle-20261123/wrapper-semantic-gate-expectation.md)仅修字面量；ABORTED/no readiness/no published CFG/source身份断言保持。

49＝position13（旧7＋新6）＋life7＋旧For21＋resolve8；405＝serialization369＋analysis28＋resolve8。named包装lambda、可选链scalar与直接lambda模型均实跑；inout临时lambda仅内部CFIR结构边界，不算合法仓颉源码。原e3f5967f 49/47/2预flight失败原XML保留；新状态准备/消息修正后49/49是独立窗口。编译失败与e922旧消息失败不伪称保有未归档的原XML。

完整G3/F3、value-generation/projection/Binding、native payload及SourceCall捕获/正式消费继续Pending。所有control-only/graph-pending产品是目前防错误发布的明确义务，完成真实transfer后必须履行解除，不作为永久排除余额。

[包装第四有限行为gate](../build/cjmp-dfa-lifecycle-20261123/lambda-scheduling-fourth-gate.md)已独立核验L1/L2、V1/V2/V3与49/405两窗口，限定ACCEPT；旧源码/预flight/消息错误拒绝原样保留，未扩大为完整G3/F3或原目标完成。

唯一实现树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10、完整SOURCE→CJO→正式消费端继续；H6排除余额保留分母，v10待办不阻断其它授权工作。保留历史未提交修改、正确测试与全部失败/拒绝，没有提交或关闭目标，没有联系停止的历史会话，没有修改定时任务。

## 最新真实行为

| 窗口 | 终态与必要边界 |
| --- | --- |
| c6e68b00 | 最初公共analyzer生产编译DONE0，早于后续整改，不证明后来字节。 |
| 663e39a4 / 6e9797e3 / e366d00e / b44bdc65 | 各自编译失败：新driver必填字段/API、node状态注解、tuple IR命名及新position测试import等；行为0不引用旧XML。 |
| 62ba91ff | resolve＋LL生产及resolve/serialization测试源码联合编译DONE0。 |
| 0476fa42 | 实际36项27通过/9失败，inputSame=false。Source/builtin驱动缺失、mini注册及typed-body图发布根因保留。 |
| a6835a1d | 实际43项40通过/3失败，同SHA窗口：while出口Int被混成Any、partial snapshot错误读nodes、lambda body提前消费split。 |
| 3520bfda | 最新43/43，0失败/错误/跳过。life7＋position7＋旧For21＋Context3/capability5，真实flow/版本/位置/必要路径验证。 |
| ca20506f | 最新399/399，0失败/错误/跳过。serialization363＋analysis28＋resolve8；SOURCE18/canonical8/negative2、SDK5均实跑。 |

[399逐case](../build/cjmp-dfa-lifecycle-20261123/ca20506f-results.json)、[输入窗口](../build/cjmp-dfa-lifecycle-20261123/ca20506f-input-window.json)和[原XML证明](../build/cjmp-dfa-lifecycle-20261123/original-xml-proof.json)保存实际终态。10030选定输入SHA为 `C83CBDD5D492B32B836BAD7200E75515DAFC0EF2072D5F9A922C2743FFBBEC45`，SDK84仍为 `527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`，均前后同SHA。52原XML无损ZIP分三份JSON，ZIP SHA为 `59B0A97D42B0D6E8FACB539B95621B2EFC7C9054362F6C97E1730935BCB04E02`，每个entry SHA与真实ledger复算相等。

[399独立行为gate](../build/cjmp-dfa-lifecycle-20261123/behavior-source399-gate.md)复算所有原XML、ledger及输入/SDK，限定ACCEPT；仍不覆盖既有源码拒绝或完整G3/value/正式消费余额。

43窗口的输入在队列编译时捕获，但逐XML timestamp证明早于JUnit，前后同SHA；399输入在调用前冻结。两窗口单独保留，不重复累计。旧0476漂移和所有旧失败不回填成通过。中间单方法诊断/driver编译失败只据各自终端记录，不冒称保有未归档的原XML。

## 实际生产基础

- 公共CfirDataFlowAnalyzer接入固定Kotlin的default/alternate incoming→transfer→freeze、currentSmartCastPosition、真实局部存储/初始化/赋值失效、while/doWhile退出恢复与Bool短路/IS/NOT事实。normal及resolved replay共用同一owner；借用目标使用同次完整symbolMapper。
- builder返回LambdaExitLayer及实际If/For/Match中间节点，模式原子/guard入口先合flow再求guard；普通闭包只构造/捕获，不因传参执行正文。真实lambda body在arguments出口/split完成后调度，保留原AST/symbol/target身份。
- Match出口恢复common角色；Match success/failure是finally内部正常分叉，备用路径不中断。默认参数fresh子图有outer→inner与innerExit→outerExit；已完成默认图不重放或与新epoch输入混合。
- Context旧VariableStorage已退休，全部变量表属于Flow；snapshot/fork/source遍历保留实际表与历史。隔离解析精确保存实际游标，snapshot/LL partial也记录恢复游标和Source关联，不按lastNode猜调用实参后的位置。
- 独立PatternBinding/Field/ValueParameter的generated visitor入口委托mini原词法注册协议；loop key、freeVariables retain和assignment事实不变。typed body只有与实际CFG同一产品才视作已完成；无CFG的typed body仍发布本次分析图。
- 普通while/doWhile按实际Bool literal接死亡正文/回边/出口，未知Bool仍保留真实分叉。没有在类型恢复处硬替换Any→Int，正确类型/alias断言保留。

## 完整语义产品的边界

节点现在区分UNINITIALIZED / CONTROL_FLOW_READY / SEMANTICS_READY；必要finally alternate要求与实际map分开保留、复制和验证。没有独立CFG node的实际判别仍登记图级pendingSemanticTransfers。owned rebuilder发布及后续requireReady同时核实际事件、node transfer和required paths，不能仅以flowInitialized授COMPLETE。

这只是防止未闭合语义被错误发布的产品协议，不是永久排除原要求。当前明确待实施：完整tuple目标执行/完成值generation/projection/Binding、普通模式与native Option判别、无initializer P的真实payload绑定、SourceCall捕获/完成生命周期，以及这些值关系的全部失效/合流/快照。DIRECT For没有完整执行模型时也不获语义ready。现有原node/body/type/phase/source不删、不降阶段、不填空Flow。

[原基础gate](../build/cjmp-dfa-lifecycle-20261123/analyzer-first-gate.md)、[五项整改gate](../build/cjmp-dfa-lifecycle-20261123/analyzer-second-gate.md)、[readiness四轮](../build/cjmp-dfa-lifecycle-20261123/readiness-fourth-gate.md)及[43行为gate](../build/cjmp-dfa-lifecycle-20261123/behavior-43-gate.md)独立保留。完整G3/R5/S2/S6、SOURCE与原70/P0–P10未关闭；399不是正式CLI/LL跨模式完整矩阵。

## 下一实质实施

按 [F3完整合同](../build/cjmp-dfa-lifecycle-20261123/tuple-assignment-value-contract.md)及其未应用候选落实同一完整产品：RHS一次求值，所有目标源序写入，下划线无store，nested/重复目标/swap必须保留原完成值，setter/optional异常只提交实际前缀；normal/mini/replay统一，不造临时AST/symbol，不把RHS tuple类型填给每个叶。候选还未应用，不因核心API存在就开放tuple的ready。

随后沿 [native transfer authority](../build/cjmp-dfa-lifecycle-20261123/native-flow-transfer-authority.md)完成Some/None、tuple/type/enum/literal/OR投影和本轮值关系，不导入Kotlin null/None-as-Nothing或未经证明的calls-in-place；再验正式sparse replay与enum F完整生命周期。固定对象仍为K=`3c7a5041f238102285764d370665f56f587f9dc4`、N=`14276740ffec1262b81b6b5236b1d840af270b37`、R=`4243106ba19d4885831a5868b1522bbdb757dee1`。

正式CLI独立92/16失败窗口与 [type projection准备](../build/cjmp-formal-consumer-20261122/type-projection-implementation-map.md)继续处理。必须由真实declaration/provider/family证明比较输入同代，保持严格constructor、原FullId/physical owner/位置；不按ClassId放宽、跳supertype或按名字隐藏overload。完整ordinary/common/depends-on→CLI/LL、原剩余ID及P0/P10验收仍按原分母推进。

所有Gradle均通过gradlew-queue.bat、PowerShell7/UTF8读/apply_patch写，仅改指定树。目标active。
