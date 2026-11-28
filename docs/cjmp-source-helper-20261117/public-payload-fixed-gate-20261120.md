# KotlinParityGatekeeper：公开 SOURCE payload 有限复审（2026-11-20）

**ACCEPT 本批 F7/F8/F9 的公开载荷修订，撤销 F4/F5/F6 中已经消除的具体调用路径拒绝。完整 S5/S6 保持 OPEN。** 本次范围内没有仍需修改 production/tests 的实际阻断；下述未覆盖行为继续保留验收资格门，不重报已修 finding。

唯一审查树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`；HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`，审查对象包括未提交文件。按本次授权仅以 apply_patch 写入本报告；production/tests 只读，不运行 Gradle/cjc、不联系旧会话。采用 kotlin-cangjie-frontend-parity 的 Gatekeeper 规则，用户的有限复审范围覆盖其实现、写测试和运行验证步骤。

## 固定依据

已读 [公开载荷 continuation](../cjmp-source-public-payload-continuation-20261120.md)、[原 F4–F9 拒绝点](s5-source-decoder-kotlin-parity-gatekeeper-20261119.md)、[后续限定门](source-decoder-gate-current-20261119.md)、[native SOURCE authority](s5-astloader-source-native-authority-20261119.md)、[Kotlin mapper](s5-kotlin-framework-mapper-20261119.md)、[JString authority 与官方 probe](../../build/cjmp-s5-type-20261119/jstring-authority.md)。

- compiler 固定 `14276740ffec1262b81b6b5236b1d840af270b37`：FuncBody header/body 分离；StringKind 四值；MatchCase 根 pattern.ctxExpr；Position 的独立包/文件基数；ConstPattern SOURCE 不写 values。沿用本树固定 authority 的原文坐标及本次用户确认的语义事实。
- Kotlin 固定 `3c7a5041f238102285764d370665f56f587f9dc4`：本轮通过固定 URL 重读 [FirMemberDeserializer.loadFunction](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/FirMemberDeserializer.kt#L618) 和 [ValueWithPostCompute](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/caches/ValueWithPostCompute.kt#L50)。前者在声明所属 child context 恢复签名/参数；后者区分内部 postCompute 可见与公开完成观察，异常重置未计算状态。本地 CJO 特有 header/string/pattern 格式产品不冒称 Kotlin 同名文件原样复制。
- runtime/stdlib 固定 `fc9f54db5d92079eb1845968abe98e52df92d70d`，docs 固定 `70281ea0ecb4adb86260ac233cf87fb4dbb23bc9`。native 无 JString 引用时 `J"same text"` → `sema_undeclared_type_name` / Invalid；其他三种形态是真实 String。该官方证据已存在，不能重新以 native core 有 JString 为前提拒绝纠正后的输入。

本树 external 两个目录为空，本轮未从另一工作树读取 production/tests 或声称重新 git show 本地缺失对象。native/runtime/docs 使用上述已固定证据；Kotlin 原文使用同一固定 SHA，未改用上游 HEAD。

## 原门逐项判定

以下简称与当前文件指纹见末表；坐标均为本树当前行号。

| 原门 | 本批判定 | 当前代码与证据 |
| --- | --- | --- |
| F7：named 原 header 被公共 API 丢弃、body0 header 不可查询 | **ACCEPT 有限公开 header 门，撤销原缺失 finding。** | API:8–12 正式提供 sourceHeaderOf；P:69–72 → AP:248–252 按声明 provenance 的唯一 sourcePath 定位同一 context；AP:485–490 验证 declaration 对象身份后查 completedSourceHeaders。L:42–47 与 D:386–408 共用 header reader，读原参数分组、平行 desugars 的 null/真实 NamedFunction、retType 有无、captureKind、always、bodyAvailable。G:249、310–317、435–437 发布并校验原 header；D:130–146 在 body0 早退前登记 header，L:33–36 保留已登记 body0 函数。 |
| F8：四值 StringKind 压成 bool | **ACCEPT 四形态保存/映射门，撤销原压缩 finding。** | G:85–86、142–145 的主字段是四值 stringKind，isJavaString 由枚举派生；D:191–201 严格映射 Normal/JString/MultiLine/MultiLineRaw，未知值失败；E:109–114 完整反向映射。T:160–190 从正式 Writer/Manager/library provider 读取四个不同 e identity，核对形态、literal kind、文本及实际类型。成功定型 JString 的官方工件门仍 OPEN。 |
| F9：MatchCase 根 pattern 无 selector ctxExpr | **ACCEPT 根 pattern 的非拥有回边门，撤销原缺失 finding。** | G:103–104 保存 contextExpression；G:357–376 在 e 全部填好后，以实际 Match/MatchCase identity 回填每个根 pattern 的同一 selector，并拒绝多 owning matches 或已有 ctx 冲突。G:487–488 的 owning expressionChildren 不包含 ctx；G:498–505 单独校验借用边。T:195–248 核对两个同型 selector 不合并、各 case ctx 的 assertSame、guard/body/type/runtime flags/位置/空 values；T:252–261 验证跨 selector 重用 case 被拒绝。 |
| F4：SOURCE composite 事务内公开 getter 提前发布 staging | **撤销原 composite → toClassLikeSymbol → public provider/tag 的具体拒绝；全域失败发布门继续 OPEN。** | V:58–72 从 wire.type 的实际 SemaTy.CompositeTyInfo.declPtr 解析 FullId；当前包取 CTX.declarations/canonical declaration，外包取 CTX.materializeExternalDeclaration。此路径已不调用 ToSymbolUtils、公开 ClassId getter 或 bindSymbolToLookupTag。原“constant classifier 查询导致公开 cache/tag 缓存本次 staging”的调用链不再存在。CTX:102–150 仍有本 context 完成提交与失败清理；它不自动证明所有 provider/cache/tag 的失败观察。 |
| F5：composite classifier 按 ClassId 重选首个 parent | **撤销原 classifier 重选 first 的具体拒绝，接受 wirePtr owner 静态路径。** | V:64–77 的 classifier 直接来自 FullId 选中的 canonical declaration；字段名匹配只发生在这个真实 classifier 的 declarations 中，并取已有 member.symbol，不再用于选择 classifier 或新造字段。ID:152–163 → CTX:236–240 先选实际 dependency snapshot；CTX:243–245 → AP:226–236 按选中 sourcePath 唯一定位 context。TD 产生 ClassId lookup tag 不再被 V 转成第二次公开 classifier 查询。本批不关闭其他 nominal/Alias 使用视图的身份门。 |
| F6：Position 直接调用 manager first | **撤销原 Position manager-first 拒绝，接受 context callback 静态路径。** | L:72–76 → CTX:184–187 → POS:25：传入的是 loadPackageSnapshot 委托到 importedSnapshot 的 callback，不是 manager 本身。P:179–205 将请求 context.moduleData 的 dependencies/refinementDependencies 可达集合用于候选选择；不能唯一选定时明确拒绝，未接 callback 的 context 也只允许唯一候选。POS:18–26 保留当前 pkgId=0/file 一基，导入 pkgId 一基/file 零基，未复用 FullId 保留值。POS:25 虽保留相同方法名，其正式入参及调用对象已经改变，不能仅据该行重报 first。 |

F7 的 header-only 路径不遍历 body e：D:386–408 只读 header 保存槽及其声明/type binder；SOURCE 请求才在 D:132–150 登记 default/helper 拥有内容并 reserve 正文。CTX:75–85、112、121–127 将完成 headers 与 d/t/source 同次提交；失败不替换此前 completedGraph。T:71–93 验证 body0、两组参数、原零 helper、真实 NamedFunction、retType=0 与签名 Int64 的独立合同；T:97–128 验证 header-first/body-first 的公共图；T:133–155 先查询非零但坏 body 的 header，再验证正文失败与此前签名/header稳定。

坏 body 输入由 T:413–423 先经正式 writer 生成合法 Block/Return/literal，再用生成 accessor 的 FuncBody.mutateBody 注入单一越界槽；不能把 writer 提前拒绝当成材料化失败。DD:889–890 的 SOURCE header binder 不读 field initializer，DD:1733–1773 仍不构造 normal function body。此处接受独立 SOURCE 能力，不授予 DFA/CFG 或 R5 normal-body 完成资格。

## 本批实际联合行为

[事前输入](../../build/cjmp-s5-type-20261119/public-payload-third-inputs.json)与[队列终态、逐 case 和归档原 XML](../../build/cjmp-s5-type-20261119/public-payload-third-results.json)记录同一 d9fa41cd 窗口：`2026-11-19T17:05:30.333Z` → `17:08:22.336Z`，DONE / exitCode=0。输入前后均为 **9905** 项、aggregate SHA256 **`F73979335E8B29CC2835992FAD69127B14ECFC46621F9D1FBFC068AE2204CFBC`**；unchanged=true。

本轮在内存解压七份 gzip 原 XML，逐份校验 bytes 与 SHA256，全部一致；总计 **11204 字节**，七个 timestamp 均在该实际窗口内；原 XML 的 classname/name 与 47 项 ledger 完全一致。没有启动新的执行或拼接早先两轮的通过数。

| 实际测试产品 | 原 XML 份数 | cases | failures/errors/skipped |
| --- | --- | --- | --- |
| 原 SOURCE expression：PSI 7 + LightTree 7 | 2 | 14 | 0/0/0 |
| 原公开引用提升/helper：PSI 2 + LightTree 2 | 2 | 4 | 0/0/0 |
| CjoSourcePublicPayloadTest | 1 | 6 | 0/0/0 |
| CjoTypeGraphDeserializationTest | 1 | 13 | 0/0/0 |
| CjoSourcePositionResolverTest | 1 | 10 | 0/0/0 |
| 合计（原 SOURCE18 + 公开 payload6 + 类型13 + 位置10） | 7 | **47/47** | **0/0/0** |

这 47 项是本批真实行为事实。TT:340–433 的双 parent Generic 用例实际涵盖 Q2、传递 refinement、BOTH/NONE 歧义和 canonical TP 身份；它验证共享 dependency selector 的一条真实使用路径，不等于专门验证了 Position 或 composite 常量。

## 输入纠正与 producer 的边界

四字符串输入 T:305–330 从官方 core 的真实 String declaration/self SemaTy/exportId 建立名义类型，JString 使用真正 Invalid，未造 JString 声明、替成 String/Int64、删除第四形态或 skip。T:176–180 的断言是固定 native 无 JString 引用的**错误恢复**合同，不是四形态全部成功定型的官方 CJO 正控。真实成功 JString 工件仍未取得，此资格门继续 OPEN。

固定 compiler 的 ExprWriter.SaveConstPattern:720–737 不写 SOURCE values。T:233、335–339 的空 values 与 W:792–801 的明确拒绝非空 values 一致；这纠正的是新增测试的错误输入，原 SOURCE 正确 fixture/期望无需回退。不能用这些空值断言给 V 的 composite/共享 allValues 内容验收授信。

PRO:744–777 对有真实合法 source 分隔符的字面量区分普通、多行与 raw；PRO:772 的 source=null → NORMAL 和 PRO:720–721 的合成 literal 标记只表达 normal 合成值。它们没有证明缺失源文本时恢复了原字符串语法，也没有证明所有源语法恢复。完整 canonical SOURCE 本来就不同于原源文件全部 token/语法节点的重建。

## 保留的验收资格

1. **F6 专用多 parent Position 行为仍 OPEN。** PT:267–274 的 context 未装 dependencySnapshotResolver，现有 10 项验证独立基数、缺失位置和逐锚点读取。后续应在正式 provider 所创建 context 中给同包 Q1/Q2 不同 allFiles，实际依赖只选 Q2，核对 expression/pattern begin/end 的 imported file=0 来源；交换候选顺序，并验证 BOTH/NONE 歧义失败及既有完成产品稳定。静态 callback 已修，不能以缺该用例继续报告它走 manager first。
2. **F4/F5 专用 composite owner 与后置失败观察仍 OPEN。** 后续从确有合法值载荷的正式消费入口核对实际 wirePtr 选中 classifier 的 moduleData/provenance/字段 symbol、重复请求及候选交换，再在后续必需槽失败时检查 context/provider/tag 和重试身份。固定 SOURCE ConstPattern 不保存 values，不能靠给它塞 values 制造成功正控。本批 T 的 values 均为空，47 绿色未验证该新 V 路径的上述行为；没有因此恢复已删除的公开 getter finding。
3. **完整 owned closure、Scope F registry、原 F1/F2、全域 provider/cache/tag 失败发布、统一值产品与 R5 normal-body/跨模式/LL 等全 S5/S6 门保持原 OPEN。** 本次接受 public payload 原子能力及指定来源路径，不将 G.finish 的已 reserve 节点完整、Library.body=null、header 可读或 47 绿色扩大为这些整项完成。以上是未覆盖的验收与原任务边界，不是本批新发现的 production 阻断。

## 当前文件与 SHA256

以下为本轮报告落盘前读数（首批取于 `2026-11-19T17:59:16.3768568Z`）；仅审查与本次门相关的路径，不表示对各文件全域另作验收。HEAD、联合窗口输入 aggregate 和当前单文件指纹各有独立用途。

| 简称 | 文件 | 行数 | SHA256 |
| --- | --- | --- | --- |
| G | [CfirSourceGraph.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt) | 552 | `7B45B9E8C0DB18EFB78FECF9D22F16665BDBABBFE358FE6A8D4EFB959DC4D68E` |
| API | [CfirImportedSourceGraphProvider.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirImportedSourceGraphProvider.kt) | 15 | `1A077ABB0DFB7001C4DF13B6F6AB99F381A145F262A87674DD8DFCB915B7098B` |
| D | [CfirCanonicalSourceDecoder.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirCanonicalSourceDecoder.kt) | 409 | `FD7D9115A7331291DEB944402FE86776A239FC46A98F2B48542FE43FF7AD9433` |
| L | [CfirSourceGraphLoader.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirSourceGraphLoader.kt) | 84 | `85724E980EE1C66525E7098CEEA77FF46C74D8BEB1CDEC0C2E9D540A35E250C2` |
| V | [CfirSourceConstantDecoder.kt](../../build/cjmp-owned-closure-20261120/documentation-link-repair-map.md#missing-source-constant-decoder)（原链接路径：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirSourceConstantDecoder.kt`；历史源码原字节缺失） | 93 | `01910B03C1B5D41A609633647B803C4FAC7BE7B9F7E0CF12C82417881FFD7BF8` |
| CTX | [CfirDeserializationContext.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeserializationContext.kt) | 247 | `4BDD0F2415D8DDD9411397969F13F1C8A59236B83955616CA33752A5374ABEC4` |
| ID | [CjoFullIdResolver.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CjoFullIdResolver.kt) | 383 | `FC7E22462A4F76A0CCCFE4700A210B3FA9F4C41817728381002AC927FB1E160E` |
| AP | [AbstractCfirDeserializedSymbolProvider.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/AbstractCfirDeserializedSymbolProvider.kt) | 502 | `4C168545FD8665D16B2C24DB66011277B731F56F93A8D0FE16BB39AD90EE3951` |
| P | [CfirDeserializedSymbolProvider.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/CfirDeserializedSymbolProvider.kt) | 241 | `CD82AD167EC737F1B6F94E7B11F7890AC8DE1D24D20DCCC1277FFA02D7DC0302` |
| POS | [CjoSourcePositionResolver.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoSourcePositionResolver.kt) | 32 | `99E8F09D632AF531D7464D0E30DEF28273D095E14560E2DB38122F4331F132BC` |
| E | [CjoCanonicalSourceEncoder.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt) | 217 | `54117511684CECE9D46811E28D2AA9F071D90EE3976AE20568919B2D78E40168` |
| PRO | [CfirSourceGraphProducer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt) | 862 | `C8E9C04C08C1CE053FB6434F8BA6012275CA81506B51909955F42A8B1F9F160D` |
| T | [CjoSourcePublicPayloadTest.kt](../../cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoSourcePublicPayloadTest.kt) | 498 | `B19F65CC0BBF433610E4DD527591F751C16917A5EC8A4A0BAAE45308E582D55C` |
| TT | [CjoTypeGraphDeserializationTest.kt](../../cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoTypeGraphDeserializationTest.kt) | 757 | `C33E7531D8D02B29603029125EFC468C240C643E0C2D5A2A77CCEB29C54983D4` |
| PT | [CjoSourcePositionResolverTest.kt](../../cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoSourcePositionResolverTest.kt) | 360 | `A216CB0F09197B389693C27D9066BD6F8E523E82D93394389BB1C0ACE2849B4F` |
| DD | [CfirDeclDeserializer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt) | 2740 | `E1548D4A4CD4EB1E5F994663EEF7A64C4237F09C55862892664A395D395ECF25` |
| TD | [CfirTypeDeserializer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt) | 283 | `5F84759B818F86438103DD7BB1FB0AC69F83D9533382A9567C9C752E685DB189` |
| W | [CjoPackageWriter.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoPackageWriter.kt) | 1993 | `2E1794C0CA89457E480019135E0435A1725E94892397A81EE27541DBB5C0E7A3` |

报告落盘后再次核对上述 18 个 production/tests 文件，SHA256 全部与表内一致；本轮唯一写入为本报告。

