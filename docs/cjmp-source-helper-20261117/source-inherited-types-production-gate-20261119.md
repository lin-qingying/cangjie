# SOURCE inherited-types 产品生产 gate（2026-11-19）

**KotlinParityGatekeeper：REJECT。普通 class 的生成 Object ref、继承列表次序／容器冻结、capture 接线及 TypeUse→encoder 已建立；H1 JavaMirror 规则、H2 Alias/class-parent 分类、H3 生成时 binding 发布合同仍未闭合。不给“全部 SOURCE superclass 产品完成”、行为或整个 S4 通过。**

限定对象是新 `CfirSourceGraph.inheritedTypeReferences`、Builder、Context.indexDeclaration、capture、TypeUse.superTypes 和其写出路径。唯一实现树 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`，包含未提交磁盘。本轮仅 apply_patch 新增本文件，不改生产／测试／Fixture／旧报告，不运行 Gradle／cjc，不联系旧 agent，不轮询当前 compile。

延续已读的 `D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md`。上游直接从主树 external Git 对象读取固定 native `14276740ffec1262b81b6b5236b1d840af270b37`；external 只读。既定 Kotlin `3c7a5041f238102285764d370665f56f587f9dc4` 不替代本项仓颉 Object／JObject 生成语义。D1/K01/T6/S5/S6 按原余额保持 OPEN。

## 固定 native 合同

[PreCheck.AddSuperClassObjectForClassDecl L1161–1177](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp#L1161)：

1. HasSuperClass 为 true 则不再生成。
2. JAVA_MIRROR／JAVA_MIRROR_SUBTYPE 先 AddJObjectSuperClassJavaInterop；只有该调用返回 false 才 AddObjectSuperClass。
3. 其余 class 执行 AddObjectSuperClass。

[TypeCheckClassLike.HasSuperClass L141–180](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckClassLike.cpp#L141) 按实际类型／target 分类：Interface 不是 class parent；Class 是；Alias 递归实际 RHS；Struct/Enum 等其他目标诊断，不能当 class parent。

[AddObjectSuperClass L183–197](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckClassLike.cpp#L183) 创建独立 RefType、curFile、Object 名称和 COMPILER_ADD；Object 本身（core package＋Object identifier）不补。其余从真实 core decl 取 target 和 target.ty，前置到 inheritedTypes；缺 core Object 是独立语义诊断。

[AddJObjectSuperClassJavaInterop L200–221](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckClassLike.cpp#L200) 对 interop Java lang 的 JObject 本身返回 false；其他镜像 class 从真实 imported JObject class 创建 COMPILER_ADD Ref、前置、设置 JAVA_MIRROR_SUBTYPE。**缺 imported JObject 时诊断并返回 true，不回退补 Object。**

继承保存不是普通 Ty 列表：[ASTWriter.SaveClassDecl L1400–1405](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1400) 调 [ASTWriterImpl.GetVirtualInterfaces L209–216](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriterImpl.h#L209)，对每个 inherited Type 分别 ObtainsAliasType。其 [TypeManager L2413–2428](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2413) 先有效性／COMPILER_ADD，再 Alias isExported／IMPLICIT_USED 资格；EXTERNAL 不构成该资格。

## 本轮已成立的产品链

| 层 | 坐标与有限结论 |
| --- | --- |
| 字段／拥有者 | [SourceGraph L224–225](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt) 是完成继承 ref 产品，按 class-like declaration identity 建 map；没有修改 raw superTypeRefs。 |
| 生成正常 Object ref | [Producer.indexDeclaration L129–138](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt) 为当前无 parent 的普通 class 从 Object symbol 构造独立 resolved ref、标 COMPILER_ADDED、前置原 refs。source 为 null 不妨碍实际生成节点存在；没有在 encoder 造这个 ref。Object classId 跳过生成。 |
| 列表防御复制 | [Builder.inheritedTypes L310–312](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt) 检查未 finish／owner 未重复登记，并复制 refs；[frozenList／frozenIdentityMap L477–479](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt) 使用不可修改 list／identity map；L387 发布该 map。有序重复 refs 槽保留，不按 Cone.equals 去重。 |
| canonical 收录 | [Capture L442–445](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) 收录 header refs，L700–706 保存 source/payload/resolved Cone 并加入 type 队列；L797–814 冻结类型角色和依赖绑定。生成 ref 不需要挂回原 raw visitor 树才可捕获。 |
| 已捕获节点换代 | [Snapshot L293–305](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) 验 source、Cone、children、origin payload、span；L278–280 验已捕获 declaration binding。生成 ref 的 origin/Cone/来源边在 capture 后被改动，不能仅因 semantic type 相同而接受。 |
| SOURCE／library／非 canonical 选择边界 | [TypeUse.run L83–85](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 仅 source class-like＋有 canonicalSource 时取完成继承 refs，否则取原 declaration.superTypeReferences；没有把 library/noncanonical 原 ref 列表直接替换成 canonical map。这里只接受这条分支，不声称所有 interop/alias 来源已正确。 |
| 当前 origin 与 Alias | [TypeUse L117–120、L134–138](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) COMPILER_ADDED Object ref直接 Semantic；显式 SOURCE refs 按 isExported／IMPLICIT_USED 选择 Alias，没取 EXTERNAL。原显式来源与新生成父类不会因有 delegation／null source互相覆盖。 |
| encoder | [ClassInfo L2073–2074](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) 及 Interface／Struct／Enum L2084／2088／2094 都消费 TypeUse.superTypes；Source Header 被选择后才进共同 type(shape)/t 池，不在 serializer 进行父类资格判断。 |

容器冻结不等于 ref 的全部内容／生成 binding 已在 SourceGraph.finish 时冻结。当前 ref 对象仍是本代身份，fields/type binding在 capture 冻结；H3 就是这两次边界之间的生成目标合同。

## H1 [P1] REJECT：JavaMirror／subtype 无 class parent 仍直接生成 Object

具体坐标：[Producer L129–138](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt)。

该条件只判断 CfirClass／Object/Any classId／hasClassParent，没有读取当前 Java mirror/subtype 事实，也没有 JObject 分支。因此 source mirror class 无显式 class parent 时进入普通 Object 构造，与固定 PreCheck L1171–1176 的先 JObject 规则不同。

本库已有正式元数据：[CfirJavaInteropInfo L58–60](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfo.kt) 的 isMirror/isMirrorSubtype；[InteropInfoProvider L74–85](../../cfir/providers/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfoProvider.kt) 发布 source/serialized Java 请求。当前生成 owner 没有消费这组规则。没有在本轮搜索到另一个给普通 SOURCE refs补 JObject 的 superclass 分支，不能用“应该在其他 resolver 已补”豁免。

**下一修复合同：** 从已完成的 superclass/interop owner 生产正确的 Object/JObject 选择和真实 target/type，SOURCE 冻结该结果。JObject 自身的 false→Object 分支、缺 JObject 的诊断→true分支、生成后的 JAVA_MIRROR_SUBTYPE 事实都要明确保存／接线，不能改成“找不到 JObject就用 Object”或仅更换 refs而漏属性。若该 interop 准备未完成，不能发布成已完成的普通 Object header。普通 class 的 Object 路径保留。

本项未运行 JavaMirror fixture；判断来自当前缺失的分支和固定 native 的明确调用／返回规则。

## H2 [P1] REJECT：class-parent 分类没有 Alias 合同，并把 Struct/Enum 当 class

具体坐标：[Producer L122–127](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt)。

当前只按 Cone runtime 分支判断：非 interface ConeClassLike 为 parent，ConeStructType/ConeEnumType 也返回 true，其余 false。该分类有两处差异：

- native CheckAndAddSubDecls 对 Alias target 进入其真实 RHS，class Alias 也构成已有 parent；当前 ConeTypeAliasType 落入 false。不能假定所有来源都已展开：[SupertypeComputationSession.expandTypealiasInPlace L378–387](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSupertypesResolution.kt) 明确在 analysis flag 不允许展开时保留原 ref。 [SourceProducer.typeOf/requireType L734–740](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt) 也不会展开 Alias，且允许已完成 Alias 类型。于是该支持的输入可多补 Object，而真实继承 Alias class 已存在。
- native 对 Struct/Enum 等非 class/interface 目标不设置 hasSuperClass；当前将这些类型当 parent 会抑制 Object 生成。即便非法继承另有诊断，也不能用这种类型形状声称 native class-parent事实成立。本轮没有因此要求修改正确 fixture或放宽 SOURCE 有效性条件。

Alias 的**是否是 class parent**与保存时**是否保留 Alias Ty**是两个合同：private/nonexported Alias 仍需按真实 RHS确定 parent，保存资格则由 TypeUse/native ObtainsAliasType 裁决。不能把资格失败解释为“没有 class parent”。

**下一修复合同：** 在实际 superclass 语义 owner 发布完整 class-parent／实际 target分类，SOURCE只消费已完成事实；或消费已准备的展开绑定产品完成分类，并保留原 Alias ref供逐槽保存。只让实际 Class target构成 class parent，Interface/Struct/Enum按native分别处理。不能在 SourceGraphProducer 新开无条件 fullyExpanded/provider/lazy-resolve 旁路，也不能禁用原支持的 analysis flag 来绕过 Alias 情形。

普通“只有显式接口”的 class仍应 Object前置；已有直接／Alias class parent不得重复生成。Object自补例外要依据真实 Object角色，不能将新 parent分类与raw源码是否非空合并。

## H3 [P2] REJECT：生成时选中的父声明 binding 没有成为冻结产品，capture 可重取另一 binding

具体坐标：[Producer L132–136](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt)、[SourceGraph L225／L310–312](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt)、[Capture.captureType L799–808](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt)。

生成时取得 objectSymbol，但只把 constructType() 的 Cone/ref存入图；没有存 native RefType.ref.target 对应的 declaration identity或该生成使用的绑定域。具体依赖链已核实：

1. [ClassLikeSymbol.lookupTag L146–152](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/CfirBasedSymbol.kt) 只由 ClassId 创建 tag；[constructType L76–90](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/TypeConstructionUtils.kt) 并不将此 symbol强绑定到 SOURCE产品。
2. [LookupTagImpl L16–22](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/ConeClassLikeLookupTagImpl.kt) 只有按 session 的弱 cache；[ToSymbol L60–65](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/ToSymbolUtils.kt) 在没有适用 cache 时再按 ClassId查 provider。
3. captureType据这次结果冻结 declaration；生成阶段的 objectSymbol没有 identity对照。 [validateUnchanged L278–280](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) 检查的是**已经选中**的 declaration仍绑定，不能证明它等于生成时的 native target。

因此，“生成查询返回 A、source图尚未保存 A的binding、capture前 provider同 ClassId变为 B、capture冻结 B并随后验证B仍绑定”是当前调用链允许的合同反例；本轮没有运行这种 provider换代用例，也没有声称当前普通 SDK窗口已触发。固定 native AddObjectSuperClass L192–194 明确保留 actual target与target.ty，这两条字段不能由一次后续名称查找替代。

**下一修复合同：** 完成继承产品应保留 ref identity、完成类型/来源、生成时实际 target declaration及捕获domain，显式 Alias的classifier事实也应与H2同代。Capture消费这些已发布 binding并校验 owner/domain/identity，不对这个生成父节点重新选 provider目标。可沿既有 typed-capture产品复用，不需要修改全局 Cone lookup tag equality/cache为特殊 Object行为，也不能只把弱 cache填一次当作冻结。

Builder.finish目前仅冻结 inherited map/list，没有对 inherited refs的完成类型／绑定／source owner专门封闭校验；Capture L443只要求 owner已收录。新 typed header的发布完整性应在这个明确产品边界表达，不能用容器只读或捕获后检查来宣称生成到捕获全过程稳定。共享 provider/fullyExpanded capture仍有 K01余额，本项只要求新继承产品的实际生成 binding不丢失，不能据它关闭整个 K01。

## 待验证与本轮边界

未运行新增测试、compiler或旧窗口重验。本轮新 refs 字段／防御复制／capture源边已读通；以下仍需后续真实冻结窗口：

- 普通无 class parent／只有接口时 Object前置；已有 class或Alias class parent时不补；Object本身不补。
- JavaMirror/subtype、JObject自身、JObject缺失诊断及同步 subtype事实。
- 有序重复继承槽、canonical源码与原 library/noncanonical域、正确 Alias保存资格和生成 current origin停止。
- 生成 ref的 origin/Cone/source/target/owner/domain换代、旧图保持自身值、生成到capture的actual binding一致。
- typed header Builder发布与真实 ClassInfo/InterfaceInfo/StructInfo/EnumInfo writer出口，而不是只断言Carrier一个super槽或仅比较最终type数量。

H1/H2/H3是下一生产修复合同，本角色只产出此 gate文档；生产patch由主线程继续。D1真实late owner收敛、K01/T6/S5/S6及完整S4仍不关闭，不将当前新compile启动或此前Alias48 map单差视为本轮通过。

## 关键未提交版本

以下13个文件在 `2026-11-19T06:17:35.7161266+08:00` 与 `2026-11-19T06:19:56.0762141+08:00` 两次 SHA-256 读取一致；结论针对这些字节，不以HEAD或旧测试aggregate替代。

| RepoRelative 文件 | SHA-256 |
| --- | --- |
| [cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt) | `153279D248592E4280E8CCA473830B63216B605F2184C37B023D616725D7FFFD` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt) | `B07875129A15C40ADC9F59CB9490EEEFEE0A505F033B13789ADB20771FE6836D` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) | `DAF4FCCF1FB44BA762F7CA8C8974621FC2D81064208819FF89C02A2000DE94B7` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) | `E6E835C4939A6781BD8C9AD37C5C53D6FE40344DC87F4DF411D025563430C12E` |
| [cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | `3B64C0A9C912151FCC9334A8139841BBD120BE5401FB599D71FE4FAF64AAADEB` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSupertypesResolution.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSupertypesResolution.kt) | `5C66D225DC320AD9A4ABBD203079929D349F7A033DE16895511D969DD7E99E5E` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/CfirBasedSymbol.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/CfirBasedSymbol.kt) | `0D58CE89C5A597FC927941F0A1F55DD5D08FEC1ABAFB638FC91173AED976B943` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/TypeConstructionUtils.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/TypeConstructionUtils.kt) | `A34CA8DFFDE4D07A390EFA327058B33FC9D71BF2BDEFB972EC14D4A48A9A0386` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/ConeClassLikeLookupTagImpl.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/ConeClassLikeLookupTagImpl.kt) | `1E3EB7E9D06936EE05996D16CDF6B7EDD0164ECFC80FECCB604A0621801D5EBC` |
| [cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/ToSymbolUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/ToSymbolUtils.kt) | `1343643A20753F153F7F62A3790E5EE02A37599FFBCA87975156FA9CC9C2E8E2` |
| [cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/LookupTagUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/LookupTagUtils.kt) | `6EC48FEF6FCFA2FD8B623497107BA3A9FFD476FB22ECF364D42D8A8B4C3320AA` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfo.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfo.kt) | `159C4F4F0C7E325F3D9C0F383880DD410B87B704720705926C6F93B62D7B08DF` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfoProvider.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfoProvider.kt) | `805DA65D9430DBD5671DD7191E163C90EC17FC62C4BEA585DD52CA26CA47BEA0` |

