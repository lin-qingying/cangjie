# S4 T6：泛型约束、最终上界与 strict capture 框架映射（2026-11-19）

独立只读 sidecar。仅新增本报告；未改生产、测试、fixture、期望或构建文件，未运行 Gradle/cjc，未联系旧 agent。D1 晚 constructor 生命周期不在本轮范围。

读取根 W 为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，有未提交修改；本库行号属于实际读取的工作文件。

已有 [ordered-type-payload-native-authority](ordered-type-payload-native-authority.md) 与 [ordered-type-payload-kotlin-authority](ordered-type-payload-kotlin-authority.md) 已读取。关键链重新使用对应仓库 **git show 固定对象**：native `14276740ffec1262b81b6b5236b1d840af270b37`，Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`。下文区分现存 API 与建议增加的完成合同，不声称新产品已经实现。

## 1. 直接结论与三个必须分离的产品

**T6 当前缺口是框架数据流，而不是 writer 的一次类型转换。** raw 原约束分组中的 RHS refs 与 TP.bounds 分别构造；正常 TYPES 只解析 TP.bounds。原组因此没有完整 resolved subject/RHS，capture 又没将它们纳入统一拥有图。writer 当前将每个 TP 的 explicitUpperBounds 合并成一组，无法保存原组顺序、source 和 implicit 引入标记。

同时，`GenericTyInfo.upperBounds` 应来自 normal resolve 完成 assumption/合法性/暴露/sanity 后的真实语义集合。排除默认 Any 的 explicitUpperBounds 仍不是这个集合。

| 产品 | 正确 producer | 后续用途 |
| --- | --- | --- |
| 原/派生 Generic constraints | raw 保存原组；normal TYPES 完成 subject、RHS 与生成约束，保留组、顺序、位置、isImplicitlyIntroduced | checker 诊断；CJO `Generic.constraints`。 |
| 每个 TP 的最终 native semantic upperBounds | normal TYPES 的共享泛型语义 owner，相关声明及 extend assumptions 完成后发布 | 泛型语义消费者；CJO `GenericTyInfo.upperBounds`。 |
| 完成的类型/Alias角色产品 | normal SUPER_TYPES/TYPES、普通使用槽解析和 CJO reader 在自己的构建阶段发布 | strict capture 只冻结；providers 做 native 节点路径选择；writer 编码。 |

不得把 defaultBound 当 finalBounds、把 TP.bounds 改名后当原 where 组、或在 provider/capture 中推进阶段计算缺失产品。不得通过全局 Alias 开关决定某个源码节点是否保留 Alias。

## 2. 固定 native 的确切合同

### 2.1 Generic.constraints 与 GenericTyInfo.upperBounds 是不同载荷

固定 `src/Modules/ASTSerialization/ASTWriter.cpp`：

- `SaveGeneric` L1145–1156 按 **generic.genericConstraints 的原/派生节点序列**写出；每组 uppers 按该组的原顺序逐项 `ObtainsAliasType(upper)`，并写 begin/end、constraint.type 的类型、isImplicitlyIntroduced。
- `SaveGenericsTy` L1015–1030 读取 **gty.upperBounds 的最终集合**，按 CmpTyByName 排序后写 GenericTyInfo。
- 并不是对一个上界列表套两种序列化器，也不是从 TP ordinal 反建约束组。

最终上界的固定流程：

| native 文件 / 方法 | 位置与行为 |
| --- | --- |
| `src/Sema/Assumption.cpp::PerformAssumptionForOneGenericConstraint` | L99–123 将 subject/upper 真正实例化后加入关系，保留 GC blame；L70–81 对上界声明的约束按替换递归收集。 |
| `src/Sema/PreCheck.cpp::CollectAndCheckAssumption` | L1196–1209 先对全部候选收集；处理 Alias 参数身份；添加 extend assumptions；最后逐声明检查。不能每个 TP 刚解析完就宣布终态。 |
| `CheckAndReduceUpperBounds` | L934–956 对 class bounds 的 meet/reduction 可能缩减集合，非法时清空并发布非法状态。 |
| `CheckAssumption` | L1139–1158 依次约束检查、上界合法性、ExposeGenericUpperBounds、AssumptionSanityCheck。FROM_COMMON_PART 有实际跳过规则，不能对 imported 结果重跑源码推导。 |
| `ExposeGenericUpperBounds` | L1106–1135 保留直接上界；沿 Generic 链遍历，只追加可达非 Generic 上界；传播 blame，删除自身。不会追加仅传递可达的 Generic，也不展开 nominal 全继承树。 |
| `IgnoreAssumptionForTypeAliasDecls` | L1397–1408 标记真实 Alias TP 的 isAliasParam；本轮源码不支持把这个名字解释成“清空所有 Alias bounds”。 |
| `AddAssumptionForType / CreateGenericConstraints` | L1460–1478 / L165–190 保存原约束数量，再根据实际 assumptions 生成追加组，新组明确 isImplicitlyIntroduced=true；其生成 uppers 使用 native排序。 |

`include/cangjie/AST/Types.h` L989–994 明确：最终集合包含所有直接上界与沿 Generic 链可达的非 Generic 上界。举例说明该集合定义：直接 `T→U, U→V, V→I` 时，T 的直接 U 保留，可达非 Generic I 加入；仅传递 V 不因为这条链自动加入 T 的最终集合。实际案例是否合法须先通过 native 的约束/合法性检查。

排序固定依据为 `src/AST/Types.cpp::CompTyByNames` L1038–1050：先 Ty::String() 的字节词典序；同字符串则按声明位置/身份或类型地址处理。`ordered-type-payload-native-authority` 已列完整位置规则。不能用 ClassId、debug renderer、TP ordinal 或集合遍历顺序替代。末级 native 指针规则是进程内身份，不能声称 JVM 任意编号精确等于 native 地址顺序。

### 2.2 Alias 完成与源码使用选择分开

固定 `src/Sema/PreCheck.cpp::SetTypeTy` L850–863 先保存实际应用 Alias 的 aliasTy及有序实参，随后将 node.ty 替换成实际语义类型。

`src/Sema/TypeManager.cpp::ObtainsAliasType` L2411–2446：错误/缺失节点得到 Invalid；COMPILER_ADD 节点直接保存已完成 node.ty；显式 Type 节点有 aliasTy 时，仅当 Alias 声明 IsExportedDecl 或 IMPLICIT_USED 才选 aliasTy，否则选实际 node.ty；其余按真实节点角色递归。完整展开并不授予 Alias 保留资格，linkage=EXTERNAL 也不替代该资格。

Alias 的 self、RHS Type 节点、应用槽分别有不同保存入口；self 绑定 Alias 自己及其 own TP，RHS 可保存符合资格的 Alias链，应用槽保存完整 abbreviation和对应完成语义类型。

## 3. 固定 Kotlin 的框架对应及允许差异

| Kotlin 固定文件 / 方法 | 精确职责 | 本库对应 owner |
| --- | --- | --- |
| `compiler/fir/raw-fir/psi2fir/src/org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder.kt::extractTypeParameter` L1015–1034、`addTypeConstraints` L1068起 | 声明 own TP、包含原 boundTypeRef、补基础默认上界。 | PSI/LT raw TP和约束转换；类型解析不在 raw 做。 |
| 同文件 `convertDanglingTypeParameters` L1044–1060 | Kotlin 将 dangling constraints 挂在 Synthetic.DanglingTypeParameter，以便继续处理 RHS。 | 仓颉应保留真实完整约束组，正常解析 RHS；不制造额外 fake TP 来模拟 native constraints。 |
| `compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirTypeResolveTransformer.kt::resolveTypeParameterBounds` L353–386 | 正常 TYPES 解析一次，按同一 owner 的 TP依赖拓扑处理，再从 delegated prototype重新解析；L388起处理非法cycle。 | `CfirTypeResolveTransformer` 的共享 TYPES helper，eager与designated入口共用。 |
| `compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt::typeParameterProto` L938–968 | 消费已完成 bounds；省略 Kotlin 的隐式 nullable Any默认 bound。 | 证明解析在上游，不证明仓颉 final native bounds等于FIR bounds，也不授予把默认 Any写作final的规则。 |
| 同类 `typeAliasProto` L792–803、`typeProto` L984–1022 | underlying、expanded、abbreviation独立角色；TP binder与type table独立。 | 完成类型/Alias产品及后续格式编码分离。 |
| `compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/resolve/TypeExpansionUtils.kt` L44–75 | 普通展开可写 cachedExpandedType；默认Supplier推进Alias SUPER_TYPES。 | 普通 resolve允许自己的计算/cache生命周期；strict capture不得调用这个可计算入口。 |

Kotlin没有 native GenericConstraint 原分组、isImplicitlyIntroduced 与 GenericsTy最终集合的完全同形产品。这里是固定 native证明的语义扩展；不得用“照搬FIR defaultBound/bounds”替代。Kotlin union bounds、expect/actual alias展开、nullable Any默认值不是仓颉 T6期望来源。

## 4. 当前 T6 的准确写入/解析/读取缺口

### 4.1 raw 当前生成两套 refs

`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirTypeConstraintDiagnosticData.kt` L40–83 的现存 record保存：parameterName、subject名字source、boundTypeRefs、整组constraintSource。它还没有真正解析后的 subject Type/绑定、组完成状态、isImplicitlyIntroduced 等合同。

| RAW 路径 | 位置与实际数据流 |
| --- | --- |
| `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt::collectTypeConstraintDiagnosticData` | L3764–3781 每组 convertTypeRef创建原分组RHS；declarationAttributes L3819–3820 存属性。 |
| 同文件 `collectTypeConstraintBounds` | L3747–3760 又将相同语法的RHS convertTypeRef一遍，按parameterName合并；L3848、3864、3880、3895 分别供 class/extend/Alias/function 的 TP.bounds。 |
| `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt` | collectTypeConstraintBounds L2836–2852 与 collectTypeConstraintDiagnosticData L2855–2875 同样重复转换；L2909–2910存属性。 |

两套 refs不是同一个拥有产品的两个视图。多条同名 subject组在 TP.bounds中会合并，原组的来源和顺序不能从该列表恢复。无效subject的RHS根本不进入任何TP.bounds，但仍应正常解析并诊断。

### 4.2 当前 normal TYPES 没有解析原组

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt`：

- processor调用 preResolveTypeParameterBounds；L175–205 保持文件和外围scope，递归各owner。
- `resolveTypeParameterBounds` L210–215 只 transformTypeParameters和unbindRecursiveTypeParameterBounds。
- `transformTypeParameter` L619–623 只解析 annotations/bounds。
- class/function/extend/Alias等入口各自调用该共享helper，原属性组不在它的解析写回链。

因此不能在export阶段简单把原group.boundTypeRefs当 resolved输入。应在这个既有 TYPES owner中接通完整组解析及TP投影，不在capture/checker补解析。

### 4.3 checker 当前补了原组的解析职责

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeConstraintsChecker.kt` L48–73 按名称判断subject；L81–110 手动检查未解析Raw UserType RHS，L132–142再查provider/当前包/core来猜可见classifier。它无法替代 normal TYPES 的完整import、Alias、外层/自身TP scope、错误引用与真实绑定。

`CfirTypeParameterBoundsChecker.kt` 读取declaredUpperBound类型再做合法性、class-bound暴露/冲突检查；这些报告可以继续由checker负责，但最终语义集合必须由共享resolve语义owner发布，不能等capture访问checker去求解。

### 4.4 capture和writer当前把缺少的产品用TP上界代替

- `CfirExportAnalysisInputSnapshot.kt` L535–536 冻结bounds和explicitUpperBounds，但不冻结完整constraint组；validateSourceDeclaration L621–630也未检查原组subject/RHS完成度。
- `sourceChildren` L352起使用正常来源边；generated `CfirDeclaration.acceptSourceChildren` 当前只遍历annotations，不拥有属性中的constraint refs。
- `CfirExportTypePayload.kt` L73–77 的Generic payload捕获parameter.bounds / explicitUpperBounds；不是native终态。
- `CfirCjoPackageMetadataProducer.genericMetadata` L1826–1838 按TP每个生成一组constraint，丢原组、source及implicit引入标记。
- `prepareGenericTypeBounds` L3229–3237仍用payload.explicitBounds写GenericTyInfo.upperBounds，既无最终Assumption终态，也无其native排序合同。

Source18/Cextra10/Tree25/providers154已有通过是用户提供事实；这些通过不构成本轮T6原分组和strict只读边界已正确的证明。

## 5. 完整可实施的 normal TYPES 产品路径

以下是一次连通的实施路径，新增API名称仅作建议，不冒充当前已有实现。

### 5.1 RAW：约束原组只构造一次

升级现有 CfirTypeConstraintDiagnosticData / CfirTypeConstraintReference为完整的约束拥有合同，至少保留：

- 原group ordinal与原始名字/source，整组source。
- 真实subject TypeRef及后续绑定；可解析但不属于当前owner的subject和错误subject也有正式结果。
- 有序RHS TypeRefs，重复槽保留，原语法prototype/source不丢。
- 显式/派生状态、isImplicitlyIntroduced；派生约束的来源/blame和正式生成位置。
- 明确未完成/已完成（含错误完成）的状态；空列表不能充当“尚未准备”的隐式信号。

PSI和LightTree应从一次group构造结果产生TP.bounds视图；移除重复convertTypeRef的collectTypeConstraintBounds来源，改为对同一拥有refs的索引/投影。原组重名可以影响语义集合，但不合并原组记录。当前 Kotlin式基础defaultBound可以作为普通类型系统输入保留，必须与native finalBounds分开。

subject当前只有Name/source，应在raw的实际语法转换中建立对应TypeRef或等价的真实绑定槽，再用正常type resolver解决；不能让checker或writer按Name重新找TP。原subject文本与已绑定symbol是不同角色，复制后名称变化也不应靠文本重匹配绑定。

### 5.2 TYPES：解析subject/RHS，保持group与TP投影同步

正确入口是 existing `CfirTypeResolveTransformer.resolveTypeParameterBounds`，其owner参数应能同时读取声明attributes和自己的TP（对应固定Kotlin helper的Declaration＋TypeParameterRefsOwner合同）。

1. 使用现有CfirTypeResolutionConfiguration，保留文件imports、外围容器、当前own TP。正常 `CfirSpecificTypeResolverTransformer` 解析所有subject和所有RHS，包括没有合法TP对应的group。
2. 将resolved wrapper及其delegated原型写回同一group产品，并用同一replacement identity表更新TP.bounds视图。不能只更新bounds，把原group留为Raw refs；也不能单独再解析group成为第三套语义来源。
3. TP依赖解析/必要重试共用同一owner算法；原型unresolve/re-resolve时同步group投影。按已绑定TP symbol/owner构建依赖，排除构造器借用的非own TP，不能按名字构建全局图。
4. 类型参数为空但仍有constraint组时，也解析原组；不建立fake TP来承接非法subject。
5. checker消费正常解析结果及source：subject可解析但不是own TP、RHS未声明或不合法的诊断保留其原source和实际类型，退役当前raw UserType名字探测逻辑。

eager preResolve、常规TYPES遍历、low-level/designated header入口必须调用同一生产算法。修复不能只接完整CLI文件遍历，把designated解析留在旧TP.bounds路径。

### 5.3 TYPES：发布最终native上界，不在providers计算

在同一个normal resolve owner的声明/依赖批次完成点接通固定native顺序：

```text
完整原group解析
→ 全部候选 assumptions（真实subject/upper替换和blame）
→ Alias参数身份处理 / extend assumptions及派生constraint追加
→ 约束/upper合法性和class bounds reduction
→ Generic链暴露非Generic可达上界 / 删除自身
→ sanity
→ 完整final semantic bounds及合法性/来源产品
```

existing `CfirTypeResolveProcessor.processFile` / `CfirTypeResolveTransformer` 是TYPES阶段owner；base `CfirResolveProcessor.afterPhase` 是已有完成通知门。批次终态发布应在正常阶段结束通知前完成。跨文件/指定解析须先完成所需依赖闭包，共用同一finalizer；capture不得替它补算。不要仅给每个TP bumpPhase(TYPES)就声称finalBounds已有值。

existing extend入口 `exposeExtendedTypeAssumptionBounds` L823起、`collectAssumptionBounds` L890起、`appendAssumptionBounds` L945起应接入这个共享关系/产品：当前class-like预过滤和无source TypeRef追加不足以代表native全过程。派生约束要成为真正追加group，明确implicit标记/blame，不是把null-source refs冒充原where。

最终产品建议由tree/语义公共层公开只读接口、resolve填充：每个真实TP绑定对应已完成的native bounds集合、合法性、来源以及native排序事实。空且合法、空且非法、未完成必须区分。Default Any与explicitUpperBounds属于不同输入视图，不能被作为finalBounds的替代值。

native排序需要消费完成语义的真实String/声明位置/身份规则。可以在normal发布准备中形成排序事实，在providers将完成集合冻结为有序向量；该步骤不允许provider重新解析类型或推进阶段。writer只能编码排序后的已完成输入，不用debug renderer发明比较规则。

## 6. 所有写入、拥有、复制、冻结和编码生命周期

| 生命周期 / 当前文件及方法 | 必须连通的合同 |
| --- | --- |
| RAW `PsiRawCfirBuilder` / `LightTreeRawCfirDeclarationBuilder` 的constraint转换 | 一份原group、subject/RHS、source/prototype；TP.bounds仅为该产品的语义输入视图。 |
| TYPES `CfirTypeResolveTransformer`共享helper、preResolve和header入口 | 解析所有group并同步refs；完整finalBounds与derived groups由normal resolve产生。 |
| checker `CfirTypeConstraintsChecker` / `CfirTypeParameterBoundsChecker` / recursion helpers | 消费绑定/合法性结果报告诊断；不再在原group Raw RHS上手动猜类型，也不作为capture的计算服务。 |
| tree `CfirTypeConstraintDiagnosticData.kt` / declaration source-child协议 | 原组及subject/RHS是declaration拥有的来源图；不止TP.bounds可被visitor访问。应在generator的source-child定义或同一声明属性拥有owner接入，不直接手改gen文件。 |
| `cfir/cfir-tree/src/.../copy/CfirOwnedGraphCopy.kt` L187–188、230–233 | 当前仅收集/复制RHS refs。升级后subject、组/TP视图共享refs、绑定symbol、derived group/blame都应由同一owned identity图分配/remap；不能生成第二套bounds。 |
| 同copy的source/name迁移 | 原group source与subject名字source是data scalar，当前entry.copy只替换bound refs，不能据此宣布它们已迁移。补齐正式source remap与捕获/恢复；语义subject symbol重定位与原token诊断名各自保留。 |
| `providers/.../macro/DefaultParameterFunctions.kt` L247起 | 当前显式遍历属性RHS。模板/正常lazy重建的类型替换必须扩展到subject、resolved group和finalBounds来源，并保持同一owned copy identity；不要仅在export侧替换。 |
| `CfirExportAnalysisInputSnapshot.capture / validateSourceDeclaration / sourceChildren` | 检查group完成状态，冻结完整group序列、source spans、TypeRef origin/prototype、绑定与finalBounds完成产品；为无效subject的RHS也建立拥有边。 |
| 同类 `validateUnchanged` L257–281 | 校验每组长度/顺序/重复身份、source、subject/RHS绑定、implicit标记、finalBounds和完成代次；现有type payload校验保留，不能只核验bounds。 |
| `CfirExportAnalysis / CfirDeclarationExportAnalysis` | 暴露冻结原/派生constraints与native finalUpperBounds；建议独立constraintsOf(owner)、finalUpperBoundsOf(TP)查询，名称为新合同建议。这里不产生assumptions或默认值。 |
| `CfirExportTypePayload.Generic` / `captureExportTypeDependencies` | 从真正完成finalBounds产品冻结Generic载荷；依赖闭包可按身份去重，有序format槽单独保存；不读取live bounds算final。 |
| `CfirCjoPackageMetadataProducer.genericMetadata` L1815–1839 | 从冻结groups逐组写type、uppers、begin/end、isImplicitlyIntroduced；upper引用按该组的TypeUse选择，不按TP重组。 |
| 同producer `prepareGenericTypeBounds` L3229–3237 / GenericTyInfo fill L3260起 | 只转换冻结的最终semantic有序bounds，保留Generic anchor回边reserve→fill；不从explicitBounds补计算。 |
| CJO reader `CfirDeclDeserializer` / `CfirTypeDeserializer` | 在reader构建阶段恢复分别编码的group与finalBounds；provider发布后不得通过capture再materialize。具体缺口见第8节。 |

这些读写必须一次接通。只使T6原refs可解析，但继续在writer按TP生成constraints，仍丢group；只把writer改读原group，又没有normal finalBounds产品，则GenericTyInfo仍错误。

## 7. strict capture：cache / Supplier / lazyResolve 的退役路径

### 7.1 当前事实与固定Kotlin事实不能混用

当前本库 `ConeTypeAliasType.kt` L14–22 的expandedType是不可变nullable字段，不是一个读取就执行的lazy Supplier。本库 `TypeExpansionUtils.fullyExpandedType` L69–76 调NoCache实现，没有固定Kotlin L49–55 的WeakPair cache写入。因此不能把旧cache描述当作当前本库事实，也不应为了parity再向capture引入Kotlin缓存。

但当前 captureType (`CfirExportAnalysisInputSnapshot.kt` L793–810)仍实际：

```text
provider/lookupTag查询declaration和abbreviated declaration
→ type.fullyExpandedType(session)
→ 再查询expanded declaration
→ 保存新计算的expanded边
```

本库 `TypeExpansionUtils` L48–50、69–75、138–159 的默认expandedConeType Supplier会推进Alias到SUPER_TYPES；同时查询symbolProvider、创建参数替换器、递归展开并附加abbreviation。这是计算入口，即使名称NoCache或多数数据已热，也不满足strict capture。

固定Kotlin正常serializer L799/L994也可调用full expansion；它证明普通serialization接口有计算能力，不能反证本库已经决定的“completed输入capture完全只读”合同。

### 7.2 normal expansion的producer继续使用原owner

现存生产owner是：

- `cfir/resolve/src/.../transformers/CfirSupertypesResolution.kt::SupertypeComputationSession`：L285–290提供阶段Supplier，storeSupertypes L322–330保存已完成Alias RHS；之后正常发布到expandedTypeRef。
- `cfir/resolve/src/.../CfirTypeResolver.kt` L660–675 从当前阶段Supplier取得RHS、建立完整Alias应用实参，并经普通展开保留abbreviation。
- `CfirTypeResolveTransformer.transformTypeAlias` L629–637 完成Alias own TP/RHS；所有正常使用槽通过type resolver完成参数替换。

这些owner应同时发布供读取的完成视图，不由capture请求它们重新工作：真实declaration binding、完整abbreviation/arguments、对应direct/full语义展开、来源/代次。正常算法仍可用私有cache和Supplier；capturer只接收其已经发布的值。

### 7.3 从capture移除哪些调用

1. 移除captureType中的fullyExpandedType及其默认Supplier读取，不改成传一个“无lazy”Supplier继续调用同一provider/替换算法。
2. Alias/declaration/TP binding改读normal producer发布的绑定关系；不在capture首次调用lookupTag.toSymbol或materialize CJO declaration来建立缓存。
3. 对真正non-alias完成类型，expanded视图可以明确为同一已完成值；对Alias self，不必调用展开函数，self与RHS是独立产品；对Alias应用/abbreviation必须有对应完成展开记录。
4. 遇到必要产品未准备时在capture入口拒绝，并保持所有phase/cache不变。未准备不是“没有Alias”，也不是返回原Alias、Any、null或临时error类型的兜底理由。
5. providers做节点角色选择时只读取冻结exportability/IMPLICIT_USED和origin，不运行expansion或assumption。
6. validator也只比较已发布字段及版本，不通过重新展开去证明旧计算值正确。

`captureExportTypePayload.Alias`读取当前expandedType纯字段可以保留，但必须确认该字段的角色是声明RHS模板还是实际应用完成展开；它不能代替独立完成记录。`captureExportTypeDependencies`应从这些完成产品收集边，不触发新的工作。

## 8. Source / CJO Alias 与 Generic 发布合同

### 8.1 Source Alias的完成角色

| Source槽 | normal发布内容 | capture读取 |
| --- | --- | --- |
| Alias declaration self | 实际Alias绑定、own TP有序实参、TypeAlias self身份；在normal Alias header owner构造。 | self只按身份编码Type；不把RHS当self，不为self现场fullyExpand。 |
| Alias RHS | 原resolved TypeRef＋prototype/origin/source、有序子槽，以及对应完成语义类型。 | 用冻结TypeUse决定RHS节点保存Alias链还是semantic。 |
| use / abbreviation | 实际Alias绑定、完整实际arguments、应用后的semantic direct/full expansion，与对应TypeRef节点来源。 | 每个节点按native保存规则选择，不能按同一Cone设全局“保留或展开”选择。 |

当前snapshot.declarationType L827–830仍在capture创建新的ConeTypeAliasType self，而完整展开随后由capture默认Supplier算。应把这份self及其RHS关联移到既有normal Alias header producer的完成合同；捕获器只创建自己的冻结wrapper/ID，不构造新语义类型。

当前 `CfirExportTypeUseGraph.selectSourceType` 已有逐节点exportability/implicit-used选择；应保持这一位置，在完整产品基础上工作。源TYPE的COMPILER_ADDED/SEMANTIC/SOURCE角色不因expanded指针存在而改变；不设置/切换AnalysisFlags.expandTypeAliasesInTypeResolution来修T6或capture。

### 8.2 CJO的两个Generic载荷不能再折成一份

当前 `CfirDeclDeserializer.deserializeTypeParameters` L1236–1245 / applyOwnerGenericConstraints L1255–1273 将Generic.constraints按subject symbol追加进TP.bounds，再补default Any；未恢复完整组/source/implicit节点产品。

`CfirTypeDeserializer.convertGenericType` L250–263：已物化当前包TP时直接返回其symbol，而GenericTyInfo.upperBounds没作为独立终态发布；另一分支把wire upperBounds做synthetic TP.bounds并补Any。两条路径不能保持相同的finalBounds/sourceConstraints角色。递归路径中的同名临时TP也不能用来满足strict完成绑定合同。

应在CJO reader自己的reserve→decode→bind→publish阶段：

- 按FullId和owner/ordinal预留并绑定真实TP；Generic引用回指同一声明，不按名字补symbol。
- 独立恢复owner Generic.constraints的顺序、subject、uppers、begin/end、implicit标记。
- 将GenericTyInfo.upperBounds恢复为已完成semantic终态，空终态保持空，不被基础Any补齐改变；必要的普通inference defaults另外表达。
- FROM_COMMON_PART/binary结果保存既有终态，不在providers重新进行Source假设暴露。

current writer原始CJO passthrough L1330–1343已能保留wire分组/位置/implicit标记；这不能证明正常typed CJO模型也保存了同样的产品。两种输入通道应在发布接口上表达同一合同。

### 8.3 CJO Alias self / RHS / abbreviation / expansion

固定native `ASTLoader.cpp::LoadTypeAliasDecl` L924–930先建立真实TypeAliasDecl、加载basic info、注册imported声明并恢复generic；不能在capture里按名字再造替代绑定。当前本库 `CfirDeclDeserializer.convertTypeAlias` L2355–2390以AliasInfo.aliasedTy建RHS、绑定Library Alias。`CfirTypeDeserializer.convertSemaTy` L141仍对TypeKind.Type返回unsupported error；因此RHS含另一个已保存Alias或显式Alias应用时，typed reader还不能表达完整Alias链。直接保存原raw CJO表不等于这个语义模型已完成。

应在reader构建阶段解码TypeKind.Type＋CompositeTyInfo真实Alias绑定和typeArgs，分别发布self与RHS的角色，再完成应用/abbreviation对应的语义展开；需要的声明/TP/依赖Alias在该reader事务中先reserve、正确bind。普通跨包lookup可由normal reader/resolver完成，但不得让capture首次触发library加载/阶段推进或生成另一份同名类型参数。

self可以是一份不现场展开的身份类型，声明RHS与完成展开在独立产品中已经准备；应用Alias的展开必须来自其实际arguments，不使用另一个上下文的旧use-site值。每份记录同时带真实Source/CJO domain，imported `(snapshot,TYPE,rawIndex)`仍是身份入口，不能用结构equals取代。

## 9. 主线程实施后必要的真实测试（本轮未新增/运行）

| 验证面 | 必须用真实管线观察的断言 |
| --- | --- |
| T6原组，PSI/LT两入口 | 多group、同subject重复group、多upper/重复upper；原group数量、ordinal、uppers顺序、subject/RHS/source完整；TYPE阶段同组refs都已resolved，TP视图共享正确身份。 |
| subject/RHS错误与scope | 不是own TP但可解析的subject、未知subject、imported Alias/重命名引用、外层与函数同名TP；RHS仍由normal resolver诊断，source定位不从合并后的TP bounds猜。 |
| 最终语义bounds | 无约束TP最终wire上界空且normal默认Any不丢其普通类型系统作用；Generic链暴露、class reduction/非法清空、合法递归与非法cycle；集合/blame/合法状态与官方cjc产物一致。 |
| extend派生约束 | 原groups保持不变，新groups按真实准备算法追加、implicit=true；上下文TP替换及blame对应实际原约束，不是source=null推断。 |
| S4 Source→CJO真实Generic字段 | 分别检查Generic.constraints和GenericTyInfo.upperBounds；只检查“能写CJO”或两者内容相同不充分。排序用native依据，避免要求跨进程指针/绝对编号相同。 |
| Alias Source角色 | self/RHS/use、public/protected/internal导出资格、IMPLICIT_USED、CompilerAdded、nested tuple/function/Pointer/VArray应用，完整actual arguments与已完成展开保留；不打开全局Alias选项改变期望。 |
| CJO正常reader→capture→writer | 官方CJO的Generic两个载荷、空bounds、Alias链及应用；typed模型绑定和raw passthrough各自完整，TP和Alias回边复用真实FullId身份。 |
| strict捕获副作用 | 先完成真实normal管线，再安装lazyResolver/Supplier/provider-materialization计数或拒绝守卫；capture/validate/write前后phase、cache、source/TP绑定、expanded产品身份不变，未完成产品拒绝时也不推进。 |
| owned copy / source迁移 /失效 | 真实完成图复制或重建后，group subject/RHS/TP绑定、source/constraintSource、derived origins、Alias展开和finalBounds独立正确；旧冻结输入不变，改动refs/order/source/bounds/abbreviation后旧validator拒绝。 |
| eager / designated /多文件 | 同一合法源码通过完整TYPES及low-level目标header解析获得相同完成合同；依赖在另一个文件/已载CJO时，capture仍不工作。 |

新语义用例应由主线程/语义测试作者按固定native源码和官方cjc验证确定语法及期望，保存独立authority产物；不要从本库现有错误writer输出生成golden。本文只给必要验证面，未声称上述拟新增用例已经编译通过。

## 10. 实施次序与 gate 边界

建议连续实施：tree约束拥有合同 → 两raw入口单份groups/TP投影 → normal TYPES完整解析及native finalBounds → Alias完成发布/正常CJO reader完成模型 → source/copy/失效生命周期 → providers严格冻结和节点路径选择 → 两个Generic载荷各自编码 → 真实frontend和strict副作用验证。

TYPES/SUPER_TYPES的正常计算服务留在其现有owner；provider/capture不通过stage推进、cache命中、defaultBound或Alias开关补产品。当前已通过的其他套件不需要为此迁移原fixture或放宽正确期望。

读取快照SHA-256：

| W 文件 | SHA-256 |
| --- | --- |
| CfirTypeConstraintDiagnosticData.kt | `DCE51DA82AF39D239D1DBD63F924607A747751D1B35A5E06633E9BF529C8261F` |
| PsiRawCfirBuilder.kt | `B5764168C0F70AB304D976CA711A48B23BBC8622069579E919D5687E44186CA0` |
| LightTreeRawCfirDeclarationBuilder.kt | `ACDD3F48F55D24F869297683E9E76CAC981815B27D22318EAD3A9F85B94B105D` |
| CfirTypeResolveTransformer.kt | `309C4F7A9B7B2FC7FB01C1AD4172C2DA06B59B5C943EEA12D1B87B4DBEFB7DC4` |
| TypeExpansionUtils.kt | `80665DDB53CA8587BF86C076F7EA8293240C3EC4A85F2AA2B189394D8EA3A485` |
| CfirExportAnalysisInputSnapshot.kt | `0FA2C3D144989EB42941A6E8925DE24F1B56A9EDBBB366B69D8162CD4308D52D` |
| CfirExportTypePayload.kt | `DDCE9F6021CA1F7F2EFF8DB89E421D81A4C4C6E050E39A822FF03EC468072BE4` |
| CfirCjoPackageMetadataProducer.kt | `839E0475DABFE5200D1C836C4EB3BDD6457A4E78DA83A4697A1B1A8E6BE8B24A` |
