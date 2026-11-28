# RawArray 语义等值 F4-C 定向 gate（2026-11-18）

**限定裁决：ACCEPT。F4-C 原 finding CLOSED；本轮复审的完成 RawArray 重复 TP 绑定及关联调用路径未发现新的实际阻塞。当前新增回归尚无本次读取可确认的完成结果。整个 S4 保持 OPEN。**

仅复审 [上一报告的 F4-C](source-frozen-encoder-gate-rawarray-closed-20261118.md)。生产、测试、既有报告和验证工件全部只读；没有运行 Gradle、cjc 或测试，没有联系旧会话。本轮唯一写入为本新增报告，使用 apply_patch。

唯一一方工作树：`本记录所指的唯一实现树`。实际 HEAD：`b4dd742d589e4b5ee6592d454634c44f634df08a`。工作树含未提交修订，裁决同时绑定实际文件字节：

- CfirExtendSubstitution：`BA6DAFAECA36E08105AAD26B9D561F83EB469C6CBD0D7880E429142FFBB5CF1B`。
- CfirTypeAwareSupertypeProviderTest：`988F9331A0E0E22334AFEB54009DE26890F365B6DF60DC75E0EBF3F1BFC54287`。

写报告前复核文末全部 14 个文件摘要及 HEAD 均未变化。固定 Kotlin 对象为 `3c7a5041f238102285764d370665f56f587f9dc4`，固定官方仓颉对象为 `14276740ffec1262b81b6b5236b1d840af270b37`；权威正文以各自 `external` 仓库中的 `git show <固定对象>:<路径>` 读取。

## F4-C 的修订证据

[共同 matcher](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt)（L364） 在 L369–370 将重复绑定一致性检查改为：

```kotlin
existing == null || AbstractTypeChecker.equalTypes(session.typeContext, existing, semanticActual)
```

首次绑定继续在 L373 使用 putIfAbsent；等价的后续实参不替换首次保存的真实类型。非 extend 自身 TP 的匹配仍在 L366–367 比较实际 lookupTag，未因本次修订丢失声明参数身份。

[公共 equalTypes](../../common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt)（L174） 建立现有检查状态，L194–195 执行双向 subtype。完成 RawArray 输入进入 L560–569 的共同分派，先比较累计维度，再比较规范叶子的语义等值。它因此满足原 finding 的全部方向：

| 重复实参 | 本轮源码判据 |
|---|---|
| N,N / F,F / N,F / F,N；N 为逐层二维 Int64，F 为 flat 二维 Int64 | 维度同为 2、规范叶子同为 Int64；重复绑定成立。 |
| 二维 Int64 与一维 Int64 | 维度判据失败，重复绑定不成立。 |
| 二维 Int64 与二维 Boolean | 规范叶子等值失败，重复绑定不成立。 |

名义容器中的嵌套 RawArray 仍经公共不变实参比较 L586–595 使用同一 equalTypes。首次保存 N 或 F 可能使最终接口保留不同物理 Cone 表示，但接口类型的完成语义相等；这与保留真实输入、避免在 matcher 制造另一份类型产品的合同一致。

固定 Kotlin `core/compiler.common/src/org/jetbrains/kotlin/types/AbstractTypeChecker.kt` 的公共 equalTypes 在现有类型系统 owner 中处理准备、构造器和双向子类型，支持复用该共同入口。固定官方 TypeManager.cpp L1071–1079 的 IsTyEqual 同样执行双向语义关系；L915–929 的数组判据要求维度相同及叶子 IsTyEqual，L113–126 将 ArrayTy 规范为叶子/累计维度后驻留。官方 L1634–1643 的 extend 实例化一致性核对使用实际驻留 Ty。此次 CFIR 修订将其保留的两种物理数组表示接回公共语义等值，原物理 Cone.equals 拒绝点已消除。

## 实际消费与新边界

[生产 type-aware provider](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirTypeAwareSupertypeProviderImpl.kt)（L406） 在 L412–419 调用同一 createExtendDeclarationSubstitution；matcher 返回成功后，L425–426 用其 substitutor 实例化接口。重复 TP 检查已位于共同 matcher，不依赖某个测试、某个 RawArray classifier 分支或 encoder 事后修正。

复审确认原名义结构递归仍在 matcher L403–420 按 declarationTypeArguments 逐位置进入，已发布的 RawArray 降维视图未被改回物理叶子。重复 TP 一致性使用完整的公共等值；维度和叶子约束均有实际失败路径。首次实参表示、参数顺序及 sourceExtend 归属继续保留。

本轮检查范围内没有需要追加的具体生产改法。此结论限于已完成 RawArray 类型及原 F4-C 的真实 matcher/provider 路径，不扩大到通用错误/存根输入合同或其它前端生命周期的签发。

## 新增回归的覆盖

[provider 回归](../../cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/providers/CfirTypeAwareSupertypeProviderTest.kt)（L79） 建立两个形参的实际 Pair 声明、一个实际 View 接口及 extend 形参 T，目标为 Pair<T,T>，接口为 View<T>；L90 经既有 registerTestTypeContext 注册正常 CfirTypeAwareSupertypeProviderImpl 和 TypeComponents。

L96–100 对 N,N、F,F、N,F、F,N 四种 receiver 直接查询 getDirectSupertypeDescriptors，要求真实 Extend descriptor 存在、View 实参语义等值且 sourceExtend 正是该 extension。L102–104 对不同维度与不同叶子查询同一 provider，要求没有 Extend descriptor。这些断言覆盖原 finding 的实际入口与拒绝对照，未以手工写入 descriptor 代替生产匹配。

该测试在内存中构造两种完成数组表示；它没有执行新的 CJO 装载事务，也没有直接断言 predicateVisible 或 subtype 查询。本轮只按其实际覆盖范围评价，未把新增断言当成已执行结果。

## 28/28 证据与当前候选的验证边界

只读核验了 [2aebda27 队列终态归档（原JSON缺失）](source-rawarray-domain-28-results.json)（原链接路径：`../../.gradle/queue/tasks/2aebda27.json`）、[原 28 项输入](source-rawarray-domain-28-inputs.json) 和 [原 28 项结果](source-rawarray-domain-28-results.json)。7 个 gzip XML 归档均在内存解压并校验字节长度、SHA-256、实际 testsuite 计数；摘要均与现存 XML 一致，XML timestamp 均落在记录的任务窗口内。实际总数为 **28，failures/errors/skipped 均为 0**。输入/结果记录的前后聚合摘要同为 `75FC2FCD0982749EC0DD55185CACD1BA84566B53114191BB4512A3B98E45AE21`；本轮没有重算其 6849 文件全集。

这个真实 28/28 窗口明确选择了旧 provider 单数组实参用例，7 份 XML 均不含新增的 `repeated extend parameter accepts all equal raw array representations`。结果记录也明确 F4-C 当时仍 OPEN，不能把该窗口继承为本次修订通过。

当前另有 [29 项输入记录](source-rawarray-semantic-29-inputs.json)，其记录的聚合摘要为 `B42F2AAC89054DD39E1F7DCBC414753801EA6086C18AB5C69DD5492485ECB4EA`，scope 包含 F4-C。末次读取尚无对应 `source-rawarray-semantic-29-results.json`。因此本报告签发的是**本轮字节的静态限定 ACCEPT**，没有声称当前新增用例已运行通过。

## 保留范围

原 SOURCE14、完整原70/P0–P10、完整 S4、Generic final upperBounds、Alias use slot、strict capture、S5/S6 及其它原余额均继续 **OPEN**。这些余额没有重列为本次 finding，也未被 28/28 或本报告关闭。

## 字节身份

路径相对唯一工作树；SHA-256 为实际文件字节。

| 文件 | SHA-256 |
|---|---|
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt` | `BA6DAFAECA36E08105AAD26B9D561F83EB469C6CBD0D7880E429142FFBB5CF1B` |
| `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/providers/CfirTypeAwareSupertypeProviderTest.kt` | `988F9331A0E0E22334AFEB54009DE26890F365B6DF60DC75E0EBF3F1BFC54287` |
| `common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt` | `DB299F0A9CA0761ED954441EC4A8C82822D1C2AA72E3A7481B64134857C52620` |
| `common/src/org/cangnova/cangjie/type/TypeCheckerState.kt` | `DBCD9DE56F1A6CF3FE5A628B5F26D5039EAEC7CDD580B97642322634E789783F` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt` | `5689F54C1C78055116E1F322197FE9EA0671D1C6AA2A28B77665CDFE3C018975` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeTypeContext.kt` | `61B7AF180C748E42DCB129A01F6786EA89C3D26B847948DD3CDFB792A3446D37` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt` | `E5E5DB99CF3D43EFD753152FB00603C4588E49DD90659503807E120899A3473D` |
| `cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt` | `9CCC7F5CA06B52B2F21623763F97FADCD19D4CC48A3994770AE894FF89DD92F8` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirTypeAwareSupertypeProviderImpl.kt` | `3DCCEB0B3C4F6A1494D938C63726300ECCA3F6D8529EAB7EE8690632F3245F18` |
| `docs/cjmp-source-helper-20261117/source-frozen-encoder-gate-rawarray-closed-20261118.md` | `083956EDD5CC9F524DCAC675091E943E275F0477B5CC0ABC8F7FC906C8513EE6` |
| `docs/cjmp-source-helper-20261117/source-rawarray-domain-28-results.json` | `E3DCABA01ECE98D90973690B63ED169F122D923FE97519F02F3A8B108D39856C` |
| `docs/cjmp-source-helper-20261117/source-rawarray-domain-28-inputs.json` | `3808CBBB9557C7943FCEC8B22B96097A4081BBFAA464FE5B35E0D9E56D0B41F6` |
| `docs/cjmp-source-helper-20261117/source-rawarray-semantic-29-inputs.json` | `93D69ECEC75A9CD5FE32CA539A4E8A75F5492A0542E7784FAD47409C0189A2A0` |
| `.gradle/queue/tasks/2aebda27.json` | `A3999DB6CE6B2E3A303B1E897509F20ED107C998EC75B758E96F5A9E434C0612` |

验证方式为静态源码考证、固定对象读取及已有验证工件核验；未启动新的构建、编译器或测试任务。

