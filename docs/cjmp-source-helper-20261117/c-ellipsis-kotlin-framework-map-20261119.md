# C foreign ellipsis 的 Kotlin FIR → CFIR 框架映射（2026-11-19）

角色：KotlinFrameworkMapper。交付物是供主线程实施的源码映射报告；本轮没有修改生产、测试或构建文件，没有运行 Gradle/cjc，没有联系停止的会话。唯一写入本报告。

## 1. 证据身份与结论

一方源码只读取 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，读取时 HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，有未提交修改。本报告的一方行号属于读取的工作文件，不能用 HEAD 代替它。主仓库及 `external` 均只读。

起点为 [C variadic SOURCE 实参协议考证](c-variadic-source-authority-20261119.md)。本轮另行读取指定技能及其 `references/parity-checklist.md`，并通过 **`git show` 固定对象**核实下文全部 Kotlin 文件，不使用 Kotlin 工作目录内容代替提交证据：

```powershell
git -C D:/code/intellij/cangjie/external/kotlin show -s --format=fuller 3c7a5041f238102285764d370665f56f587f9dc4
git -C D:/code/intellij/cangjie/external/kotlin show '3c7a5041f238102285764d370665f56f587f9dc4:compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/Candidate.kt'
```

其他 Kotlin 引用也以同样的 `提交:路径` 读取。已确认对象存在，提交标题为 `[Serialization,stubs] Add IS_FULL_VALUE_CLASS flag to metadata`。下文 Kotlin 链接全部固定为 `3c7a5041f238102285764d370665f56f587f9dc4`。

必要的 native 差异从 `D:/code/intellij/cangjie/external/cangjie_compiler` 的 `14276740ffec1262b81b6b5236b1d840af270b37` 通过 `git show` 复核。语言文档合同及既有 cjc 成功事实引用起点报告，不声称本轮重新查询 docs MCP 或重新编译。

**可实施结论：**保留 Kotlin 的 `MapArguments → Candidate → CheckArguments → ArgumentCheckingProcessor → normal completion → results writer → ResolvedArgumentList` 职责链。C ellipsis 必须在这个链条中发布 **真实 formal 的映射和独立、有序的 extras**。extras 接受 CType 期望类型并进入普通 atom/约束/完成流程；成功树保存全部实参，声明签名只保存 fixed 参数。不得用最后一个 formal、虚构 CType formal、Kotlin `vararg Any?` 或错误 ArgList 表示合法 extras。

这是一项有 native 证据的语言模型差异，不能声称 Kotlin 有一个可直接照搬的“无 formal C ellipsis Candidate”。本报告也不构成现有 CFIR 的 100% parity 验收；文件拆分、包位置及 helper ownership 的现存差异在第 3 节明确列出。

## 2. “Kotlin 无 C ellipsis”应限定到哪个层

### 2.1 固定 FIR 的正式模型没有独立 C extras

以下为正向源码证据，不以一次无结果搜索作为全部证明：

1. [FirFunction.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/declarations/FirFunction.kt#L62) L62、65 保存 `contextParameters` 和 `valueParameters`；[FirValueParameter.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/declarations/FirValueParameter.kt#L58) L58 的 `isVararg` 属于**真实参数节点**。
2. [FirArgumentsToParametersMapper.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/FirArgumentsToParametersMapper.kt#L208) L208–224：没有对应参数即 `TooManyArguments`；参数 `isVararg` 才把多个位置 atom 收入该参数。L319–322 的收集结果仍是 `result[parameter] = VarargArgument(...)`。
3. 同文件 L39–65 的 `ArgumentMapping.toArgumentToParameterMapping()` 把 `SimpleArgument` 与 `VarargArgument` 全部展开为 `atom → FirValueParameter`。`DefaultArgument` 不添加源码实参。所有合法显式 value arguments 都有 formal；“每个实参有 formal”不等于“每个 formal 都有显式实参”。
4. [AbstractCandidate.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/resolve/calls/AbstractCandidate.kt#L20) L20–25 和 [Candidate.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/Candidate.kt#L187) L187–208 的成功 mapping 值均为 non-null `FirValueParameter`，没有 C extras 合同。
5. [FirResolvedArgumentList.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/expressions/impl/FirResolvedArgumentList.kt#L46) L46–47 用 `mapping.keys` 暴露成功实参；L84–113 才在错误调用中保留 nullable formal mapping。writer L753–755 明确说明 null formal 通常只出现在 error reference。

补充对象检索：`git grep -n -i -E '(hasVariableLenArg|cvariadic|ellipsis|cvararg)' <固定提交> -- compiler/fir` 未找到这套无 formal C-call 合同；有 ObjC variadic override checker 和 dynamic vararg 辅助引用，不能把它们当成 C ellipsis 调用解析实现。

### 2.2 Kotlin/Native 确有 C variadic 支持，但通过真实 Kotlin vararg formal

不能写成“Kotlin/Native 不支持 C 变参”。固定 [StubIrElementBuilders.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/kotlin-native/Interop/StubGenerator/src/org/jetbrains/kotlin/native/interop/gen/StubIrElementBuilders.kt#L693) 的 `FunctionStubBuilder.assembleFunctionStub()` L693–725，在非 JVM 分支 L706–708 明确执行：

```kotlin
val variadicType = KotlinTypes.any.makeNullable().toStubIrType()
parameters + FunctionParameterStub("variadicArguments", variadicType, isVararg = true)
```

[StubIr.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/kotlin-native/Interop/StubGenerator/src/org/jetbrains/kotlin/native/interop/gen/StubIr.kt#L412) L412–416 定义这个参数；[StubIrTextEmitter.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/kotlin-native/Interop/StubGenerator/src/org/jetbrains/kotlin/native/interop/gen/StubIrTextEmitter.kt#L360) 的 `renderFunctionParameter()` L360–366 输出 `vararg`；[StubIrMetadataEmitter.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/kotlin-native/Interop/StubGenerator/src/org/jetbrains/kotlin/native/interop/gen/StubIrMetadataEmitter.kt#L499) 的 `FunctionParameterStub.map()` L499–507 保存 `varargElementType` 并构造 `kotlin.Array<元素类型>` 的参数类型。

所以 Kotlin/Native C interop 在 FIR 入口具有额外的、真实的 `vararg Any?` 参数，普通 FIR 可以继续使用 formal mapping。这与仓颉 SOURCE 保持 fixed 签名、extras 无 formal 的合同不同；不能为了满足 Kotlin `mapping.keys` 前提在 CFIR 中复制这个 stub 参数。

### 2.3 仓颉差异的固定 native 依据

| native 对象内位置 | 证明的必要差异 |
| --- | --- |
| `src/Parse/ParseDecl.cpp::ParserImpl::CheckVariableParams(FuncDecl&)` L2237–2275 | `...` 只允许 foreign、不能首项、必须末尾；发布变长位后 L2272–2274 **删除**该节点。 |
| `src/Sema/TypeCheckUtil.cpp::GetParamTysInArgsOrder` L757–814 | L781–787 为 fixed 取真实参数类型，为 C extras 追加 `GetCTypeTy()` **检查槽**；L809–813 继续检查 fixed 覆盖。检查槽不是声明参数。 |
| `src/Sema/TypeCheckCall.cpp::CheckCallCompatible` L1172–1205 | L1182 每项进入 `CheckWithCache`；L1191–1193 对 C 声明调用的直接 Unit 实参报专门错误；L1196–1197 仅 foreign ellipsis extras 跳过随后 C struct/formal 同型限制。 |
| 同文件 `CheckCallArgs` L2154–2173、`CheckFuncPtrCall` L2189–2230 | 函数类型调用使用 `isC && hasVariableLenArg` 的类型合同，为超出 fixed 的项追加 CType；缺 fixed 仍错误，命名实参有专门诊断；L2205 逐项 Check，L2209–2222 完成 ideal types。 |

Unit 限制在本轮复核的 direct C declaration `CheckCallCompatible` 中明确存在；复核的 `CheckFuncPtrCall` 循环未显示同一条专门 Unit 分支。主线程不得仅凭此表推断函数值调用也具有完全相同的 Unit/struct 诊断分支，应该消费语义权威给出的该入口合同。

## 3. 模块、包、文件的准确位置

以下路径以对应仓库根为基准。`K` 根为 `D:/code/intellij/cangjie/external/kotlin` 的固定 Git 对象；`W` 根为唯一允许写入的 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。表中 K 文件均已 `git show`。

| ID / Kotlin 模块与完整仓库内路径 | CFIR 模块与当前完整仓库内路径 | 位置结论 |
| --- | --- | --- |
| K-STAGE：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/ResolutionStages.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirMapArguments.kt` | Kotlin `MapArguments` 是 ResolutionStages 内部 object；本库拆为 Cfir 文件。stage 职责对应，文件拆分并非原样一致。 |
| K-MAP：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/FirArgumentsToParametersMapper.kt` | `:cfir:resolve`；当前具体 mapper 仍在上述 `CfirMapArguments.kt` | Kotlin 独立文件拥有 `ArgumentMapping`、`BodyResolveComponents.mapArguments`、private `FirCallArgumentsProcessor`；本库没有这个同形拆分。若按严格文件/helper parity 整理，目标是同包 `CfirArgumentsToParametersMapper.kt`，保留 stage 只负责候选创建/发布；此为目标路径，不声称已存在。 |
| K-CAND：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/Candidate.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt` | 包与具体 owner 对应；extras 可扩展该 owner，不能新建旁路 Candidate。 |
| K-ABS：`compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/resolve/calls/AbstractCandidate.kt` | `:cfir:semantics`；`cfir/semantics/src/org/cangnova/cangjie/cfir/semantics/AbstractCandidate.kt`、`AbstractCallCandidate.kt` | 本库已有跨模块抽象可承载公开合同；**模块、包及文件拆分不同**。本轮没有证明模块重映射规则，不能把 `semantics` 当作 Kotlin `semantics.api` 的原样对应。严格目标位置为 `cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/calls/AbstractCandidate.kt`，迁移前必须核查依赖边界。不得因此另加一套 resolve-only 公共抽象。 |
| K-CHECK：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/CheckArguments.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirCheckArguments.kt` | stage 和文件职责对应，Cfir 名称本地化。 |
| K-PROC：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/ArgumentCheckingProcessor.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/ArgumentCheckingProcessor.kt` | processor 和 helper 的职责 owner 对应；参数签名及仓颉内部处理不逐字相同。 |
| K-ARGUTIL：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/ArgumentUtils.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/ArgumentUtils.kt` | ordinary formal 的 expected-type helper；C extra 不需要 fake formal 来调用它。 |
| K-COMP：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirCallCompleter.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt` | normal completion owner 对应。 |
| K-CS：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/ConstraintSystemCompleter.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/ConstraintSystemCompleter.kt` | 约束完成、变量排序与 postponed atoms 收集的 owner 对应。 |
| K-DFS：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/CandidateTraversal.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/CandidateTraversal.kt` | 全 atoms DFS 职责对应；Kotlin 的 private Context helper 与本库 private atom extension 拆分不同。 |
| K-WRITER：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt` | `:cfir:resolve`；`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt` | 本库 writer 在 `inference`，Kotlin 在 `transformers`，**不是精确包对应**。严格目标为同模块 `resolve/transformers/CfirCallCompletionResultsWriterTransformer.kt`；本轮只定位，不移动。 |
| K-REMAP：`compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/BodyResolveUtils.kt` | `:cfir:resolve`；当前 `remapArgumentsWithCangjieVararg` 在上述本库 writer 内 | Kotlin vararg remap 是 body/resolve 顶层 helper，本库是 writer 私有方法；处理 Array 语法糖有语义差异，但不能声称 helper placement 相同。C ellipsis不进入任何数组 remap。 |
| K-LIST：`compiler/fir/tree/src/org/jetbrains/kotlin/fir/expressions/impl/FirResolvedArgumentList.kt` | `:cfir:cfir-tree`；`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirAbstractArgumentList.kt` | Kotlin abstract/impl 均在 expressions.impl 单独文件；本库合并到 expressions 下的 abstract-list 文件。严格目标为 `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/impl/CfirResolvedArgumentList.kt`。 |
| K-BUILD：`compiler/fir/tree/src/org/jetbrains/kotlin/fir/expressions/FirArgumentUtil.kt` | `:cfir:cfir-tree`；`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/CfirArgumentUtil.kt`，另有 expressions/CfirAbstractArgumentList.kt 同名 builder | 本库两处 builder，第一处包在 `cfir` 根。Kotlin 单一 expressions utility owner；严格目标为 `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirArgumentUtil.kt`。不能用兼容重载保留两套产品合同。 |

第 4–8 节按**当前实际位置**给主线程定位；上述严格目标是 parity 整理时的归属，不表示这次只读报告已完成移动。无 formal extras 的产品扩展具有语言证据；普通文件/包迁移本身没有 C ellipsis 语义豁免。

## 4. MapArguments 与 formal/extras 发布

### 4.1 Kotlin 方法和本库现有入口

| K 位置 / 方法 | W 位置 / 方法 | 必须保持的职责 |
| --- | --- | --- |
| K-STAGE L709–756，`MapArguments.check` | CfirMapArguments.kt L88–129，`CfirMapArguments.check` | 为**每个候选**重新创建 raw atoms，不共享会被初始化 subAtom 的可变 atom；调用 mapper，再初始化 Candidate，diagnostics 交 sink。 |
| K-MAP L69–112，`BodyResolveComponents.mapArguments` | CfirMapArguments.kt L150 起 `mapCallableArguments`、L385 起 `createCallableArgumentMapping` | 实际的调用形状、位置/名字/覆盖检查由 mapper owner 完成；不是 stage 之外的源码特判。当前方法归属不等同于 Kotlin extension，见第 3 节。 |
| K-MAP L144–225，`FirCallArgumentsProcessor.processNonLambdaArguments / processNonLambdaArgument / processPositionArgument` | `createCallableArgumentMapping` L413 起的实参循环 | fixed 按真实参数绑定；C extra 是固定参数边界之后的新合法角色，不是 `parameter == null` 的错误恢复。 |
| K-MAP L227–247、332–448，`processNamedArgument / getParameterByName / findParameterByName` | 同 mapper 的命名实参处理分支 | 保持语言自己的名字诊断；foreign 不因为 Kotlin 支持 named vararg 而获得命名参数。 |
| K-MAP L249–282，`processExternalArgument / processExcessLambdaArguments` | CfirMapArguments.kt L637 起 `mapTrailingLambdaArguments` | Kotlin externalArgument 指括号外 lambda，**不是 C extra**。C extras 也不能借此绑定最后一个 fixed。 |
| K-MAP L284–315，`processDefaultsAndRunChecks` | mapper 的参数覆盖/default-state 检查 | Kotlin empty vararg/default 省略真实 formal 的机制，不应用于 foreign fixed。C ellipsis零 extras 没有待构造的空数组formal。 |
| K-MAP L47–64，`ArgumentMapping.toArgumentToParameterMapping / numDefaults` | Candidate 初始化调用及 CfirMapArguments.kt L757–775 `createArgumentMappingOutcome` | non-null formal mapping 继续只描述真实绑定；extras 独立发布，不能改成末尾 formal 或以 null 值塞进成功 mapping。 |

### 4.2 实施数据合同

令真实 fixed formal 为 `P=[p0…p(n-1)]`，源码 value atoms 为 `A=[a0…a(m-1)]`。合法 direct foreign C ellipsis 要求 `m >= n`，有：

```text
M = { a0→p0, …, a(n-1)→p(n-1) }     // fixed formal mapping
E = [an, …, a(m-1)]                   // 独立 C extra atoms，可为空
A = ordered fixed atoms ++ E
dom(M) ∩ E = ∅
```

识别 C ellipsis 应消费 declaration STATUS 已发布的 `interopInfo.resolvedAbi.isCFunction && hasVariableLenArg`；函数值调用消费其完成/解析的 `ConeFunctionType.isCFunc && hasVariableLenArg`。`ResolveUtils.kt` L245–259 已示范声明 STATUS 推进及函数类型投影。不能按短名 `CFunc`、`foreign` 文本、最后一个参数类型或“不存在 formal”推断 C ellipsis。

本库 Candidate L799–808 的 `declaredParametersForMapping()` 返回声明参数或函数值 fixed 参数；L962–1000 的 `callableValueParametersForMapping()` 只按 `functionType.parameterTypes` 合成 **真实类型槽对应的 fixed 参数**。不要把 extra 的 CType 检查槽追加进该循环。common/synthetic invoke 的 C 位应来自真实函数类型 receiver；不能只检查 synthetic invoke 声明上没有的 foreign 标志。

主线程应在 CfirMapArguments.kt L160–166，**普通 Array 识别以及 L223、259、288 的 exact-arity 分支之前**建立这项调用协议。三个数量入口都要消费它，不能仅跳过 direct-call L259。CangjieVariadicUtils.kt L21–25、65–80 的普通 Array 识别必须与 C ellipsis 互斥；即使某个非法 C 签名的最后 formal 看起来是 Array，也不能误获得普通数组解糖。

`ArgumentMappingOutcome` 为合法 C ellipsis发布 `expectedParameterCount=n`、`requiredParameterCount=n`、`maximumAcceptedArgumentCount=null`、`mappedArgumentCount=n`、`matchedNamedArgumentCount=0`、`hasMappingFailure=false`。actual count 仍为 m；extras 数量单独表达。不能把实际 m 当成“已绑定 formal 数量”，也不能把 expected 改成 m。形状失败照常由 diagnostics/sink 淘汰，不能因为额外实参数量合法就把 CType 类型失败强行改为成功。

`CfirCallResolver.kt` L795–822 的 `arityDistance / requiredAcceptedArity / maximumAcceptedArity` 当前仅考虑 Array 变参；必须消费 mapper 发布的 C arity `[n, 无界]`。这不是修改 overload 优先级的理由。

## 5. Candidate：formal mapping 与独立 extras 的准确 owner

| K-CAND 方法 / 行号 | W Candidate.kt 方法 / 行号 | 实施责任 |
| --- | --- | --- |
| `arguments` L189–191 | `arguments` L292–295 | 保持全部候选 atoms，包含 E；不能替换为 formal mapping 的 keys。 |
| `argumentMappingInitialized / argumentMapping` L193–197 | 同名属性 L298–304 | mapping 值继续 non-null；C extra 不产生参数节点。 |
| `initializeArgumentMapping` L201–208 | 同名方法 L332–339 | 一次性初始化 canonical arguments、fixed mapping 和已采用 C ellipsis 的有序 E。 |
| `updateArgumentMapping` L211–213 | 同名方法 L356–359 | Kotlin 此方法只更新 formal mapping；本库另裁剪普通 Array eligible/expected types。C E 不能 `retainAll(mapping.keys)`，因为它从定义上不在 keys 内。 |
| `replaceArgumentPrefix` L224–239 | 同名方法 L455–480 | 同步 atom 身份变更；Kotlin 的此方法用于 contextual invoke 前缀，不表示 C ellipsis语义存在。若共享框架改变 canonical atoms，E 必须依据 old→new 身份更新，不能据 mapping 过滤。 |
| `addPostponedAtom` L253–255 | Candidate 现有 postponed-atom owner | extras 中待完成表达式继续进入同一列表/约束图；不另造 C-only completer。 |

本库 L313–443 的 `_variadicParameter / _variadicElementType / _variadicEligibleArguments / initializeVariadicCallInfo / markVariadicArgument / variadicExpectedTypeForArgument / remapVariadicArgumentExpectedTypes` 均是**有真实 Array formal 的普通仓颉变参**，不能复用作 C E 状态。

建议新增的合同名字为 `CfirCVariadicCallArguments<P>`，明确这是本报告的实施建议，**Kotlin 和当前本库均没有该 API**。只读合同至少发布 `fixedParameterCount: Int`、`extraArguments: List<P>`；候选另保留 canonical `arguments`。未使用 C ellipsis 与使用 C ellipsis但 E 为空应有类型/状态上的明确区分，不能用 `E.isNotEmpty()` 识别协议。

初始化/remap 的唯一可变 owner 仍是 Candidate；跨模块合同放进已有 AbstractCallCandidate 抽象的公共层，不能让 tree/checkers 依赖 resolve 的具体 Candidate 或 ConeResolutionAtom。第 3 节已记录现有公共抽象相对 Kotlin 的位置差异；不能通过新建第二套抽象掩盖该差异。

## 6. CheckArguments 与 ArgumentCheckingProcessor

### 6.1 expected type 准备

| 固定 Kotlin 位置 / 方法 | 当前 CFIR 位置 / 方法 | C ellipsis 所需差异 |
| --- | --- | --- |
| K-CHECK L31–56，`CheckArguments.check` | CfirCheckArguments.kt L69–98，`CfirCheckArguments.check` | 遍历 Candidate.arguments；仅按显式 context 前缀跳过 context。E 必须在此实际被遍历，合法 E 不应触发 hasMappingFailure 早退。 |
| K-CHECK L85–105，`Candidate.resolveArgument` | 同文件 L111–145，同名 private extension | 先确保类型声明，再准备 expected type，最后调用普通 processor。不得改成 extra 裸类型白名单检查。 |
| K-CHECK L112–140，顶层 `Candidate.prepareExpectedType` | 同文件 L170–196，同名顶层 extension | **在 L177 的 `parameter == null` 返回之前**识别已发布 E，给它 CType。fixed 仍走 formal 类型、inout/普通变参选择及 substitutor；没有角色的 unmapped error atom 不因此获得 CType。 |
| K-ARGUTIL L36–51，`FirExpression.getExpectedType` | ArgumentUtils.kt L152 起 `CfirExpression.getExpectedType` | 这是有 formal 的 ordinary helper；Kotlin依 `isVararg` 和 spread/named 决定是否取 element type。C extra 无formal，不能制造 formal 以复用它。 |
| K-CHECK L140，candidate substitutor | 本库 prepareExpectedType L192–196 | CType 目标也应用候选/current/显式约束替换的共同流程；保持泛型和未决类型的 normal constraint ownership。 |

CType 检查目标使用真实 `StdlibClassIds.CType` 的 interface Cone 类型；本库 CandidateFactory.kt L785 已使用 `ConeClassLikeType(StdlibClassIds.CType.toLookupTag(), isInterface = true)` 作为实际约束。`cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeTypeContext.kt` L57–59、421–428 以及同包 `CfirCTypeSemantics.kt::isMetCType` 是现有类型系统能力，不应在 mapper/checker 再复制 primitive/struct/pointer 白名单。

### 6.2 processor 的方法归属与角色传播

| K-PROC 方法 / 行号 | W ArgumentCheckingProcessor.kt 方法 / 行号 | 保留的框架行为 |
| --- | --- | --- |
| public `resolveArgumentExpression` L79–96 | public 同名方法 L194–215 | 建 ArgumentContext，使用包含调用 Candidate 的约束系统；传入 E 的 CType expected type。 |
| public `resolvePlainArgumentType` L98–115 | public 同名方法 L221–240 | 已知类型检查仍归 processor；不是为 E 另建分析入口。 |
| public `createResolvedLambdaAtomDuringCompletion` L117–131 | 同名方法 L246–264 | normal postponed/lambda lifecycle；此方法存在不表示普通 lambda 是 CType。 |
| private `ArgumentContext.resolveArgumentExpression` L136–213 | 同名方法 L270–304 | atom 分派与 single/postponed child 递归沿用普通链；C E 必须进入该分派。 |
| private `resolvePlainExpressionArgument` L215–224 | 同名方法 L360–392 | Kotlin expected null 不检查普通类型；本库还在此处理嵌套调用/候选局部 replacement。给 E 类型目标后必须实际走该链。 |
| private `createArgumentConstraintPosition` L226–230 | 同名方法 L757 起 | 实参 source 与约束位置仍正常生成，不能把额外实参诊断挪到 callee 或末尾 formal。 |
| private `resolvePlainArgumentType` L233–293 | 同名方法 L767 起 | 正规化实际类型并做 applicable-type 检查；CType 是约束目标，实际语言类型仍独立。 |
| private `checkApplicabilityForArgumentType` L304–366 | 同名方法 L784 起 | 子系统、current substitution、类型约束及结构化诊断；本库 L789 的 expected-null 返回不能吞掉 E 的检查。 |

本库 `resolveNestedCallForExpectedType` L402 起没有同名 Kotlin counterpart；它是已有仓颉嵌套调用处理，不应为此次 C ellipsis 再复制一套。E 的 role 必须随 ArgumentContext 的 child 递归与 candidate-local replacement 保留。仅在最终 `checkApplicabilityForArgumentType` 用 `atom in extraArguments` 判断不充分：此时 atom 可能已是包装的 child 或重解析后的替换 atom。应从顶层已分类 argument 传递明确角色，采用当前 ArgumentContext 传播上下文的模式；不是将所有 `parameter == null` 或所有 CType expected type 都视为 C extra。

具体 C declaration 调用规则落在这个 processor 的真实适用性/诊断链：

- 对已确认 C declaration call，按 native CheckCallCompatible 的直接 Unit 规则报告调用实参诊断。**不要从 CType primitive 集合删除 Unit**；该集合用于更广的 C 类型合同。
- 本库 L926–934 的 `CStructCannotAutobox` 对 `@C struct actual != proper expected` 发布错误。仅 `foreign && ellipsis && extra` 的已发布角色获得 native L1196–1197 的例外；仍应完成 CType 类型约束，不得因此跳过整个 processor。
- fixed 参数、普通调用和非 CType extra 保持自己的检查；泛型、literal、错误类型继续 normal constraint/error propagation，不能用一次 `isMetCType(actual)` 决定整体成功。

本库 `cfir/semantics/src/org/cangnova/cangjie/cfir/diagnostic/ResolutionDiagnostic.kt` 有 `CStructCannotAutobox`，`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L774 有实际渲染分派。检索未发现对应 `sema_unit_cannot_as_cfunc_arg` 的调用 resolution diagnostic；现有 `CFUNC_CANNOT_HAVE_UNIT_ARGS` 用于 C 函数签名/类型检查。若补调用错误，必须连通 resolution diagnostic → factory/rendering → 实参 source，不能把签名 checker 当作调用 extra checker。

## 7. Normal completion 与 results writer 的全部关键入口

### 7.1 normal completion 不更换 owner

| K 位置 / 方法 | W 位置 / 方法 | 实施条件 |
| --- | --- | --- |
| K-COMP `FirCallCompleter.completeCall` L78 起、FULL L123–135 | CfirCallCompleter.kt `completeCall` L145 起、FULL L234–239 | normal constraint completion 后建立 final substitutor，调用 results writer。不能 E 预先成功后略过这一步。 |
| K-COMP `runCompletionForCall` L372–404 | 同名方法 L944–960 | 以 `ConeAtomWithCandidate(call, candidate)` 为根完成同一候选系统；不添加 C extra 专用完成器。 |
| K-COMP `createCompletionResultsWriter` L431–443 | 同名方法 L1004–1019 | final writer 获得最终替换器、类型近似、data-flow 和上下文服务。 |
| K-CS `ConstraintSystemCompleter.complete` L65–75 | ConstraintSystemCompleter.kt 同名方法 L113–123 | 接收同一组 topLevelAtoms，进入正常 runCompletion；C extras 不另建约束系统。 |
| K-CS private `ConstraintSystemCompletionContext.getOrderedAllTypeVariables` L428–475，内部 `collectAllTypeVariables` L444–467 | 同名 extension L478–556，内部 `collectAllTypeVariables` L517–549 | 两者都调用 CandidateTraversal，收集 E 内嵌套候选/fresh variables；formal mapping 不作为遍历边界。 |
| K-CS companion `getOrderedNotAnalyzedPostponedArguments(candidate)` L493–496、私有 list overload L498–525 | 同名 overloads L594–601、609–640 | 两者都从 top-level candidate atom 经完整 arguments 图收集 postponed work，保持未分析 atom 的正常分析流程。 |
| K-DFS `ConeResolutionAtom.processCandidatesAndPostponedAtoms` L10–16、private `Context.processCandidatesAndPostponedAtoms` L29–77，尤其 L67–69 | CandidateTraversal.kt 同名 public extension L10 起、private `processAtomRecursively` L19 起，L31–33 | DFS 从 `candidate.arguments` 进入全部实参，因而只要 canonical A 保留 E，E 的嵌套/延迟图就可继续完成。不能改成 mapping-only DFS。 |

本库 CfirCallCompleter.kt L540–552 `addSameClassifierArgumentTypeConstraints()` 只遍历 formal mapping，为 actual/formal 同构类型添加约束；**E 不应加入该映射**。E 的 CType 约束已经来自 CheckArguments/Processor。不要为满足这条 specialized loop 人造 formal。

### 7.2 writer 方法、helper 与当前丢参点

| K-WRITER 方法 / 行号 | W writer 方法 / 行号 | C ellipsis 实施要求 |
| --- | --- | --- |
| `transformFunctionCall` L450–499 | `transformFunctionCall` L397 起，核心 L449–477 | 成功、失败引用分别发布正确树产品；合法 E 不是 error reference。 |
| private `FirNamedReferenceWithCandidate.computeAllArguments` L526–530 | 同名方法 L357–365 | Kotlin 成功 `mapping.keys.unwrapAtoms()` 有全部 formal 前提。本库 C 成功应从已接受的完整 canonical atoms 得到 value 实参，保留 E。error 分支仍保留原始 args。 |
| private `Candidate.handleVarargsAndReturnResultingArgumentsMapping` L757–777 | 同名方法 L941–973 | 只对实际普通 vararg/Array 语法糖执行收集；C E 保持独立顺序，不进入数组 remap。 |
| private `ResultingArgumentsMapping` L744–747 | 同名 data class L931–935 | 当前仅 regular formal map＋nullable allArgsMapping 不足以发布合法 E；需要完整 ordered accepted arguments 和独立 E 的明确成功产品。 |
| K-REMAP 顶层 `remapArgumentsWithVararg` L34–121 | writer 私有 `remapArgumentsWithCangjieVararg` L988–1034 | 前者构造 FirVarargArgumentsExpression，后者构造 CfirArrayLiteral；均依赖真实数组formal，**不适用于 C ellipsis**。helper placement 差异见第 3 节。 |
| private `Candidate.createArgumentsMapping` L939–995，formal loop L949–981 | 同名方法 L823–914，formal loop L847–864 | 为每个 E 注册 CType contextual expected type，再沿已有 replacement 注册链传播。不能仅补 computeAllArguments 却让 extras 无 expected type 完成。 |
| `ExpectedArgumentType.ArgumentsMap / ExpectedType / getExpectedType` L1542–1568 | 同文件 L1935–1973 的同名类型/extension | 使用这个已有下传产品；不新增 C-only expected-type 通道。fixed/lambda/replacement 信息正常保留。 |
| private `FirCall.transformArgumentList` L591–662；内嵌 `ArgumentTransformer.transformElement` L597–639 | private `rewriteArgumentList` L739–773；`transformCallArgument` L796–817 | Kotlin 先造 ArgList 再 transformArguments；本库先转换 maps 再造树。职责相接但没有同名 1:1 方法 counterpart；这里不能声称原样 parity。C 实施必须先完成每个 argument，再将 transformed A/M/E 同时发布。 |
| `ArgumentTransformer.transformNamedArgumentExpression` L642–658 | mapper/普通 argument transform 的仓颉命名包装处理 | Kotlin可能构造 fake spread；foreign 不支持命名参数，不照搬这个成功语义。 |
| `LinkedHashMap<ConeResolutionAtom,V>.unwrapAtoms` L1672–1674、`Collection<ConeResolutionAtom>.unwrapAtoms` L1680–1682 | 同名 map extension L2019–2020、collection extension L2001–2003；另有 `unwrapAtom` L2008–2013 | Kotlin前两者只取 `atom.expression`，本库会展开 postponed child；E 视图、fixed mapping、完整 arguments 必须使用同一转换身份。不能各自展开后再靠类型/位置反推关系。 |

现状的两个确定丢参边界：W `computeAllArguments` L364 从 fixed-only mapping 读不到 E；即使在 nullable `allArgsMapping` 暂存 E→null，`rewriteArgumentList` L772 的成功 builder **仍只读 transformedRegularMapping**。两处必须一起改，并将第 8 节树合同一起接通。

`rewriteArgumentList` L749 已有 `IdentityHashMap<CfirExpression,CfirExpression>`，L754–758 的 transform 缓存必须覆盖 fixed、context、E 与 ordered arguments 的所有共享表达式。一个源实参只转换一次；转换后的角色视图、mapping keys 与 operands 必须指向同一结果。`transformCallArgument` L796–817 继续处理 candidate replacements、递归写回及 integerOperatorApproximator；CType expected type不得替换最后得到的实际语言类型。

### 7.3 同一 helper 的其他调用者

Kotlin writer `transformAnnotationCall` L715–737、`transformDelegatedConstructorCall` L997–1023 同样使用 mapping/ArgList helpers；CFIR `transformAnnotationCall` L372–395 也复用本库 rewrite helpers。这些不是 C ellipsis合法目标，扩展公共 builder 时须给它们传入其真实的 ordinary 成功产品，不能用 nullable role 或额外 error 分支兜底。

## 8. ResolvedArgumentList：公共产品及完整生命周期

| K-LIST / K-BUILD 位置与方法 | W 当前位置与方法 | 必须接通的产品合同 |
| --- | --- | --- |
| K-LIST `FirResolvedArgumentList` L18–53，`originalArgumentList` L23 | CfirAbstractArgumentList.kt `CfirResolvedArgumentList` L44–121，original L72 | 原始列表保存语法/source；不是完成后的 E/operands 数据源。 |
| `mapping / mappingIncludingContextArguments` L32、41 | 同文件 L81、90 | 两者继续只描述到真实 value/context formal 的 non-null 映射。extras 无formal 不加入这两张图。 |
| `arguments` L46–47 | 同文件 L95–96 | C 成功列表必须有完成的 ordered value A；对 C call 不再由 `mapping.keys` 唯一推出 A。公开发布完成的 E，零 extras 也保留协议身份。 |
| `transformChildren` L49–52 | 同文件 L107–109 | 委托 `transformArguments` 处理整份成功产品。 |
| `FirResolvedArgumentListImpl` L56–80；`filterArgumentMapping` L66–69 | `CfirResolvedArgumentListImpl` L130–201 | Kotlin由 context-inclusive map 过滤普通 map；本库直接持有两图，没有同名 filter helper。增加完整 A/E 时不能从 context mapping 过滤推出无formal extras。 |
| impl `acceptChildren` L71–74 | 本库 abstract `acceptChildren` L115–118 | 正常子树访问包括完成后的 E；context 访问沿已有树框架的实际合同，不把 E 伪装成 context argument。 |
| impl `transformArguments` L77–80 | 本库 impl `transformArguments` L179–201 | 转换 A、M、E 与 context map 时统一 identity cache，保留顺序、formal identity、共享视图关系。 |
| `FirResolvedArgumentListForErrorCall` L84–113 | 同文件 `CfirResolvedArgumentListForErrorCall` L210–279 | 真实错误调用继续保留所有 args；不得让成功 C call 使用这个 concrete kind。 |
| K-BUILD `buildResolvedArgumentList` L27–32 | CfirArgumentUtil.kt L29–34；CfirAbstractArgumentList.kt L285–290 | 两个当前入口都必须改成同一明确产品合同；严格 Kotlin utility owner 见第 3 节。只给一个 impl 新增不可公开读取的字段不够。 |
| K-BUILD `buildArgumentListForErrorCall` L34–39 | CfirArgumentUtil.kt L16–21；CfirAbstractArgumentList.kt L298–302 | 错误产品与合法 C extra 产品保持明确的不同语义。 |

建议树层只读 C-extra 合同使用 `CfirExpression`，不使用 resolve atom。成功 constructor/builder 同时接收完整完成 A、真实 M/context mapping 与有序完成 E（和 fixed count）；这些视图源于同一 mapper/completion 产品，不能 constructor 再猜尾部。新增接口名称是实施设计，不冒充 Kotlin counterpart。

以下为本库已经存在、必须同步扩展的生命周期 owner；固定 Kotlin K-LIST 文件没有这些同名方法，不能编造 counterpart：

- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirAbstractArgumentList.kt::acceptSourceChildren` L47–50：当前访问 original list 与 mappingIncludingContextArguments keys。新增完成 E 需要实际访问，不能只迁移 original extra 的 source 而漏掉已重写表达式。
- 同文件 `CfirResolvedArgumentListImpl.captureMutableState` L139–155：保存/恢复完整 A/E，以及 M/context map 的共享身份。当前仅保存两张 mapping。
- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirArgumentListMutableState.kt::companion.capture` L11–26：保持 concrete owner 分派；由成功 impl 捕获新状态，不写外部快照补丁。
- `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyVisitor.kt::visitArgumentList` L28–71：L31–36 的分配壳和 L46–54 的填充阶段均需复制完整成功 A/E，fixed/context 参数通过 `remapDeclaration`，所有表达式通过同一 `copyOwned` 身份图。当前两个阶段只持有 mapping，会丢 E。

## 9. 主线程的连续实施路径与 SOURCE/ABI 边界

实施应连续完成下面的同一条数据链；每个节点都具有上文定位，不能只改数量条件后提交“已支持”。

```text
真实声明 STATUS/ABI 或真实 C 函数值类型
  ↓
MapArguments 的 mapper：fixed M + ordered E + canonical A + arity outcome
  ↓
Candidate 初始化/更新/前缀替换：保持 A/M/E 身份与协议
  ↓
CheckArguments.prepareExpectedType：fixed→formal，typed E→CType
  ↓
ArgumentCheckingProcessor：普通 atom 分派、子系统、约束、精确 C-call 诊断
  ↓
CallCompleter + CandidateTraversal：全部 A 的 normal completion / final substitutor
  ↓
results writer：所有 A 的 expected map、replacement、转换一次、发布 A'/M'/E'
  ↓
成功 ResolvedArgumentList：完整 ordered value args + fixed/context map + C extras
  ↓
visitor / transform / owned copy / mutable state / source traversal
  ↓
SOURCE：完整 args / operands；callee 签名仅 fixed；真实 call kind
```

SOURCE owner 为 `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt`：L388 读取完成 argumentList.arguments；L412–421 当前把 `target.hasVariableLenArg` 直接映射为 VARIADIC，必须改为已完成调用的实际语义。direct foreign ellipsis 为 DECLARED，函数值入口为 FUNCTION_POINTER；普通 Array 语法糖实际采用时才有 VARIADIC。不能从函数类型的变长位推导调用解糖 kind。`appliedSignature` L424–440 继续仅使用 fixed parameter types，并保留 `isCFunc / hasVariableLenArg`。

SOURCE 与 C ABI promotions 分开。起点报告已经定位固定 native `ApplyImpl.cpp::HandleVarargTypePromotion / HandleForeignFuncCall` L276–329：只对 extras 做默认 promotions，Int8/Int16 sign-extend→Int32，UInt8/UInt16/Bool zero-extend→Int32，Float32→Float64；其余合法类型按 native helper/ABI 合同处理。不能借用 K-CHECK L222–242 的 signed-to-unsigned integer coercion 或 Kotlin numeric-class conversion 把 SOURCE actual types 改成 ABI 类型，也不能把 E 合成为数组。

本库是否已具有可执行 C ABI lowering 不属于本报告的已验证实现结果。起点报告记录 `chir/cfir2chir/.../Cfir2ChirTypeMapper.kt` 明确拒绝 C/变长函数类型；此报告提供前端生产实施路径，不宣称 C variadic ABI 已完成。

## 10. 此任务不得引入的 Kotlin-only 语义

| 固定 Kotlin 实际实现 | C ellipsis 实施边界 |
| --- | --- |
| K-MAP `isVararg` formal、`VarargArgument` 收集、空 vararg；K-REMAP FirVarargArgumentsExpression | C extras 无 formal，不强制同型，不生成 Array、spread 或 fake 参数。普通仓颉 Array 语法糖另按 native 规则。 |
| K-ARGUTIL L40–48 的 spread/named-array expected type；K-WRITER L642–658 的 fake named spread | foreign 不获得 Kotlin named vararg 或 spread 成功语义。 |
| K-MAP L302–312 的 default-parameter/empty-vararg 补齐 | foreign fixed 全部必须提供，不能用 default 或“空 C vararg formal”补缺参。普通仓颉 default 参数不因此被禁止。 |
| K-STAGE L732–738 的 contextual invoke stubs，K-CAND L224–239 prefix replacement | 保留框架真实的前缀更新入口不等于给仓颉 C ellipsis增加 Kotlin extension-function receiver/context 参数规则。本库 CheckArguments L94–96 已明确普通仓颉实参 `isReceiver=false`。 |
| K-MAP DynamicScope 的命名拆包及多 external lambda；K-STAGE L759–767 `unwrapNamedArgumentsForDynamicCall` | dynamic 不是 C ellipsis，不能用 DynamicScope 放宽参数数量和类型。 |
| K-CHECK L65–79 的 Java SAM compatibility/numeric priority，L144–200 的 SAM conversion | 仓颉本库 L187 明确没有 SAM 转换；不能给 C extras 建 SAM/Java compatibility 分支或兜底优先级。 |
| K-CHECK L222–242 的 implicit signed-to-unsigned coercion；L245–261 的 numeric class conversion | 它们不是 C 默认 argument promotions。不得启用本库 L153–162 的空 coercion 扩展点来“支持”C ellipsis。 |
| K-PROC L251–289 的 suspend/custom function-kind 与 Unit-return conversion，K-WRITER L609–635 的转换包装 | 不把普通函数/lambda 自动转换成 CType/CFunc，不引入 Kotlin suspend/custom function kind 或以 Unit conversion 吞掉 C-call Unit 错误。合法 C function 值走仓颉真实互操作合同。 |
| Kotlin/Native stub L706–708 的 `vararg Any?`，metadata L502–507 的 Array 参数 | 这是 Native interop 的另一种源码入口模型，不能复制成 CFIR ellipsisformal。C extras 的类型集合由 CType 决定，不能换成 Any?。 |

共享 normal inference、postponed work、substitution、tree visiting 是框架职责，必须保留；排除上表语义不允许减少这些处理步骤。现有 CFIR 的其他 Kotlin-only/compatibility 占位不构成本任务增加它们的授权。

## 11. 主线程完成后的可观察合同（本轮未运行）

对 `cfir/analysis-tests/testData/diagnostics2/common-specific/source-frozen-types/functionAbi.cj` L8–10、34：

- mapper/Candidate：M 三项，E 为 `[extra]`，canonical A 四项；min=3、max无界。
- 检查/完成：extra 的 expected 为 CType、actual 为 Int32，进入普通 Processor/completion；没有 fixture 名称特判。
- 成功树：arguments 为 `[first, middle, last, extra]`，M 三项、E 一项；transform/copy/source traversal 保留全部四项的完成身份。
- SOURCE：DECLARED，arguments 四项、operands 五项（含 callee）；callee fixed `[Int32, Int64, Int32]`，返回 Int32，C/变长位 true。Int32 extra 默认 promotion 为 identity。

还需要由主线程/语义测试角色验证零 extras、多个异质 CType、缺 fixed、非 CType、direct Unit、C struct extra、嵌套 generic/literal completion、真实错误调用保留全 args、ordinary exact arity、普通 Array 与 VArray 边界。本轮没有写这些测试、没有执行它们；Kotlin fixed vararg fixtures 不能作为 C ellipsis 的语义期望来源。

读取快照的 SHA-256（用于后续并行生产修改后重新定位）：

| W 文件 | SHA-256 |
| --- | --- |
| Candidate.kt | `4368D8104450DF16CDB2AFF6E99A901B17BCFF4EB7359C626F47B266D1EC919A` |
| CfirMapArguments.kt | `DD99000CD618A6656BFA2561595C37CBA328920DCED0C0453C18F041D2434DB6` |
| CfirCheckArguments.kt | `8B57C0D05135636E36702A43830F1F82D9DDDB10973862AA6A811063D813F171` |
| ArgumentCheckingProcessor.kt | `B75E5E28643E37EE5BB8D4CFDD506009CFBADC25A648E013222636338D1AFE2B` |
| CfirCallCompletionResultsWriterTransformer.kt | `60DE2028FFC7D71E39A658865C61B67FB6F083585E710CD51576CF8D81F92EB5` |
| CfirAbstractArgumentList.kt | `3FA77697B6F94DC0BD690A0F1F7B8319E0CC4D0608B6AFA7A15557EEC1F01E93` |
