# S5 类型身份恢复：固定 native 权威合同（2026-11-19）

角色：CangjieSemanticsAuthority。先读 [continuation](../cjmp-source-type-use-continuation-20261119.md) 与 [source-chain-delivery](../cjmp-source-chain-delivery-20261116.md)，随后按主线程新增优先级先交付 TypeKind.Type、Generic 的递归和跨包身份合同。本文不是生产实现验收；不关闭 S5/S6。

唯一 native 对象为 `D:/code/intellij/cangjie/external/cangjie_compiler` 的 `14276740ffec1262b81b6b5236b1d840af270b37`。已执行 `git show -s --format=fuller <SHA>`：主题 `!1689 merge 14216_revert into release/1.1`，提交时间 `2026-05-14 19:05:03 +0800`。所有下列 native 源码均以 `git show <SHA>:<path>` 读取，行号属于该对象；没有 checkout，没有用镜像工作文件替代版本。只新增本报告，不修改生产或测试，不联系旧会话，不运行 Gradle/cjc，不实施后端。

下文链接固定到同一 Git 对象。引用简称 R=ReferenceLoader.cpp、L=ASTLoader.cpp、W=ASTWriter.cpp，三者均在 `src/Modules/ASTSerialization/`。

## 1. TypeKind.Type 的真实职责

| 固定源码坐标 | 可直接采用的合同 |
| --- | --- |
| [ModuleFormat.fbs:540](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L540)，581–583 | `Type` 的枚举值为 **28**，注释为 `typealias`；`Generic` 为29。这是 `SemaTy` kind；不等于语法 Type node，也不等于 `PackageFormat.fbs` 的 CHIR Type。正式 CJO AST schema 是 `ModuleFormat.fbs`、标识 `CJOF`（11行）。 |
| [R:216](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L216)，227 | dispatcher 明确为 `TypeKind_Type → SetTypeTy<TypeAliasTy, TypeAliasDecl>`，不是 unsupported，也不是按 RHS 类型改 kind。 |
| [ModuleFormat.fbs:596](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L596)，608–613 | `SemaTy.typeArgs` 保存逐槽参数；`info=CompositeTyInfo`，其中 `declPtr:FullId` 是 Alias classifier 身份，`isThisTy` 在 reader 中仅影响 Class 分支。 |
| [R:346](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L346)，373–397 | 先从 `declPtr` 得到真实 `TypeAliasDecl`，按 `LoadTypeArgs:334–342` 读取完整、有序、可重复的 typeArgs，然后 `GetTypeAliasTy(*typeDecl,typeArgs)`，最后保存当前类型槽。**构造 Alias 身份时不读、不展开 RHS。** |
| [TypeManager.cpp:108](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L108)，54–63；[Types.cpp:1076](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp#L1076) | Alias 使用真实声明与应用参数构造，并由 allocatedTys 复用。Alias equality 走 nominal equality，不能把 display name 或 RHS 展开结果当 canonical classifier。 |
| [L:924](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L924) | 先创建并登记实际 Alias 节点，basic/export 身份完成，再装 own Generic；RHS 留到 refs。 |
| [R:521](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L521)，524、559–560；[R:501](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L501) | `Decl.type` 恢复 `tad.ty`，是 Alias **self**；`AliasInfo.aliasedTy` 恢复 `tad.type`，是 **RHS**。self 不能补 RHS。 |
| [W:944](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L944)，958–963、995–1012 | writer 为 TypeAliasTy 写 nominal 身份和 typeArgs；先 reserve 类型表 index，再递归填充对象。正式 writer 会产生 Type(28)，reader 必须消费。 |
| [R:66](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L66)，86–102 | imported AST 的 RefType wrapper 保留真实 target、原 Alias Ty 和逐槽 typeArgs；带 Alias 时另存 aliasTy。这些是语义类型包装，不是原文语法的重新解析。 |
| [CjoManager.cpp:389](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/CjoManager.cpp#L389)，414–454 | 依赖包全部 refs 完成后，正式编译消费者才替换 Alias 使用处；LSP indexing 分支不替换。替换 walker 在417–418跳过 Alias 声明自身 ty，继续访问其子节点。 |
| [TypeManager.cpp:2508](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2508) | 展开阶段读取实际 RHS，用 Alias own TP 与实际应用参数生成映射，实例化并继续替换嵌套 Alias。`IN_REFERENCE_CYCLE` 是既有语义状态；不能以任意 decoder 重入冒充它。 |

由此主线程的 Type reader 产品必须同时保留：canonical Alias symbol、原应用参数、原 Alias 使用身份、完成后的 expanded 使用类型；Alias owner 独立保存 self/RHS/own TP。是否展开由正式消费模式与完成阶段决定，不能在 classifier lookup 中递归展开 Alias RHS。

## 2. GenericTyInfo.upperBounds：准确加载顺序

1. **包和表域就绪。** [L:405–434](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L405) 创建真实 imported Package/File，423行按 `allTypes` 长度建立空槽。此处没有造完每个 Ty。
2. **真实 owner 与全部 own TP 就绪。** [ASTLoaderImpl.h:146–159](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoaderImpl.h#L146) 创建实际 node 并立即登记 d/e identity。[L:573–588](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L573) 顺序加载 `Generic.typeParameters`，585行给每个实际 TP 设置 `outerDecl=&decl`；此方法不读取 bounds。
3. **跨包声明 barrier。** [CjoManager.cpp:345–386](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/CjoManager.cpp#L345) 先对 common 与完整所需依赖队列执行 `LoadPackageDecls`，队列走完才对各 loader 执行 `LoadRefs`。`LoadAllDeclsAndRefs:397–411` 同样两遍。原生不是在创建第一个 TP 时递归读后续 TP 的 bounds。
4. **当前类型槽查缓存。** [R:269–290](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L269)：wire index 0 → InvalidTy，非零减1；`allTypes[index]` 已有身份便直接返回。
5. **Generic 的身份先发布。** [R:306–316](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L306)：`GenericTyInfo.declPtr → GetDeclFromIndex → 实际 GenericParamDecl → GetGenericsTy(*gpd)`；**316行先存 `allTypes[type]=t`**。
6. **语义 bounds 后填充。** 同文件318–330：仅 `isLoadCache` 跳过 bounds；正常 SOURCE 新 CJO 逐 wire upper index 执行 `LoadType`，325行插入同一个 gTy.upperBounds；327–330以真实 `gpd.outerDecl.GetGeneric()` 登记 assumptionCollection。TP 身份、owner 与该 owner 的 Generic 必须在此之前存在。
7. **完整完成后对消费者发布。** native 依靠声明阶段和 refs 阶段的 barrier 保证；JVM 若采用按需物化，应将同一正式包 context 内的 identity-ready 与 metadata/bounds-complete 分开。内部递归可取真实身份，外部完成视图不能漏 bounds。此项是映射到主线程的框架要求，不是声称 native 有同名状态枚举。

`TypeManager.GetGenericsTy:66–75` 使用 allocatedTys 复用实际 TP 的 Ty；[Types.cpp:138–142](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp#L138) hash 仅取 decl 指针，[1125–1128](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp#L1125) equality 仅比较实际 TP decl。**bounds 不参与 TP 身份构造**；同名但不同 owner 的 TP 不是同一个类型参数。

用户选择的“所有 GenericParams 先 reserve，再 bounds 填充”满足此顺序：先选定实际 owner symbol，遍历完整 TP list 建实际 symbol/声明身份，绑定 containing owner 与参数顺序，登记于 owning package context；第二阶段再填每个 TP 的 final semantic bounds 与 owner 的原 constraints。不要把 native 的顺序改述成“每一种复合 Ty 都先造空壳”：普通 nominal/tuple/func 的 R:346–397 是读参数后才缓存，Generic 才明确先登记再递归 bounds。

## 3. 合法循环与非法循环必须分开

**合法类型图回环合同：** 对已通过语言语义检查的 class/interface 上界，如 `Generic(T) → Interface(I,[Generic(T)]) → Generic(T)`，第一次进入 T 的 R:316 已缓存实际 T；读取上界 I 的 typeArgs 再次进入 T 时，R:277–278返回该身份，不重复加载 bounds、不造第二个 TP、不返回 Any/Unit/错误类型。I 构造完成后仍填回原 T.upperBounds，然后登记该 owner 的 assumptionCollection。

**后列 TP 与跨包引用同理：** 某个上界先引用 owner list 后列 TP 或另一个包的 TP，必须取先前声明阶段登记的真实 symbol；不得依据当前 decoder 的栈、名字或当前包造替身。是否互相约束的具体源程序合法，取决于下述语义检查，不能从“图可恢复”推成“任意互相约束源码合法”。本轮没有编译具体 fixture，不提供虚构的成功 cjc 证据。

| 固定语义坐标 | 循环边界 |
| --- | --- |
| [PreCheck.cpp:146–162](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp#L146)、[960–989](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp#L960) | `AreUpperBoundsDirectlyRecursive` 只沿 generic 的直接 upper 链检查 TP 循环；970–977对直接递归发 `sema_generic_param_directly_recursive`。979–985对非 ClassLike 上界包含递归 TP 发另一诊断。ClassLike 上界中的 typeArg 回环不会因这条检查直接被拒绝。 |
| [PreCheck.cpp:103–143](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp#L103) | containment 检查有 visited 集；跨 owner TP 不沿同一 owner 上界链假设自递归。 |
| [PreCheck.cpp:1010–1025](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp#L1010)、1041–1082 | 仍须满足真实 generic 实例化约束、class/interface 上界规则与多个 class 上界的一条继承链；图中的回边不是免除这些检查。 |
| [Assumption.cpp:99–124](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Assumption.cpp#L99) | 已有约束先 lookup，121行先加入 collection，然后123行递归 AssumeOneUpperBound；语义约束收敛有明确身份/visited 机制。 |

因此合法 loader 重入必须返回同一已 reserve 的真实 binder/type identity。损坏 FullId、不可用依赖、真正非法语义环，各走各自错误产品；不可把这些错误分支用作合法循环的递归 fallback。

## 4. 跨包 canonical symbol 与表域

| 固定坐标 | 路由规则 |
| --- | --- |
| [ModuleFormat.fbs:45–49](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L45) | FullId 的 pkgId、decl exportId、index 分别承担包路由、跨包身份、当前包 d 索引；不能只用数字 index 跨包共享 cache。 |
| [L:532–569](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L532) | 明确要求全部 decl 已加载。CURRENT 从 allLoadedDecls[index] 获取；imported 从输入包的 importedFullPackageNames 路由到目标包 exportIdDeclMap[decl]；PACKAGE_REFERENCE 返回 package decl。跨包时本地 index 不充当目标声明坐标。 |
| [L:503–529](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L503) | 同一个 importedPackageName 的 exportId map 明确只由一个 loader 写；真实身份在包域内唯一。此处普通 decl 空 exportId 的 identifier 分支不能推广为“按名称创建 TP”。 |
| [L:944–952](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L944) | GenericParamDecl 总是登记本包 d 身份，只有真实 exportId 非空才加入跨包 exported map。 |

主线程的跨包 resolver 必须返回目标包拥有的 canonical symbol/context；多个 reader 请求不得各造一个 owner/TP。类型 memo key 至少保持 owning input/package table 域与 t index，symbol identity 保持真实 d/FullId 域；新 SOURCE 产物、loaded snapshot、不同包的同数值 index 不混代。此为上述 native 路由的 JVM 合同，不评判尚未读取的当前生产实现。

## 5. Generic 的两套独立载荷与槽角色

| wire 槽（ModuleFormat.fbs） | 输入角色 | native 恢复合同 |
| --- | --- | --- |
| `Generic.typeParameters`（60） | 有序 own TP **d索引向量** | L:573–588建立 TP/owner；不是上界向量，不可按 name 分组。 |
| `Generic.constraints`（61）、`Constraint.begin/end`（52–53） | 原/派生约束记录与记录级源位置 | R:432–450按 vector 顺序建 GenericConstraint，调用 CreateAndLoadBasicInfo 恢复位置；不重新解析源文件。 |
| `Constraint.type`（54） | subject 的 **t索引** | R:443将实际 semantic TP Ty 包成 RefType，target 从真实 TP 身份取得；不是 d索引或名称。 |
| `Constraint.uppers`（55） | 每条原约束的有序 RHS **t索引向量** | R:444–446逐槽 WrapType；保留组、顺序、重复条目。 |
| `Constraint.isImplicitlyIntroduced`（56） | 原/派生记录角色 | R:448精确恢复；位置不决定 implicit 标记。 |
| `GenericTyInfo.declPtr`（601） | 最终 semantic Generic 的真实 TP FullId | R:310–316获取真实 TP，再建立/缓存其 Ty。 |
| `GenericTyInfo.upperBounds`（602） | 推断/收敛后的 semantic 上界 **t索引向量** | W:1018–1022以 CmpTyByName 排序 semantic set 以稳定出版；R:325读入 semantic set；不可用 constraints 拼接代替这个产品，也不可把这个排序视作原源码组顺序。 |
| `SemaTy.typeArgs`（611） | nominal 应用参数、Tuple fields、Func parameters 等有序 **t向量** | R:334–342严格逐槽读；Generic 的 bound 在 GenericTyInfo 中。 |

`LoadDeclRefs:524` 先恢复 decl.ty，528行再读该 owner 原 constraints。GenericTyInfo 可以在第一步递归恢复 final bounds；原 constraints 不充当建立 final bounds 的 reader 算法。缓存加载的 INCRE_COMPILE/isLoadCache 排除分支不应用于本轮正式 SOURCE 新 CJO。

## 6. 主线程可直接采用的类型交付合同

完整类型恢复应在同一个正式 materialization 上下文内完成以下顺序：

1. 包路由与真实声明身份登记，包括 Alias/nominal/function owner 及完整 own TP list；所有 TP 绑定真实 containing owner 后才开始 bounds。
2. Type(28) 按真实 Alias classifier＋原 typeArgs 恢复；Generic(29) 先建立真实 TP Ty 身份，再填全部 GenericTyInfo.upperBounds；合法递归复用 identity-ready 对象。
3. 独立恢复 Alias RHS、原/派生 Generic.constraints 与记录级位置/implicit 标记；self/RHS、原 groups/final bounds 分开。
4. 跨包请求路由到目标包同一正式 context；全部所需引用完成后才按消费模式发布 expanded 使用视图。

本报告提供固定 native 合同。没有当前生产修复、真实新产物消费或新窗口测试结果，不能据此关闭完整类型恢复、S5 或 S6。Expression、local/Return/loop 与正式 SOURCE 消费的补充报告另行交付。
