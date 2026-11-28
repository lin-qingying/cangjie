# CJMP 冻结类型 encoder 接续（2026-11-18）

> 11月19日新增来源/Paren/公共原型、正式Alias/ABI套件与TypeUse候选见[新实施记录](cjmp-source-type-use-continuation-20261119.md)。a39e14e2的两Alias例已从原句柄读取真实失败，当前RHS期望Type/实际Tuple；新来源G2/G7仍拒绝。下方184/29与“套件未新增”保留为旧时点，不覆盖新候选，也不证明新字节通过。

> 最新d4288e8a真实29/29、6849输入SHA同为B42F2AAC89054DD39E1F7DCBC414753801EA6086C18AB5C69DD5492485ECB4EA、7原XML均在窗口。[完整29证据](cjmp-source-helper-20261117/source-rawarray-semantic-29-results.json)。mixed nested/flat重复TP的实际provider对照及不同维度/叶子拒绝通过；[有限静态ACCEPT](cjmp-source-helper-20261117/source-frozen-encoder-gate-rawarray-semantic-equality-20261118.md)关闭F4-C。原SOURCE14再次通过；完整S4、S5/S6与原前端/P0–P10仍OPEN。最后验证句柄8186/队列d4288e8a已DONE退出0，未留待恢复的Gradle。

本记录接续 ordered-types、snapshot 和 codec 记录，不缩减原70项/P0–P10前端范围，不实施后端。唯一写入树仍为 本记录所指的唯一实现树；全部历史 staged/unstaged 修改保留。

## 已核验的新窗口

fc048caa 实际184/184，0失败/错误/跳过。6849输入事前/事后 SHA 40FE9AF001451D0404E98001A3212E7428E8E769B1223FD3C48D6B39BFED0BC6 相同，19原XML全部在真实队列窗口。证据：[完整184结果](cjmp-source-helper-20261117/source-frozen-encoder-184-results.json)。原SOURCE14再次实际通过；另包含新增四个官方引用用例、复制19、S0 39、canonical/helper/负例、stored-CJO30、native RawArray layout2和类型关系6。不得据此关闭整个S4/S2/P8或目标。

7d8aeb81 的原计划178窗口实际只执行104/104；analysis-tests:compileTestKotlin期间 JVM native malloc 1285168 bytes 失败，SOURCE未执行。RawArray筛选误接在tree任务后，没有执行新两例。6849 aggregate SHA漂移，不能称同代完整验证。仅纳入真实窗口中的三模块报告：[104部分终态](cjmp-source-helper-20261117/source-frozen-encoder-partial-104-results.json)。原hs_err_pid23000.log保留。后续user-home Gradle配置通过apply_patch加入HeapBaseMinAddress=8g，fc048caa实际成功，不修改生产语义或压低正确期望。

## 生产修订

- Producer类型编码退休activeTypeSubstitutor、Cone.equals memo、classifier provider查询及expressionOrNull；冻结payload、捕获domain、真实binding、固定TP投影和完整有序shape进入同一t池reserve/fill。
- Class/Struct/Enum self按native条件双memo：已有specific槽才复用；均无槽时预登记双方并编码原输入。Generic以domain/projection/parameter锚点独立排队上界，在结构shape完成后准备，填表不换声明域重查payload。
- Alias声明self为Type＋own TP＋Alias绑定，RHS单独冻结/编码；具体使用槽的Alias保留选择仍未交付。
- RawArray normal保留构造实参和原生UInt维度，布局保存规范叶子与累计维度。loader读取dimsOrSize；共享复制及两个实际receiver/reference恢复入口透传标量。共享类型系统按累计维度和不变叶子比较；声明级降维视图独立于marker规范叶子。
- semantics.api新增CfirAnnotationExportPlan，SourceGraph生产计划实参，Frozen/TestRegistration为空参数。S0完整保存parser-owned Attribute/overflow/when语法域，不伪造类型或callee。实际Source保存闭包消费计划target/args及canonical类型、reference、array initializer、patterns；两类CJO origin统一消费原始Anno/arg.expr及loaded snapshot。

## 固定审查和继续修订

[五路径有限gate](cjmp-source-helper-20261117/source-frozen-encoder-gate-final-20261118.md)静态ACCEPT F1/F2/F3/F5，R2/R6未回归；F4-A/B仍REJECT。该报告绑定17AF2406…候选；184行为通过不能替代F4拒绝。

随后已修F4-A/B：marker函数replaceArguments/erase统一typeSystemArguments规范叶子；declarationTypeArguments集中提供RawArray降维视图，extend matcher/predicate、class substitution、extend assumption及声明父类型替换共同消费。新增函数恒等/deep替换和真实type-aware provider原生BuiltinDecl RawArray嵌套/flat receiver回归。2aebda27原句柄11378已DONE/退出0，实际28/28、6849输入同SHA、7原XML真实窗口：[28结果](cjmp-source-helper-20261117/source-rawarray-domain-28-results.json)。随后F4-C重复TP的物理Cone.equals出口在共同matcher改为公共equalTypes，并由新29窗口单独验证；未将两轮计数相加。

## 新官方类型输入

文档链接/H1规范已修，validateDocumentation最后实际通过；原127及后69违规窗口分别保留。十份原报告/接续字节完整gzip归档，已解压核验长度/SHA；格式改动不替换原finding或生产候选SHA。[文档终态及原报告验证](cjmp-source-helper-20261117/source-frozen-documentation-results-20261118.json)。

orderedAliasRoles.cj与functionAbi.cj已由固定cjc 1.1.3编译/flatc解码：分别48声明/42类型/27表达式，36声明/11类型/42表达式。cjc实际7和4条chir unused警告，原诊断保存；没有伪造无警告，也没有执行机器码。源SHA分别6B911947B0533ED9D7889C1CCAF820527A0349BF68F90B9B6130E41ACF92C996、786C2DA1E0B7063A93280F708DAE892877224D9092ED905464BACC4FE1C869B7。完整原CJO压缩及AST/JSON/SDK/schema摘要见[官方类型证据](cjmp-source-helper-20261117/frozen-type-roles-native-evidence-20261118.json)。这两份正确源尚未接入新增frontend Kotlin套件，不计为本仓通过。

下一实施仍是完整S4：Generic最终语义upperBounds与独立原始constraint分组/位置/implicit来源，Alias self/use/RHS的具体节点资格与完整冻结投影，严格capture退休默认fullyExpandedType/provider阶段推进。随后S5统一decoder/materialization、S6正式ordinary/common/depends-on与LL选中正文消费，继续S3其余canonical及原前端全部余额。当前TypeKind.Type decoder仍报Unsupported、createRecursiveTypeFallback及四种annotation表达式reader还在，库函数body仍null；这些均未关闭。

原C3/C4/D5/E6/E7/E8/E9/E12/F4/G1/G3/G5、H1/H9/H10/H13、H6前端门、P0/P10、49版本和v10全部保留。v10暂缺不阻断授权实现；H6排除后端余额保留分母、不实施、不计完成。固定Kotlin3c7a5041f238102285764d370665f56f587f9dc4与native14276740ffec1262b81b6b5236b1d840af270b37继续git show读取。PowerShell7/UTF8读取/apply_patch写入/gradlew-queue不变，不联系停止旧会话。
