# This 双产品、C-call 完成检查与构造器准备 gate（2026-11-19）

独立只读复审，仅核对用户本轮列出的生产改动及其直接消费者。唯一写入本报告；未改生产、测试、fixture、期望或构建文件，未运行 Gradle/cjc，未联系旧代理或停止的会话。

## 1. 结论与范围

**This 原失败的双产品发布、C01 完成后 Unit 检查、RAW/宏默认构造共享工厂、retNode 来源发布及两处 fat jar 接线均已看到实际源码实现。整体 gate 仍未关闭，主要剩余是 constructor eligibility 的阶段合同。**

| ID | 级别 | 本轮确定的 remaining |
| --- | --- | --- |
| K-01 | P1 | 宏阶段以尚未发布的 interopInfo 判断 mirror，缺失快照被当成非 mirror，可能错误保留/创建 implicit constructor；STATUS 后没有对应的最终资格处理。 |
| K-02 | P1 | CJMP matching 的 existing post-merge 默认构造路径仍绕过新共享工厂，用旧 ImplicitDefault、body=null、TYPES phase＋implicit retRef；且在 family/structure 快照取得后才改变物理 declarations。 |
| T-01 | P2 | ResolveUtils 的普通 applied-call 表达式类型分支仍读 typeFromSymbol，未消费已发布的 callSiteType。函数值/enum 分支已接线，普通函数分支未闭合。 |
| H-01 | P2 | 与新 retNode 发布直接相邻的既存 header 合同缺口：static constructor 的 implicit return 在 TYPES 仍无条件取 nominal owner，native 应为 Unit。 |

P1/P2 是源码 gate 判断，不表示 main 已运行的 compile/test 新出现这些失败。本轮没有运行验证，也不将旧运行结果冒充上述改动后的结果。

读取根 W：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，存在未提交修改。下文路径相对 W，行号/哈希属于读取的工作文件。

固定 native `14276740ffec1262b81b6b5236b1d840af270b37` 的 Utils.cpp、TypeChecker.cpp、TypeCheckCall.cpp、TypeCheckDecl.cpp，以及 Kotlin `3c7a5041f238102285764d370665f56f587f9dc4` 的 completion writer，均通过对应仓库 `git show 提交:路径` 复核。沿用已读 parity 技能的证据/owner 标准，不做整个前端的历史包布局验收。

## 2. This 的两个真实产品：原混用路径已修

### 2.1 producer 与引用合同

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt`：

- `substitutedReturnType` L855–863 仍在完成声明替换后调用 bindThisTypeToCallSite，表示 native GetCallTy 的表达式结果。
- `substitutedCallableSignatureReturnType` L866–876 独立形成 callee 签名返回：声明没有 This 时保持完成返回；class receiver形成带 This 标记的名义返回，generic receiver保持 generic；不是拿 Call.resolvedType 补回标记。
- 两个入口复用 `substitutedDeclaredReturnType` L879–891 的声明/候选替换输入，没有按 asThis 名字处理。

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAppliedCallReferenceFactory.kt` L25–56：

- 参数名已改为 callSiteType，独立存入引用。
- signature return 从 `finalSubstitutor(candidate.substitutedCallableSignatureReturnType())` 产生，和完成参数类型属于同一候选/final 替换合同。
- 普通非函数的 named value 没有 callable signature return 时保存 null；callSiteType 仍独立保存，不用它伪造签名。

`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/references/impl/CfirResolvedAppliedCallableReference.kt` 已分别公开 substitutedReturnType 与 callSiteType，并保留 parameters、target、isFunctionValue、matchedParentType。

对当前 Carrier.asThis 正例，源码数据流现在可表达：

```text
substitutedReturnType = ClassThis(Carrier,[Int64,Bool])
callSiteType           = Class(Carrier,[Int64,Bool])
```

此项源码接线通过。未运行当前原 fixture 的 L331/L332，不能宣称两 parser 已通过本次实现。

### 2.2 两字段的 lifecycle 和 SOURCE readside

| 当前 owner | 已核实的接线 |
| --- | --- |
| `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyVisitor.kt::copyAppliedReference` L146–158 | signature return、参数、parent 与 callSiteType 都通过同一 context.type / symbol remap 复制。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirTypeVariablesAfterPCLATransformer.kt::transformResolvedNamedReference` L83–96 | 两字段独立应用 PCLA final substitutor，保留引用 identity cache。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt` AppliedReference / capture 分支 | payload 增加 callSiteType，与 signature return 分开保存。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt` L299–302、698–708 | 增加 appliedCallSiteType 捕获、类型图收录与身份校验，未将两个快照槽合并。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt::appliedSignature` L434–436 | callee 仍只用 substitutedReturnType / substitutedParameterTypes 建 Func；Call record 仍使用表达式结果。 |

这关闭了前次报告指出的“Call nominal 结果被写成 MemberAccess callee return”的主路径。SOURCE/encoder 没有新增 This 标记补丁。

### T-01：普通 applied call 的表达式类型读取仍未闭合

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/ResolveUtils.kt::typeFromCallee(CfirReference)` 的方法合同 L122–125 明确是**提取表达式类型**。

当前 L158–167：

- isFunctionValue 分支读 callSiteType。
- enum constructor 分支读 callSiteType。
- 普通 applied function/constructor 的 else 分支仍执行 `typeFromSymbol(resolvedSymbol)`。

因此对已经完成的普通 This 或泛型调用，查询该公共表达式入口仍可能退回声明返回的 This/声明 TP，丢失 callSiteType 的 receiver/实例化结果。新字段不是只为 enum 和函数值而发布的。

实际读点仍存在：`CfirExpressionsResolveTransformer.kt::storeTypeFromCallee` L2541–2551 可从函数调用/限定访问重写类型，L6503–6507 在已 resolved reference 但表达式类型尚缺的路径也调用它。`CfirCallCompleter` 和 `CfirCallResolver` 还有该公共入口的读取；不能只修 SourceGraph 后忽略正常表达式读取。

正确修复 owner 是这个 resolved-applied 表达式投影分支：消费工厂已经发布的 callSiteType；callee signature 的消费者继续读 signature。不要以 declaration type 或 substitutedReturnType 为已完成 call 的替代来源。该项不冒充当前 Carrier原断言仍失败，只指出实际读取链的残留。

## 3. C01：完成后 Unit 义务已实际接入

`CfirCheckArguments.kt` L81–83 在合法映射、direct C declaration ABI 已确认后调用 `candidate.requireCompletedCFunctionArgumentCheck()`。Candidate L319–325 保存该义务，并检查不是 mapping-failure 产品；函数值入口未被自动扩成同一 native 规则。

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt` L483–496：

1. 仅 Mode.Normal 执行完成义务，位于 rewriteArgumentList 返回完成实参之后。
2. 对 actual arguments 应用最终替换后检查 Unit。
3. 使用正式 UnitCannotBeCFunctionArgument 降低 candidate applicability；已有同类诊断避免重复报告。
4. 写回错误引用，并从完成的全 arguments 构造 error list，fixed 的 formal 映射保留，C extra 只在真实错误产品中成为无 formal 错误项。
5. 后续正常 callDiagnostic / resultType 链继续传播 candidate 错误，没有在 SOURCE 编码时抛类型异常。

这补齐了前次 C01 指出的“fresh T 后来固定为 Unit，但没有任何完成后消费者”的源码缺口。CType 的全局 Unit 原子合同未被改变，C struct extra 也继续沿普通检查链。

源码 gate 通过，不等于已有负例位置、handler 名称或不同 completion mode 的测试均通过。此轮未读取新运行结果，也未运行 Gradle；不把旧 Unit位置问题宣布为已验证解决。

## 4. 默认构造共享工厂与 retNode：已修部分

### 4.1 完整的 RAW 默认构造节点

`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/builder/CfirImplicitConstructorBuilder.kt` L20–40 是共享树工厂：

- 显式 symbol/moduleData、receiver、constructed-class TP refs。
- RAW_CFIR phase、Synthetic.ImplicitConstructor（fromSource=true）。
- PUBLIC / FINAL 的默认状态。
- 真实空 Block，而非 body=null。
- exportFacts 明确 isCompilerAdded / isImplicitlyAdded。
- returnTypeRef 在 RAW 保持 implicit，由正常 TYPES 发布，不提前伪造已完成 header。

PSI `PsiRawCfirBuilder.kt` L1573–1579、LightTree declaration builder L640–647 和宏 finalizer 的 missing-constructor分支都已调用该工厂。宏删除最后一个显式 init 后，当前最终 Source nominal 无 constructor 时可以真实新增默认构造器；前次 O-01 的普通 class/struct 路径已接线。

创建资格与树节点工厂已分开，providers 无需反向依赖 parser；没有 fixture名、最后一个方法名或 source text 判定。

### 4.2 native constructor 身份和来源节点

Source attributes 输出 `CfirCjoPackageMetadataProducer.kt` L2896–2899 已排除 Synthetic.ImplicitConstructor 的 PRIMARY_CONSTRUCTOR 位；CONSTRUCTOR、COMPILER_ADD、IMPLICIT_ADD 继续保留。Library/CJO 原始 attributes 仍走 original CJO 的独立保存路径。前次 O-02 的 Source implicit-primary 身份差异已修。

`CfirTypeResolveTransformer.kt`：

- L439–448 在实际解决 Source constructor implicit return后发布 COMPILER_ADDED origin。
- L470–482 为 payload enum constructor的真实生成返回节点发布 COMPILER_ADDED；无参数 variant 不被冒充成函数 retNode。
- 非 Source/CJO return refs 不因此重建。

固定 native `TypeChecker.cpp::AddRetTypeNode` L228–238 与 `TypeCheckDecl.cpp::SetEnumEleTyHandleFuncDecl` L509–534 给出生成 retNode 的独立证据；这不是根据 body是否非空推测来源。普通 instance constructor / payload enum 的新来源接线通过；static header的返回类型仍有相关缺口，见 H-01。

## 5. Ctor COMMON / mirror 的准备时机

### 5.1 COMMON raw角色已存在，mirror完整快照尚不存在

当前宏 finalizer `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/ImplicitConstructorDeclarations.kt`：

- Source / cjoOrigin guard 已加，删除/保留/新建均按 Synthetic.ImplicitConstructor 的稳定 origin分类。
- excludedByNativeRole 读取 owner.status.isCommon/isSpecific 与 interopInfo.java/objc mirror/wrapper。

COMMON/specific 与 interop快照不能按同一时机判断：

| 资格事实 | 当前准备时机 |
| --- | --- |
| Source COMMON / SPECIFIC 语法角色 | PSI convertDeclarationStatus L4075–4076 已从 parser role写入 raw status；LightTree modifier list L189–190 也发布同一事实。宏输出重新解析后会带其最终语法角色。 |
| mirror / synthetic-wrapper / ABI语义 | `cfir/providers/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfoProvider.kt::publishInteropInfo` 从已解析 annotation identities、版本/feature门、serialized facts生成；STATUS首次调用，BODY_RESOLVE再补完整参数值。 |

因此普通 Source COMMON 的早期排除本轮有明确输入依据。SPECIFIC 的早期排除是**推迟到合并后的最终决策**，不能解释成 native 对所有 specific 最终都禁止 default init。

### K-01：宏阶段读取未发布 interopInfo，mirror资格变成缺失即否

实际阶段链已经核实：

```text
Macro construction/splice
→ finalizeImplicitConstructorDeclarations / primary field materializer
→ success/degraded 的 annotation metadata seal
→ recordExpandedRawFilesOnce(provider,...)
→ session.runResolution(recordable)
→ TYPES / STATUS / CJMP / BODY...
```

直接证据：

- `compiler/frontend/.../MacroExpandPhase.kt` L392–393 在 macro结果中调用两个 helpers，再计算cache key、返回construction结果。
- `cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/analyse.kt` L545–567 先 expand取得 RecordableRawCfirFiles，再注册provider、运行resolution。
- `CfirStatusResolveProcessor.kt::withResolvedStatusPhase` L333–335 对 RAW(<TYPES)直接退出；L348–353 才发布 resolved status与 publishInteropInfo。
- `CfirStatusResolver.kt` L330 也在status构造中调用发布者；status invalidation L126 明确清掉 interopInfo。
- interop publisher L84、108/113 根据正式annotation/serialized身份形成 Java/ObjC mirror/wrapper；宏finalizer没有执行这个完整身份准备步骤。

所以受支持的 Source mirror角色在当前宏准备时完全可能仍 `interopInfo=null`。finalizer 用 `?.isMirror == true` 把它当作非 mirror，保留early implicit或创建missing implicit。STATUS后来识别镜像身份，却没有回到默认constructor资格的正确发布owner。镜像没有显式constructor的错误输入也应保持 native“没有自动补 constructor”的树和诊断合同，不能靠生成默认constructor改变它。

固定 native顺序不同：`TypeChecker.cpp` L2091–2104 先 primary/CJMP合并，再 Java/ObjC PrepareTypeCheck，最后 AddDefaultFunction；`Utils.cpp::AddDefaultCtor` L375–393 在 mirror事实已准备时排除 ObjC mirror/wrapper及 Java mirror class。

**不能在宏阶段对 RAW声明直接请求完整 STATUS解决此问题。** 该阶段尚未完成provider注册、TYPES及声明来源发布；目前 STATUS门自己也拒绝 RAW输入。也不能在最终CJO writer滤掉镜像constructor，因为错误已经进入normal声明、缓存、scope和family产品。

### 5.2 完整的正确准备合同

默认constructor资格需要一个真实的 declaration-preparation门，其输入应明确包含：

1. 最终 Source/CJO归属、class/struct类型、实际实例constructor集合；static不抵实例constructor。
2. 已准备的 COMMON角色；specific则消费合并后的effective成员产品和native资格。
3. 已完成绑定及feature/版本门的 Java/ObjC mirror/wrapper身份；“没有镜像”与“尚未准备”必须区分。
4. 本代次可修改的声明/annotation事务，以及最终provider/family/cache快照发布边界。

主线程应让 existing interop身份owner在constructor最终准备之前发布这一真实资格输入，或把整项constructor最终准备纳入同等完整的后续declaration preparation事务。若只提前发布身份子集，仍须复用同一 annotation binding/version/feature owner；不能在 finalizer匹配 `JavaMirror`/`ObjCMirror` 短名，也不能把未准备态变成 false。

只移动一行 null-check、仅强制调用lazyStatus、或在已seal字段/family产品之后移除constructor，都没有接通这个合同。准备资格、决定节点、完成header、发布member graph的顺序必须一起落实。

## 6. K-02：CJMP后合并路径仍是旧工厂且发布顺序不一致

当前 `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt::addSpecificDefaultConstructorAfterMerge` L191–219：

```text
只排除COMMON及Java mirror
→ 检查family.membersFor(owner)有没有实例constructor
→ buildConstructor:
     phase=TYPES
     origin=ImplicitDefault
     status=owner.status
     returnTypeRef=implicit
     body=null
```

这不是本轮新引入的分支，但它仍是实际生产caller。早期宏finalizer明确排除了 specific，合法specific若需最终补构造，就会进入这项后续决策；不能只凭RAW/宏工厂统一就宣布所有default constructor统一。

具体剩余：

- 没有调用 CfirImplicitConstructorBuilder，新 empty Block / Synthetic.ImplicitConstructor / exportFacts合同缺失。
- 把 class/struct status对象直接作为constructor status，缺少默认PUBLIC/FINAL独立合同。
- 自称TYPES却仍持有implicit retRef，绕过本轮在正常TYPES发布COMPILER_ADDED retNode的路径。
- 资格仅检查Java mirror，没有完整ObjC mirror/wrapper规则。
- 原点ImplicitDefault不是新Source-derived constructor身份，后续来源/export会看到另一种生成语义。

还有一个明确的快照顺序问题：matching L78–79 先取structure再调用helper；L89–94 先取family再调用helper。helper随后只修改物理 owner.declarations。`CfirCjmpFamilyAggregate.kt` 中 declarationOrder及merge.members为冻结产品，membersFor读取这些产品；物理append不会自动更新这份已取得的成员/owned-symbol图。

正确owner仍是合并后的declaration准备和family发布门：先根据native完整资格确定是否允许生成；允许时复用共享树工厂、正式TYPES/STATUS header通道和来源事实，再把节点纳入最终成员、owned symbols与declarationOrder，最后封闭family/structure产品。不要在已完成family上append一个它不会读取的节点。

COMMON缺少必须显式constructor、specific找不到合法common或mirror资格错误时，应保留真实诊断；“merged列表为空”本身不授予用默认constructor兜底的资格。

## 7. H-01：static constructor 的 header retNode 应为 Unit

新COMPILER_ADDED标记解决了原Type节点存在性，但当前 `CfirTypeResolveTransformer.transformConstructor` L439–447仍对所有implicit constructor return取nominal owner，没有status.isStatic分支。

这条输入真实存在：PSI `convertConstructor` L1442明确对static不借用class TP，L1490仍建立implicit return。BODY constructor L589–590只是转换已有return ref，没有读取到独立的static-return修正分支。

固定 native `TypeChecker.cpp::CheckCtorFuncBody` L457–476区分：STATIC返回Unit；实例constructor返回parent class/struct类型，随后给真实retNode赋值。

因此应在constructor header的真实类型owner先选择 static→Unit / instance→constructed owner，再发布COMPILER_ADDED retNode。该项是与新增来源发布直接相邻的既存类型差异，不应以“已经标为CompilerAdded”代替正确返回类型，也不应交给Source writer改type。

## 8. 发布闭包与后续边界

`prepare/frontend/build.gradle.kts` L24 和 `prepare/ide-plugin-dependencies/cangjie-frontend-cfir-for-ide/build.gradle.kts` L20 已将 `:compiler:compiler.version` 加到手工合并列表。前次V-01指出的两个binary发布闭包漏项在源码上已接通；本轮未构建jar或验证资源实际在包内。

本次优先remaining是 K-01 的interop readiness和 K-02 的合并后生成/发布一致性；T-01补齐普通applied表达式读取，H-01补齐static header类型。This和C01不需要退回旧单产品/单次检查实现。

后续主线程验证应分别观察：macro前后镜像角色变化、COMMON排除、specific合并后的真实constructor集合、空Block/retNode来源、普通/函数值This双类型、PCLA Unit及完整error list、发布资源。此为后续检查边界，本轮不运行任何测试或Gradle。

读取快照 SHA-256：

| W 文件 | SHA-256 |
| --- | --- |
| Candidate.kt | `E8BC16EB8D1EA28EDF80D52B3ACCF9EFBED303464C83235E7DA7109BD3812E9D` |
| CfirAppliedCallReferenceFactory.kt | `BC50FBEBFF7FA33FBC11E52FE7CDF82BAF3336E6DC71487D41DA0B7518B6F654` |
| CfirCallCompletionResultsWriterTransformer.kt | `7023B980C9C2885ECF2BD6B27E0F3809CFBAF0596CF5111D92DE293944E83BD2` |
| ImplicitConstructorDeclarations.kt | `46EA541FDCE8A7A41F28C124BA2DA1060C6CB018204CC6E3BE31F9CD88689569` |
| CfirImplicitConstructorBuilder.kt | `EDFDAB80DD7F3A5D69F35A29EB6CAF1655C99BECB3A4CE635539B8AC964F47CC` |
| CfirCjmpMatchingProcessor.kt | `087291AF3C53EABF2606B701A44C53C8AA7F45B51F7467337B8A88FAB2ED7A68` |
| CfirTypeResolveTransformer.kt | `54F7C90F11AD45029602624E3803C1B3F01BECE50F79F5C72829C12184031C6E` |
| ResolveUtils.kt | `D861216CED8544E507C52F2537DA977FC6BCD2DD91DDB193B5A89A2AC230D2A2` |
