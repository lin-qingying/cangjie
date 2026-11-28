# Type／返回节点／constructor 分派定向 gate（2026-11-19）

**KotlinParityGatekeeper：REJECT。正常 raw Type variants→Specific/TYPES 分派、两 Raw 的共用空 Block ctor、Ctor/enum 返回节点创建和 enum 冻结返回出口已修复；但实际 ctor 创建链仍有 D1 旧工厂、D2 static init 返回类型、D3 新 origin 消费合同余额。不能签发全部 constructor 入口或完整 S4 通过。**

接续 [TypeUseGraph 复审](type-use-graph-recheck-20261119.md)，仅核对本次类型与 constructor 入口。唯一树 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`；对象为最新未提交磁盘。延续已读 `D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md`。唯一写入是 apply_patch 新增本文件；未改生产、测试或旧报告，未跑 Gradle／cjc，未轮询新 compile／analysis-api diagnostic generator，未联系停止旧会话。

固定 Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`、官方 `14276740ffec1262b81b6b5236b1d840af270b37` 均从主树 external 以 git show 读取，external 只读。以下源码判断不依赖旧运行成绩。

## 39579531 记录的范围

已读 [联合首轮结果](type-use-c-ellipsis-joint-first-results-20261119.json)：111 项、83 passed、28 failed、exit 1；原输入前后 aggregate SHA `88A9FF4A8FFCD12802E4C69DC9BCCAF8AB0866A882A144F32287897639448236` 相同。按记录中的 classname 分组：

| 记录组 | 总项／失败 | 本轮如何使用 |
| --- | --- | --- |
| 原 SourceExpressionExport 两 Raw＋StoredMember 两 Raw 的两个用例 | 18／10 | 保留原 Source 余额，不以本次静态 ctor 修订宣称这十项已通过。 |
| FrozenTypes 两 Raw | 4／4 | 不与原 Source18 混为一组，不因 Alias self/RHS 已越过而关闭后续完整断言。 |
| TypeUseGraphLifecycle 两 Raw | 16／8 | 每个 parser 8 项、4 failed；记录失败包含 `Not CfirResolvedTypeRef (CfirParenthesizedTypeRefImpl) in storeResult`，发生在 `parenthesized(value: ...)` 参数外层。与本轮修复的 concrete type dispatch 对应。 |

上述是读取已保存结果，不是本轮执行结果；本轮没有独立重核每份 XML 窗口。新 compile／generator 启动及生产暂冻结不构成行为 ACCEPT，也不能替代 constructor return／类型分派定向运行。

## raw Type 分派：原未 resolved 外层拒绝点源码层关闭

| 入口／owner | 当前坐标与行为 |
| --- | --- |
| TYPES 的中央 type ref 入口 | [CfirTypeResolveTransformer L644–645](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 交给同一个 typeResolverTransformer；L651 User、L673 Basic、L679／681／683／685／687 Paren／Function／Tuple／VArray／Option 全部调用中央 transformTypeRef。 |
| Specific 的实际 concrete dispatch | [CfirSpecificTypeResolverTransformer L289–305](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt) 覆盖 Option、User、Basic、Paren、Function、Tuple、VArray；不再仅从默认 transformer 递归 children 后返回 raw 外层。 |
| 递归与当前结果发布 | [Specific L99–113](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt) 先变换真实 children，再调用 session.typeResolver；L136–141 成功构建当前 resolved ref，复制 origin，delegation 指向原节点；L156–165 错误输出同样保留 origin／原型／partial。 |
| 特殊已完成／省略节点 | Specific L267／274／282 保留 resolved、专用 error 和 implicit 合同；TYPES L658／665 分别保持 resolved 与 implicit，不为了消除 raw 外层错误把真正省略类型强行解析。 |

参数／返回／泛型实参中的 Paren 现在有调用链：

`具体 ref.transform → TYPES/Specific.transformParenthesizedTypeRef → transformTypeRef → inner transform → 当前 resolved/error ref(delegatedTypeRef = 原 Paren)`

来源产品保留 Paren 外层对象及其有序 inner 边；仅以新 resolved ref 替换使用槽，未把括号 Type 删除为任意首个 child。当前仓颉 Function/Tuple/VArray 等额外类型角色也走这个中央 owner，不只给失败参数加分支。

固定 [Kotlin Specific L72–77](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSpecificTypeResolverTransformer.kt#L72) 先 children 后发布 resolved，Function 还有 [L81–103](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSpecificTypeResolverTransformer.kt#L81) 的专门实际输出；[Kotlin TYPES L432–445](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirTypeResolveTransformer.kt#L432) 区分 implicit 与 resolver dispatcher。本地角色数和语言字段不同，本轮只确认中央解析／当前结果发布／原型保留合同，不宣称整个 Kotlin 类型 resolver 原样复制或全部诊断位置已对齐。

## 新 ctor／enum 主路径修复确认

| 产品／阶段 | 读取结论与坐标 |
| --- | --- |
| tree 共同工厂 | [CfirImplicitConstructorBuilder L20–40](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/builder/CfirImplicitConstructorBuilder.kt) 创建 RAW_CFIR primary ctor、独立 implicit 返回 ref、真实空 Block；origin 为 Synthetic.ImplicitConstructor，事实明确 isCompilerAdded／isImplicitlyAdded，status public/final。来源坐标只是参数，资格由调用 owner 判定。 |
| origin 产品 | [DeclarationOrigin L54–58](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationOrigin.kt) 新 origin 明确 generated=true、fromSource=true，因此它可进入源码图，同时保持编译器生成角色；不再靠 fake kind 推导这两项。 |
| 两 Raw 接线 | [PSI L1575–1579](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt)、[LT L642–646](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) 调同一工厂，传实际 symbol／module／isLocal／dispatch receiver／constructed TP refs；不在两个 raw owner 再保留 body=null 实现。 |
| 宏后存在性决策 | [ImplicitConstructorDeclarations L42–64](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/ImplicitConstructorDeclarations.kt) 用新 origin 区分声明 ctor 和 implicit ctor，排除 common/specific/mirror，删除被真实实例 ctor 取代的节点；保留已有 implicit 时移到末尾，宏删掉最后一个 explicit ctor 后用共同工厂新建。没有让 static init 替代实例 ctor。此结论限该函数处理的文件根 class/struct。 |
| Ctor TYPES 返回节点 | [TypeResolveTransformer L439–448](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 从原 implicit ref 创建已解析 owner 类型；源码图且非 CJO 的当前返回 ref 显式标 COMPILER_ADDED，原型由公共 helper 保留。节点创建不依赖 body 已取得 CFG。普通实例 ctor 主路径已消除“完成后仍 SEMANTIC／FuncBody ret=0”的原来源缺口；static 语义见 D2。 |
| enum payload TYPES 返回节点 | [L470–482](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 有参数且 source、非 CJO 的 variant 标 COMPILER_ADDED；无参数 variant 维持 VarDecl 的不同角色，不补造 FuncBody。 |
| enum 独立冻结出口 | [TypeUseGraph L37／45、L79–86](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 增加 identity-keyed enumConstructorReturns；payload 选择实际返回 ref，无参数保存 null，整体 map 冻结。 [Producer L2036–2041](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) source payload 构造 FuncBody 时消费该用途，旧 direct typeOf/Cone 出口已移除；原 CJO wire 分支仍按 imported FuncBody 读取。 |

固定官方 [CreateDefaultCtor L311–338](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L311) 创建真实空 Block、参数列表并发布 COMPILER_ADD／IMPLICIT_ADD；[SetEnumEleTyHandleFuncDecl L527–534](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckDecl.cpp#L527) 单独创建 payload constructor 的 retType RefType。该证据支持本轮两个创建 owner 的修订，不将 enum 强行塞进 CfirFunction body visitor。

## 正常 BODY 与原型合同没有放宽

[DeclarationsResolveTransformer L585–604](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 对 ctor 正常进入 DFA、参数／委托／body scope，退出后存 CFG并调用完成发布；共享工厂的空 Block 在 RAW 时无完成 type／CFG，仍需这条真实 BODY 链解析，创建空节点本身不等于完成。

[完成发布 L423–435、L491–495](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 仍限制正式 BODY phase、默认 inference session、外层拥有域退出、实际完成 body/type 和该函数 CFG；未看到为了新 implicit ctor 放行 null body 或预置 BODY phase。Ctor/enum 返回节点的 TYPES 创建与 BODY 就绪是独立产品。

Ctor 原 implicit SEMANTIC ref 经公共原型工具被保留，新当前节点发布 COMPILER_ADDED；Graph [L104–105、L115 之后的非 SOURCE 分支](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 以当前 origin 判定 presence／停止，不因原型是 SEMANTIC 就删除当前生成 Type。普通 named function 的原省略判据仍以已发布原型 origin 消费；本轮没有把“原省略”和“当前实际生成返回节点”合并成 source-null 判断。

## D1 [P1] REJECT：实际 TYPES／matching 创建链仍保留旧 body-null 工厂

具体坐标：

- [TYPES.ensureImplicitDefaultConstructorIfNeeded L977–1000](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 仍 buildConstructor，origin=ImplicitDefault、return=implicit、body=null、status=klass.status，没有调用新共同工厂。它有实际调用点 [transformClass L234](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 和 [resolveClassTypes L748](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt)，不是不可达旧定义。
- [matching.addSpecificDefaultConstructorAfterMerge L192–218](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt) 也创建 ImplicitDefault＋body=null；同时标 resolvePhase=TYPES，但返回 ref 仍 implicit，没有这次新增的实际返回创建／解析。它没有因共同工厂存在而自动获得空 Block、来源事实、TYPES/STATUS/BODY 合同。
- [旧 origin L118](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationOrigin.kt) 的 fromSource 不是新 origin 的 true；Ctor TYPES L447 的当前生成分类和 BODY L474 的来源发布均受影响，body-null 也无法满足 BODY 完成条件。

TYPES 旧 owner 仅排 specific 和已存在实例 ctor，没有执行新 macro owner 的 common／mirror 等 native 资格。对 common class 进入 TYPES 时没有实例 ctor的输入，它仍可补出该旧声明；固定官方 [AddDefaultCtor L381–393](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L381) 明确不在对应 common class/struct 情况补造。这里给出源码条件与分支差异，没有声称运行了某个新 common fixture。

**下一修复合同：** 收敛所有真实 implicit ctor 创建 owner 到同一产品合同，退休旧 body-null 构造实现；资格在对应语义 owner 按 native 判断。晚建 matching 节点需实际完成其声明头及后续阶段，不以 resolvePhase=TYPES 替代解析。不能仅因新 Raw／macro 路径已用共同工厂就保留这两个入口，也不能让新 origin 的消费者兼容旧 source-kind 分类来掩盖工厂差异。本轮不签发整个 CJMP matching，但这两个实际 constructor 创建入口属于本次复审余额。

## D2 [P1] REJECT：static init 的 TYPES 返回类型仍是 owner class-like，官方要求 Unit

具体坐标：[TypeResolveTransformer.transformConstructor L441–447](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 与 [buildConstructedTypeForConstructorOwner L1008–1014](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt)。

该选择没有 constructor.status.isStatic 分支；helper 只把所属 class-like 与全部 owner TP 实例化为构造后类型。两 Raw 的 static init 确实进入这个输入：[PSI L1441–1443、L1488–1490](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) 和 [LT L1045–1047、L1084–1086](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) 已区分 static 的借用 TP 列表，但返回 ref 仍 buildImplicitTypeRef。

固定官方 [CheckCtorFuncBody L457–476](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L457) 对 STATIC 返回 Primitive Unit；仅实例 ctor 返回 parentClassLike／parentStruct 的类型，并填入 FuncBody.ty、FuncDecl.ty 和 retType.ty。当前标 COMPILER_ADDED 只是正确发布“生成节点存在”，没有把错误的静态返回 Cone 改正确。BODY 的 discard-value 解析／CFG 就绪不负责把此返回类型改为 Unit；共同工厂的正常实例默认 ctor也不能证明 static 分支正确。

**下一修复合同：** 在 ctor 类型创建 owner 区分 static init 与实例 ctor：static 生成 Unit 返回节点，实例使用真实 owner 构造类型，均保留对应原型／origin。签名与 FuncBody ret 冻结相同正确语义，static 不引入 owner TP 实参。不得用 serializer 特判 Unit 修补错误前端输入。本轮未运行 static init 反例；结论来自两 Raw 的输入和固定 native 返回类型分支。

## D3 [P2] REJECT：新 origin 已发布，部分 constructor 资格消费者仍只认旧 origin／fake kind

具体坐标：

| 消费者 | 当前判据与合同差异 |
| --- | --- |
| [isCjmpMatchingCandidate L24–26](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpDeclarationSemantics.kt) | 仅排 ImplicitDefault 或 source.kind=ImplicitConstructor，没有消费 Synthetic.ImplicitConstructor。当前 Raw 的 fake source 恰好使排除成立，不能证明明确 origin 的合同被消费。 |
| [cjmpHasCommonDefault L113–118](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CjmpAttributes.kt) | primary 分支同样按旧 origin／fake kind 排除；新工厂已有真实 body，更需要用其实际 implicit 角色判断是否为可声明默认实现的 primary ctor。 |
| [PrimaryConstructorParameterFields.isDeclaredPrimary L151–152](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/PrimaryConstructorParameterFields.kt) | 新 origin.fromSource=true、isPrimary=true 时，只剩 fake source kind 使它不被当成源码 PrimaryCtorDecl。工厂 source 参数允许 null；对这样的合法构造输入，该布尔式会成为 true，违背新产品“implicit ctor”角色。 |
| [Producer PRIMARY_CONSTRUCTOR 位 L2896–2899](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | 这里已按新 origin 排除 implicit primary，说明产品中已有可用稳定判据；上面三个消费者与这个格式身份判据尚不一致。 |

本项不把 diagnostic 的定位／fake kind 处理一概视为错误；拒绝的是**是否为真实声明 primary／matching/default 候选**仍依赖源码位置属性。factory(source=null) 的 isDeclaredPrimary 结果是可从当前 API 与布尔式确定的合同反例，不声称本轮某个 fixture 已触发；matching/default 后续是否进入分支还取决于其各自 status／feature，本轮不伪造运行结论。

**下一修复合同：** 用已发布 constructor 角色建立一致的资格判据，并让 matching、primary 字段、default 与格式属性共同消费；source kind 继续用于定位。结合 D1 移除旧创建路径，不用 old/new origin＋source-kind 的多路兼容来保留原分类漏洞。补明确新 origin、nullable／迁移 source、显式 primary 和真实 native 排除角色的验证。

## 尚未验收的余额

上一轮 T2-R1 的正常 enum payload 返回创建及独立出口、T2-R2 的两 Raw／macro 共同 implicit ctor 空 Block主路径可在源码层关闭；D1 说明整个 constructor 创建链尚未收敛，D2／D3 是本次实际入口复审找到的具体合同缺口。不能据此回写旧 39579531 为通过。

后续定向验证需覆盖全部 raw Type variants 的参数／返回／实参外层 resolved 输出和 Paren/prototype 身份；实例／implicit／static／enum ctor 的返回类型、origin、真实 body/CFG、signature／FuncBody 槽；宏删掉最后一个显式 ctor 与 native 排除条件；晚建 TYPES／matching owner；新 origin 的 shared copy、source 迁移和资格消费者。保留正确 fixture 与真实 XML，不因前一轮失败而删除断言或放宽 BODY gate。

T6 的完整 Generic/catch 仍未实施，保持 OPEN。本轮不验收完整 S4、decoder、S5/S6 或全部方案；没有生成／编译／测试通过声明。

## 关键未提交版本

以下 14 个关键文件在 `2026-11-19T04:57:19.5800694+08:00` 与 `2026-11-19T04:59:04.9321668+08:00` 两次 SHA-256 读取一致，标识本轮实际核验的磁盘候选，不以 HEAD 或旧批次 aggregate 替代。

| RepoRelative 文件 | SHA-256 |
| --- | --- |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/builder/CfirImplicitConstructorBuilder.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/builder/CfirImplicitConstructorBuilder.kt) | `EDFDAB80DD7F3A5D69F35A29EB6CAF1655C99BECB3A4CE635539B8AC964F47CC` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationOrigin.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationOrigin.kt) | `5EC06F14E5FA3EF0660BC06EF980731E92531A318C63D3531BF92910C37CBE83` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/ImplicitConstructorDeclarations.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/ImplicitConstructorDeclarations.kt) | `46EA541FDCE8A7A41F28C124BA2DA1060C6CB018204CC6E3BE31F9CD88689569` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) | `54F7C90F11AD45029602624E3803C1B3F01BECE50F79F5C72829C12184031C6E` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt) | `8ED694429CD585008D317ED9C49C834EF068B051AA6EF2F2638385EEFDADB153` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt) | `087291AF3C53EABF2606B701A44C53C8AA7F45B51F7467337B8A88FAB2ED7A68` |
| [cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) | `B5764168C0F70AB304D976CA711A48B23BBC8622069579E919D5687E44186CA0` |
| [cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) | `ACDD3F48F55D24F869297683E9E76CAC981815B27D22318EAD3A9F85B94B105D` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) | `984910DDF473538E592945B0E674E12CAEAFB46E9E3FF648287A75ABDA277599` |
| [cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | `A26BB328FEBACEED1AE404F140D4D3D222E6FEC7D6393B5BAE8B6F3FCAB9483C` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) | `596E74AC8EEDE716C7D7473C77710E7D571DFB5218CCDAF05DBBA62DC9294342` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpDeclarationSemantics.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpDeclarationSemantics.kt) | `5F40A7944C4A46F3CC322AAEDF170CDD426981751806CFD114736063EA739F55` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CjmpAttributes.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CjmpAttributes.kt) | `09CF22045843B3448231214BC0687889CAAF82B19A2D47205012E4E525127384` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/PrimaryConstructorParameterFields.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/PrimaryConstructorParameterFields.kt) | `A0DF2995BF21D5C39ED59FE11B4E3C65F4090239548AED7D31359BB034FC4139` |

