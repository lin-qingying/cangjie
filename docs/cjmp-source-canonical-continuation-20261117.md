# CJMP canonical SOURCE 实施接续（2026-11-17）

> 最新f90e054a真实联合101/101，原SOURCE14为14/14，新四引用4/4，6841同SHA、完整原XML全部在窗口：[终态](cjmp-source-helper-20261117/source-pattern-reference-joint-101-final-results.json)。[codec接续](cjmp-source-codec-continuation-20261118.md)列出S3/S4完整、decoder/正式消费及原余额，未因局部绿而关闭整项。

> 2026-11-18的正式接入及终态见[codec接续](cjmp-source-codec-continuation-20261118.md)。原SOURCE已12/14、新四引用例4/4，联合101为99/101；局部模式完整产品已接入，空名称规则修订待两原例复验。以下各旧运行保留历史，不覆盖最新代码。

> 最新完整候选联合回归已核验：queue `b07922fa`实际97项87通过/10原SOURCE失败，canonical8/8、copy19/19、metadata36/36、helper/lambda18/18、原负例2/2和SOURCE4/14；6834输入前后同SHA，12原XML61,265字节全部在真实窗口，[完整终态](cjmp-source-helper-20261117/source-canonical-regression-97-results.json)。不是将六项与两项拼接为一次八项。随后为接入S0修正生成声明origin为明确的SOURCE派生来源，尚需该新字节复验；以下各旧运行记录保留为历史。

原 CJMP 七十项/P0–P10的前端范围、完整 SOURCE/CJO 前端交付和全部历史修改保留；后端排除余额、v10与其余原前端验收继续开放。唯一实施树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，没有提交、回退或联系停止的旧会话。

## 已核验终态

- 默认参数/lambda最终定向集为75/75，queue `d6c71a37` DONE/退出0；六原XML15,334字节均在真实窗口，6826输入与事前SHA一致。[完整证据](cjmp-source-helper-20261117/front-default-final-75-results.json)和[限定源码ACCEPT](../build/source-expression-codec-audit-v113/source-s1-final75-production-review-20261117.md)。这不关闭S1/S2完整或原条目。
- 当前代码的原SOURCE基线为4/14，queue `f9c1c6b3` DONE/退出1；十个正确完整正文失败保持，四原XML44,270字节在真实窗口，6826输入无漂移。同轮LL实际编译与文档检查通过。[完整证据](cjmp-source-helper-20261117/source-canonical-baseline-results.json)。它是canonical修改之前的基线，不是新候选复验。
- 首轮canonical八项为0/8，queue `81dd54aa` DONE/退出1；生产已编译，八个case均到达公开producer后失败于数字字面量的真实payload表示。两raw builder以String保存数字拼写，候选只接受数值对象是生产错误；保持全部正确fixture/结构/身份断言，现已修正为消费BODY验证过的原拼写。[完整原XML与逐case](cjmp-source-helper-20261117/source-canonical-first-eight-results.json)。

## 本轮实际生产候选

固定Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`与官方v113 `14276740ffec1262b81b6b5236b1d840af270b37`均按Git对象考证。[Kotlin续接](../build/source-expression-codec-audit-v113/source-canonical-codec-kotlin-continuation-20261117.md)和[官方续接](../build/source-expression-codec-audit-v113/source-canonical-codec-native-continuation-20261117.md)明确独立SOURCE产品的真实差异：语义先准备，providers再冻结，serializer只投影，不把typed CFIR复制进新normal分析域后重跑CFG。

- `cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt`：SOURCE30和三辅助角色的typed记录、opaque身份、d/e槽、pattern内层及flags、声明借用与控制回边；reserve/fill完成门与防御复制。SOURCE产品和normal分析就绪分开。
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt`：完整BODY输入、family域、函数tail/Return、局部声明与lambda、真实callee/类型、内建Assign、源数字/字符串、捕获固定点和生成声明。原CFIR/body/type/CFG/phase不因产品生成改写。
- 同目录`CfirSourceForInCanonicalizer.kt`、`CfirSourceTypeOperationCanonicalizer.kt`、`CfirSourceTryCanonicalizer.kt`、`CfirSourceSpawnCanonicalizer.kt`和`CfirSourceAdditionalCanonicalizer.kt`：range/iterator、typed is/as Match/Some/None、资源逆序构图及独立异常变量、future/scheduler和其它值域。原用户binding的SOURCE初始化只写图投影；新声明的HAS_INITIAL仅在实际提供initializer后发布。
- Tree/BuilderConfigurator新增真实`sourceCallHasSideEffect`位，resolve setCall按实际compound来源发布，S0 payload捕获此位；已通过正式生成器，没有手写generated文件。
- 新`CfirSourceCanonicalGraphTest.kt`两parser共八项，复用四个固定官方合法fixture，并验证normal域不变。模型区分`STRUCT_CREATION`与`OBJECT_CREATION`，新测试的函数值调用按官方CallStructCreation使用前者；原SOURCE14、75及其CJO断言均未改。

## 尚未关闭的生产与验证

第二轮八项已确认4/8，queue `3d8df05a` DONE/退出1：controlFlow及exceptionsResources在两parser通过；declarationsCalls的函数值callee被多包Member、operatorsConversions的写下标被当作get，两类共四失败。6834输入/SHA `7C6FA741C9C69878B68E228B1E5CF09445C965A0BF1F4C5B63FD1989456D87BB` 事前/事后一致，原XML全部在真实窗口：[results](cjmp-source-helper-20261117/source-canonical-second-eight-results.json)。

生产按实际绑定修正：implicit invoke以原函数值access作为base，不再造Member(value,value)；写下标直接消费完成的set call，不把写目标重新求get。第三轮queue `9a358f0e`已DONE/退出1、实际6/8，6834输入/SHA `D0B4301B0DDE43D021C7496CA64F7B7DB2D0487255D7A7899AEF01E85B4F8C9A`事前/事后一致，原XML均在窗口：[results](cjmp-source-helper-20261117/source-canonical-third-eight-results.json)。declarationsCalls已两parser通过；余operatorsConversions两项仍为缺少get。

当前已考证真实根因：normal Array的合法Int64下标快速路径只返回元素类型，未生成resolvedGetCall。SOURCE现在按明确的Array类型角色，在语义层通过真实成员scope与完成实参类型生产get图；tuple/VArray/RawArray保留各自原生Subscript角色，用户下标仍要求原正常解析的绑定。该修订还未行为复验，不预记八项全绿。

最新定向复验只跑剩余两parser的operatorsConversions，queue `f0891368` / session67842已确认实际运行，事前6834输入/SHA `51193353A76107C123D4B0C8B746BB06E19B3669B4D5F1B9219A5643C0BAB2BC`：[inputs](cjmp-source-helper-20261117/source-canonical-conversions-two-inputs.json)。先检查同一进程终态和窗口XML，勿重新启动或将之前六通过拼成一次八项全绿；两项通过后仍需最新候选的完整八项、S0/copy/helper回归、原SOURCE14及完整S3/codec/消费验收。

S3完整仍OPEN：optional链、String/Lock lowering、复合求值值域、effect handler的完成产品仍明确未交付；Source位置/provenance、原tail/括号事实、const负step及声明/调用绑定闭包仍需完整考证和审查。S4统一encoder/d-e-t工作队列、同代snapshot身份与全部槽完成门、S5正式decoder/内容能力、S6前端/LL消费和S7原条目全验收均未实现完成。当前producer尚未接入正式CJO写出，不能以八项模型/语义测试替代原SOURCE14或整个P8。

本轮新S3独立审查agent及第二次请求均因服务token rate limit失败，没有得到新S3 ACCEPT；先前S1限定ACCEPT与固定映射报告保留，不能伪造审查结论。主线程按固定源码继续核查、实施S0/S4并保持完整S3待审，不需用户重新授权。

S0接入前已发现真实origin问题：原factory使用ImplicitDefault，其fromSource=false，新SOURCE生成VarDecl会被当前isSourceDeclaration准入拒绝。现新增明确的Synthetic.CanonicalSource(fromSource=true)，仅用于SOURCE语义产品生成声明，原普通ImplicitDefault与normal BODY/CFG生命周期不变；还未给该修订新增行为通过记录。

origin修订后queue `50370e62`（session93717）已DONE/退出0，实际执行tree/providers/resolve编译，BUILD SUCCESSFUL（4m20s）。该结果证明当前候选编译，不替代来源准入的行为验收；下一实际接入是S0捕获canonical generated声明、类型/绑定依赖和初始化投影，然后由正式frontend传入统一encoder。完整S3、来源位置、其它surface lowering、S4–S7和原70/P0–P10保持OPEN。
