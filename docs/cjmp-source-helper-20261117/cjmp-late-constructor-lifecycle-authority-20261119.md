# CJMP 晚生成 constructor 生命周期考证（2026-11-19）

**裁定：D1 应退休两份旧 fallback 构造实现，保留 native 在 nominal 合并／interop 准备之后按真实成员体生成默认实例 ctor 的语义。specific 没有自己的 ctor 不等于合并后没有 ctor；合并后缺实例 ctor 也不一律是 error recovery。必须增加正式 declaration preparation owner，并在最终 family／member 图发布前完成新节点、TYPES、STATUS、归属和索引合同；不能以 phase=TYPES 冒充完成。**

唯一实现树 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`；HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`；读取包括最新未提交内容。延续已读 `D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md` 的考证流程。本轮 read-only，唯一写入是 apply_patch 新增本报告；不改生产／Fixture／旧报告，不跑 Gradle／cjc，不联系旧 agent。

本轮不复验 SOURCE／This／C extras 或默认 Object 修订，不以局部通过关闭 D1／D3／K01／T6／S5／S6 或整个 S4。已读取 [6a7fdb7f 第二联合记录](source-signature-ctor-cvariadic-second-results-20261119.json) 的 227／215 passed／12 failed；之后收到第三窗口 48／46 passed／2 Alias failed、Ctor/This/Paren/attrs 已推进、aggregate 漂移的用户更新，作为后续状态保留，未独立重核该第三窗口。该状态不改变以下 native 生命周期结论。

## 固定 v113 的真实调用链

上游全从 `D:/code/intellij/cangjie/external/cangjie_compiler` 用 git show 固定 `14276740ffec1262b81b6b5236b1d840af270b37` 读取。固定对象全仓 git grep 未找到名为 `GetDefaultCtor` 的符号；本报告使用实际存在的 `HasCtorForTypeDecl`／`CreateDefaultCtor`／`AddDefaultCtor`，不制造 GetDefaultCtor 的源码坐标。

[TypeChecker.PrepareTypeCheck L2086–2128](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L2086) 的顺序是：

1. L2089 清 Searcher cache。
2. L2091 CheckPrimaryCtorBeforeMerge；[Utils L224–243](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L224) 在文件级 class／struct 上反糖 primary ctor，先于 nominal 合并。
3. L2093 PrepareTypeCheck4CJMP；[CheckCJMP L299–335](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/CJMP/CheckCJMP.cpp#L299) 在 compileSpecific 下执行 nominal common→specific 合并。
4. native backend 条件下 L2097–2098 Java／ObjC PrepareTypeCheck。它们在默认生成之前修改真实成员：[Java L18–34](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/NativeFFI/Java/BeforeTypeCheck/GenerateJavaMirror.cpp#L18)、[ObjC L15–23](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/NativeFFI/ObjC/BeforeTypeCheck/Desugar.cpp#L15)。
5. L2104 AddDefaultFunction；[Utils L246–260](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L246) 对文件进入 CheckDefaultParamFuncsEntry；TOOL_ADD 根不执行此生成。
6. [CheckDefaultParamFunc L2231–2284](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L2231) 对非 broken class／struct 先检查真实实例 ctor 是否存在，缺失才 AddDefaultCtor；另外生成 getter/setter，并对需要的实例 ctor 补默认 super 调用。static init 不充当实例 ctor。
7. L2116–2119 更新位置限制并重建 symbol table；L2124–2128 再补 curFile/default 标记和 warmup cache。

因此，“late ctor”不是匹配器任意补出一个方法，而是**对已准备 nominal 的完整声明生成**，随后有正式符号／归属发布与常规类型检查。默认 super 调用也有这个 owner 顺序；不能因默认 Object／父类型尚未准备，就把“当前没有 super 输入”当成无父类或改由 serializer 补调用。

## 决策依据：看真实未过滤成员体，不看零参数可调用性

[HasCtorForTypeDecl L2219–2227](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L2219) 只判断成员是否具有 CONSTRUCTOR 且非 STATIC：

- 任意参数列表、private／public、是否有 body、是否将被导出都不参与这个存在性判断。
- 不能因为没有“可零参数调用”的 ctor 就另造 `init()`；有一个 `init(x: T)` 也阻止默认生成。
- 不能用导出后的 filtered member 列表判断；native 遍历的是合并后的真实成员体。
- common ctor 合并到 specific 后已经是实例成员；specific 的物理源码列表没有 ctor 不是默认生成理由。

[MergeCommonIntoSpecific L223–249、L275–294](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/CJMP/CheckCJMP.cpp#L223) 先验证 nominal，移动／保留 common 成员进入 specific，更新 outerDecl、依赖和 common root 的导出归属；存在性在这之后判断。本库保留两个物理声明，所以对应的是完整 logical merged declarations，而不是只看 specific.declarations。

[AddDefaultCtor L369–395](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L369) 的实际 guard：

| nominal 当前事实 | native 默认生成 |
| --- | --- |
| 不是 class／struct | 不生成。 |
| class/struct body broken，或 AddDefaultFunction 所在根 TOOL_ADD | 上层不进入相应生成。 |
| OBJ_C_MIRROR／OBJ_C_MIRROR_SYNTHETIC_WRAPPER | 不生成。 |
| common class／struct | 不生成。 |
| JAVA_MIRROR class | 不生成。 |
| 满足其余条件且没有实例 ctor | CreateDefaultCtor，追加到 body 末尾。 |

代码仅检查 COMMON／mirror 等位，**没有用 SPECIFIC 位直接拒绝**。L383–384 的“specific 总会从 common 得到 ctor”是注释中的一般解释，不能把它当成实际额外 guard。specific 是否已经有 ctor，应由合并后的实际成员判断。

## 哪些 specific 缺 ctor 是正常路径，哪些属于恢复

common parser 的前置约束来自 [ParseCJMPDecl L184–186、L216–237](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseCJMPDecl.cpp#L216)：common class／struct 必须看到 CONSTRUCTOR 或 PRIMARY_CONSTRUCTOR；否则报告 `parse_cjmp_in_common_ctor_required`。这个检查**没有排除 STATIC**。

另已核实：[ParseConstructor L1010–1029](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseDecl.cpp#L1010) 只从自己的修饰符加 COMMON／STATIC；[Decl.IsCommonOrSpecific L1012–1014](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Node.cpp#L1012) 只看自身属性。[ParseCJMPDecl L257–260](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseCJMPDecl.cpp#L257) 禁止带 common/specific 修饰的 static init；L279–290 只检查 inner 修饰与 outer 相容，没有要求所有 common nominal 的普通成员都加 common。普通 `static init() {}` 不因处于 common class/struct 中就自动成为 common ctor。

由此得到以下分类；这是固定源码的语义／生成条件结论，没有执行新 cjc fixture，也不保证忽略初始化、继承、FFI 等其他错误后的整程序成功：

| 情况 | native 动作 | 合法性／保留合同 |
| --- | --- | --- |
| specific 没有自己的 ctor，但合法 common 有实例 ctor，合并后可见 | 不新建默认 ctor。 | 可为正常成功路径；common 有默认实现时可继续消费该 ctor，没有默认实现且要求配对时仍必须有正确 specific 实现。不得新建 zero-arg ctor替代 required common ctor。 |
| common 有实例 ctor但有参数／private／未导出／待实现，specific 自己无 ctor | 实例存在，仍不默认生成。 | 可有独立匹配或访问／实参错误；不存在性和可调用性不能合并。 |
| common 只有普通 static init，没有实例 ctor；nominal 成功合并，specific 同样无实例 ctor且非 mirror | common parser 的 ctor presence 可以成立；static 被实例搜索排除，specific 不受 COMMON guard 限制，生成默认实例 ctor。 | **正常 late 生成条件**。不能把所有“specific merged 无实例 ctor”称为 recovery，也不能添加一条原生没有的 SPECIFIC 禁生成条件。 |
| common 本身完全没有任何 ctor／primary ctor | common parser 已诊断；common 自己不默认生成。 | 若后续还分析 specific 并遇到可生成条件，可按 native 保持分析节点；它不撤销 common 源输入错误，不成为合法成功证明。 |
| specific 没有 common 对应物，或 nominal 类型／父类型等匹配失败 | Prepare 后的 specific 仍可能满足 AddDefaultCtor 的结构条件。 | [MatchCJMPDecls L1216–1220](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/CJMP/CheckCJMP.cpp#L1216)／[MatchNominativeDecl L164–189](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/CJMP/CheckCJMP.cpp#L164) 有独立诊断。这里可继续生成以维持正常分析，但不能伪装匹配成功／生成可发布的无错程序。 |
| body broken、TOOL_ADD、对应 mirror 排除、仍为 COMMON | 不进入／不创建。 | 不能为了 IDE、TYPES 或匹配通过强行补 ctor。 |
| ordinary class／struct 无实例 ctor，满足 native guard | 生成默认实例 ctor。 | 原本的正常生成路径；不应因退休 TYPES fallback 而丢掉。 |

“错误恢复”属于**已有 owner/匹配诊断下的分析状态**，不是另一种 body=null 的 ctor 实现。native 没有为这些情况用残缺 factory；若按其结构条件继续生成，仍是完整 CreateDefaultCtor 的节点与属性，原诊断保留。

## 完整生成节点和后续完成点

[CreateDefaultCtor L311–345](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L311) 创建真实 FuncBody／空 Block／参数列表、精确 outerDecl、public 实例可见性和 CONSTRUCTOR／COMPILER_ADD／IMPLICIT_ADD。位置与 toBeCompiled 读取准备后的实际成员：[SetCtorFuncPosition L264–281、HasRecompilingMemberVar L296–305](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L264)。调用该工厂本身尚未完成 type／body checking。

native 后续 [CheckFuncBody L203–213、AddRetTypeNode L228–238](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L203) 创建实际 retType；[CheckCtorFuncBody L457–477](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L457) 填实例 owner 类型，static 分支另为 Unit，并检查 body。本库现有 shared [CfirImplicitConstructorBuilder L20–40](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/builder/CfirImplicitConstructorBuilder.kt) 已有完整 raw 空 Block／新 origin／明确事实；它可以复用，不能代替下表的完成证明。

## 本库当前边界与具体问题

| 层 | 实际源码坐标 | 对晚生成的约束 |
| --- | --- | --- |
| 原始成员输入 | [CfirCjmpContainerInput L42–58](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirCjmpContainerInput.kt) | original physical members 是不可修改输入。不能把 merge 后生成节点偷偷当成原源码候选或覆写已发布 common input。 |
| 拓扑收集／事实准备 | [BatchFamilyAggregate.prepareGroup L193–205、L213–235](../../cfir/providers/src/org/cangnova/cangjie/cfir/session/CfirCjmpBatchFamilyAggregate.kt) | 先收集 declarations/children，后准备根事实和 TYPES；若在后面的 TYPES 中临时追加 ctor，当前收集拓扑未自动包含它。准备必须先于最终收集/冻结。 |
| structure／family freeze | [PreparedGroup L32–45、L68–105](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpFamilyAggregate.kt) | containers/children 已冻结；pure evaluateStructure／evaluate 不得写 CFIR、查 provider 或推进 phase。本库“family.freeze”在这些冻结构造与求值边界体现，不是可在末尾调用后再添节点的开关。 |
| 查成员体 | [Structure L154–158、Family L315–319](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpFamilyAggregate.kt) | membersFor 有导出过滤；mergedDeclarationsFor／merge.members 才是完整合并声明体。HasCtor 必须用后者，不能用 filtered export/call 结果。 |
| 当前 late mutation | [Matcher L78–79、L89–94](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt) | 当前先拿 structure/family，才 addSpecificDefaultConstructorAfterMerge。旧工厂 [L192–218](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt) 生成 body=null／ImplicitDefault、phase=TYPES，但 ref 仍 implicit。这既不能充当 native 完成节点，也没进入已冻 members/owners。 |
| derived cache | [SingleFlightCache L20–36、L46–49](../../cfir/providers/src/org/cangnova/cangjie/cfir/session/CfirCjmpSingleFlightCache.kt) | 完成值按 key 保留；同线程递归读未发布值直接失败。不能在其 pure family 求值中再调用同一个 family/structure 取得生成资格。 |
| provider | [SourceSymbolProvider L88–145、L255–256、L857–860](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirSourceSymbolProvider.kt) | raw 一次注册后 FINALIZED，禁止再 record raw；ctor 的 file/owner 索引在注册时建立。追加到 class list 不等于注册了归属。当前 API 没有正式晚声明发布入口，必须显式扩展生命周期，不能悄悄重复 raw record。 |
| scope 的旧旁路 | [MergedDeclaredMemberScope L51–56](../../cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirCjmpMergedDeclaredMemberScope.kt) | ACTUALIZED 把 live ImplicitDefault 拼到 family.membersFor；INSTANTIATION 只读冻结 mergedDeclarationsFor，两视图不同。应消费一份含实际生成成员的完成图，而非继续此追加路径。 |
| scope 代次 | [ScopeProvider L415–435、L462–480](../../cfir/providers/src/org/cangnova/cangjie/cfir/scopes/CfirCangJieScopeProvider.kt) | 会比较 snapshot／physical member 输入并退休 owner/phase stamp；可复用该机制发布新完整代。但刷新 scope 不能补回旧 family 的缺成员、owner 或 provider file 映射。 |
| CLI／LL 驱动 | [ComponentsContainers L83–85](../../cfir/entrypoint/src/org/cangnova/cangjie/cfir/session/ComponentsContainers.kt)、[LLFamilyAggregate L35–46](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpFamilyAggregate.kt) | 两者已有共用接口／不同准备实现的形式；constructor preparation 应沿同一方向接入，不能只在 CLI matching 写树。 |

D1 两个旧实现应删除／收敛，不等于删除 native 的正常晚生成语义。[TYPES fallback L978–1001](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 及 matching 旧 factory 的方法存在性、body-null／类型状态差异仍未关闭。D3 的旧 origin/source-kind 消费者也需跟随正式生成角色迁移，本报告不重签其通过。

## 可实施的 preparation owner

**建议新增 resolve-owned 的 `CfirCjmpNominalPreparation`，实现在 `cfir/resolve/src/.../resolve/cjmp/`，通过 tree/session 的接口向 providers 聚合层提供已准备 nominal 产品。** 这是建议新增的接口/owner，目前仓库没有已完成该合同的实现。职责对位 native PrepareTypeCheck 的 nominal/生成准备；既不归 serializer/scope，也不在 pure match evaluator 内做树 mutation。

现有 [CfirCjmpDeclarationFactsPreparer L429–435](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpFamilyAggregate.kt) 可作为依赖方向参考。新 owner 应在 `structureFor`、`matchingGroupFor`、`defaultParameterStatesForBootstrap` 的**最终 cache 计算/发布之前**完成，供不同 matching mode 复用同一 prepared nominal generation；ctor 存在性不依赖 final callable/body/default 匹配，不应为生成 ctor 强行推进一整组 BODY。

建议合同与执行次序：

1. **输入准备。** 精确取得源码 nominal roots、物理 file／symbol、已展开/primary 已准备的原始成员，完成 nominal self／super／TP 与现有 ctor 的必要 TYPES/STATUS。保留 common 源输入 ctor-presence诊断、nominal 合并失败事实、TOOL_ADD、body-broken、interop 标记；不靠 null／名字／fake kind 猜这些事实。头部自身解析使用实际 RAW header scopes，不能递归索取尚未准备的完整 family。
2. **私有纯 merge 预览。** 用完整 original container inputs 运行相同 nominal merge evaluator，得到未导出过滤的 logical members/对应关系。这份预览只用于 preparation，不发布成可供调用/实例化消费者缓存的完成 family。original common/specific 输入保留，imported common 只读。
3. **interop 与 native 资格。** 在预览合并后的实际上下文完成相应 interop preparation 后，按 TOOL_ADD/body-broken/COMMON/mirror 和全部非 STATIC ctor 判断生成。现有 real ctor，包括 private、有参、interop 生成 ctor，都阻止默认生成；早期 raw provisional implicit ctor须由此 owner 明确采用、退役或替换，不能把其存在性用来跳过最终 native 资格核验。普通与 specific 都服从这份实际规则，不能只写 `if (isSpecific)`。
4. **真实创建与准备。** 需要生成时使用共同 factory，绑定 specific/ordinary 的准确 owner/file 与自有 TP 引用，保留 native append 顺序、来源/属性及对应默认 super 生成合同。声明自己的 COMMON/SPECIFIC 修饰不能从 owner.status 全量复制来假扮源码匹配候选。新 ctor从 RAW 开始，先准备其 metadata／owned graph 与明确 owner/file，再实际解析 TYPES 返回 ref／参数/上界/注解及 STATUS，形成同代完成产品。已完成 owner 的 phase 不会自动完成后来出生的成员。
5. **正式发布 generation。** 将新节点的 physical/list 与 logical/member 布局、file/owner、symbol、source/annotation metadata、declared order 和域一起提交；形成含生成节点的完整成员产品与 owner maps。保留原始源码 container，生成节点通过正式 completed layout 表达，不伪造 original source input。provider 必须有正式准备声明发布合同，具体见下一节。
6. **再冻结与查询。** 从这一完整输入收集 declarations/children，准备 declaration match facts，发布 structure/family；所有 match mode 和两种 member view读取同代 complete layout。不能保留 ACTUALIZED-only live ctor追加；必要的新代发布沿现有 scope owner/epoch retirement，旧不可变 snapshot不被改写。
7. **正常 BODY／导出。** 新 ctor在已注册所属域中走正常 BODY/CFG；之后才 capture export TypeUseGraph／SOURCE encoder。constructor preparation 只证明已创建/头部完成，不把 empty Block 的存在当作 BODY 完成。

原始语义输入与完成生成成员图分开，是 native “先原声明合并，再真实生成”在不可变产品中的表达；不是为绕过旧缓存另建一个 callable scope 兼容列表。

### Provider 提交必须具体解决当前 FINALIZED 限制

当前源码 provider 不能通过现有 API 重新注册晚节点。可实施方向是为 `CfirSourceSymbolProvider` 增加**正式 prepared-declaration generation 提交**，与 `recordExpandedFilesOnce` 的 raw 一次注册合同明确分开。接口只接受 preparation 的完整产品，统一提交 ctor/file/owner 和所需声明索引/metadata；禁止任意 raw file 二次写入或由名字推 owner。

准备阶段的 late ctor应携带明确 file/owner，在私有目标上用已有实际 resolver 服务完成头部，避免“为了 lazy lookup 先向公共 scope 暴露半节点”。[TypeResolveTransformer.resolveDesignatedDeclarationTypes 的 LL 调用 L165–169](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirTypeLazyResolver.kt) 已有 explicit file／containing declarations；new target 的类型准备应使用这些真实上下文和写入/恢复合同。STATUS 用 [StatusResolveProcessor.transformConstructor L611–615](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirStatusResolveProcessor.kt) 的真实 status/参数完成流程。若改为普通 lazy target 驱动，则 provider/resolve target 也必须先具备精确归属，并在同一 preparation barrier 中阻止外部读半代；两者不能靠 phase标记替代。

LL 文件缓存当前 [getContainerCfirFile L144–146](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/file/builder/LLCfirDeclrationsCacheForModule.kt) 依赖 PSI 文件；[LLProvider L127–128](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/providers/LLCfirProvider.kt) 直接委托它。真正 compiler-added/source-null 节点不得以“恰好能取到 class PSI”替代已准备的 owner/file 合同；需把新成员归属接入同一 LL generation/target 输入。

### 锁、缓存与 matching 准备位置

[LLMatching.doResolveWithoutLock L59–83](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt) 现有位置先推进 previous phase，再准备 candidates/facts/family；这是准备语义与其可取消副作用的正确边界。constructor preparation 应在最终 facts/family 之前协调到这里及共同 aggregate façade，不能留到 [under-lock L86–96](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt) 只为 append ctor而持 specific CJMP 锁，再跨域解析 common。

Pure `CfirCjmpPreparedGroup.evaluate*` 继续无 CFIR mutation、无 provider查询／phase推进；prepared ctor及 completed layout是其输入。nominal preparation 要有独立单飞／取消／恢复合同，不能等待自己尚未发布的 family；生成失败/取消时不发布半 group、重复 ctor或半 provider 索引。现有缓存的“完成 key 持有不可变值”意味着需要在第一次使用 complete generation 前稳定输入，或正式发布新代；不是清空所有 caches 后继续读同一坏输入。

## completion checklist 与不得声称的状态

| 完成点 | 必须真实证明的内容 |
| --- | --- |
| native nominal preparation | primary 已准备、完整未过滤 merge、interop/排除/body-broken 来源事实、HasCtor的任意非 STATIC 判据。 |
| fresh ctor | 真实空 body、参数列表/符号/owner/file/TP、编译器生成身份与属性、按 native 的顺序/位置与相应 super 生成。 |
| TYPES | 不是 implicit 占位；实例 owner返回 Type 节点/COMPILER_ADDED 原型合同成立，self/TP域正确；不能用 ctor.resolvePhase=TYPES 冒充。 |
| STATUS | constructor 本身与 signature internals 的实际 resolved status/可见性完成，不只复制 class.status。 |
| provider | ctor symbol 具有精确 file/owner与同代 metadata/索引；raw FINALIZED 不被旁路重开。 |
| complete family | 原始来源输入保留，生成成员纳入声明次序、physical/logical owner、完整布局；不同 matching mode和两种 view一致。 |
| BODY | 正式 owner Type/Block/CFG完成，未放宽 gate，不由 source-null或错误恢复 stub假定成功。 |
| behavior | 需未来固定输入窗口核对正常 late、继承 common ctor、有参/private ctor、static-only common、缺 common／错误 common、mirror／TOOL_ADD/broken、CLI/LL 和取消/换代。此次不运行。 |

这是一份可实施的生命周期合同与 owner 选择，不是现有实现已满足的证明。D1 应保留上述 native 正常晚生成与必要的错误分析节点，退休的是旧残缺/越界实现；D3、新旧 source/phase/provider/scope 余额不因本报告关闭。T6/S5/S6、默认 Object修订复验与整个 S4 仍按各自余额继续。

## 关键磁盘候选

以下 16 个相关文件在 `2026-11-19T05:39:46.5439680+08:00` 与 `2026-11-19T05:43:17.0845665+08:00` 两次 SHA-256 读取一致；它们标识本轮 read-only 版本，不代表其他生产字节或任何测试窗口。

| RepoRelative 文件 | SHA-256 |
| --- | --- |
| [cfir/providers/src/org/cangnova/cangjie/cfir/session/CfirCjmpBatchFamilyAggregate.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/session/CfirCjmpBatchFamilyAggregate.kt) | `55491FD21609DC6BA6641221D6424131935D50242A6E440AFB9687052DB12D49` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpFamilyAggregate.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpFamilyAggregate.kt) | `42695E3A129E35071A43F90C697A4D2462932C8310596951AE0C9A03F9770924` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirCjmpContainerInput.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirCjmpContainerInput.kt) | `AE4D09342838FBC3F4D51BB1DC9EDFB30FE01B8A4B281BCF30E2D76E0D4395CB` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/session/CfirCjmpSingleFlightCache.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/session/CfirCjmpSingleFlightCache.kt) | `2C8D1C867E057A31525C1C2DB96A2A6ECCEC6204BD0FFA12CD4FDF8FD019282A` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt) | `087291AF3C53EABF2606B701A44C53C8AA7F45B51F7467337B8A88FAB2ED7A68` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) | `309C4F7A9B7B2FC7FB01C1AD4172C2DA06B59B5C943EEA12D1B87B4DBEFB7DC4` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirStatusResolveProcessor.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirStatusResolveProcessor.kt) | `37F6076B20A54C124521791698EBE4D84F1BFD26541384B9B48FF22FCBE90549` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirSourceSymbolProvider.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirSourceSymbolProvider.kt) | `339B57B5E614CFC31F64F204E5B06D7E9997E1A882D995094918762BA3A10955` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/scopes/CfirCangJieScopeProvider.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/scopes/CfirCangJieScopeProvider.kt) | `F6466BA94ECE64D72260A6783994334E57385958BC579EFC2E2FB5B284DF1EBA` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirCjmpMergedDeclaredMemberScope.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirCjmpMergedDeclaredMemberScope.kt) | `96C42DD051D9C9B6DDBB13E9083280B34EE3B76452FFA817BBBCB85F3D6835E5` |
| [analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt) | `148F818EDAD416191A55B84D55A0AC24505EF94FD6A3AF0AAB7AF15680049677` |
| [analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpFamilyAggregate.kt](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpFamilyAggregate.kt) | `C05FFE89857286C629D0B97292D0AAB6E3E82FF1AC6A88FAE14C389DC7BC2EB6` |
| [analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirTypeLazyResolver.kt](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirTypeLazyResolver.kt) | `3C6C51FEA175F9015B4389DF310DB079F8B582EAB6DBCCD837C368AAAFE06964` |
| [cfir/entrypoint/src/org/cangnova/cangjie/cfir/session/ComponentsContainers.kt](../../cfir/entrypoint/src/org/cangnova/cangjie/cfir/session/ComponentsContainers.kt) | `94AFE44A90442F64E75CB79C94DBF88989E1CB53E526791761DFAE10A581E965` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchRunner.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchRunner.kt) | `39949D5341D35E071A82D70480384E65F971DF27EE3F010534BF1BA2868B3733` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/builder/CfirImplicitConstructorBuilder.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/builder/CfirImplicitConstructorBuilder.kt) | `EDFDAB80DD7F3A5D69F35A29EB6CAF1655C99BECB3A4CE635539B8AC964F47CC` |

