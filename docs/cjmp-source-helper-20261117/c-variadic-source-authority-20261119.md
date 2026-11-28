# C variadic SOURCE 实参协议考证（2026-11-19）

状态：只读语义考证与框架实施建议，不构成生产实现、测试数据或 SOURCE 链路验收。

## 1. 范围与证据身份

一方代码只读取 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。读取时 HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，但工作树有未提交修改，下列本库行号指读取时文件，不代表该 HEAD 的提交内容。上游对象从 `D:/code/intellij/cangjie/external` 通过 `git show` / `git grep` 读取：

- Kotlin：`3c7a5041f238102285764d370665f56f587f9dc4`。
- native：`14276740ffec1262b81b6b5236b1d840af270b37`。
- 语言文档：本轮 docs MCP 获取 `manual_source_zh_cn_cangjie_c_仓颉调用_C_的函数_1` 和 `manual_source_zh_cn_function_call_desugar`。文档是补充语义依据，不冒充上述固定源码版本。

本轮承担 CangjieSemanticsAuthority 与 KotlinFrameworkMapper 两项读取职责；没有联系旧会话，没有运行 Gradle、仓库测试或 cjc，没有修改生产和测试文件。typeOrigin、tupleAlias 不在范围内。唯一写入是本报告。

输入为正确 SOURCE fixture [functionAbi.cj](../../cfir/analysis-tests/testData/diagnostics2/common-specific/source-frozen-types/functionAbi.cj)：L8–10 有三个真实形参加 `...`，L30–36 的默认正文调用位于 L34。用户提供的固定 cjc 1.1.3 成功编译事实，本轮读取 [保存的 native 诊断](../../docs/cjmp-source-helper-20261117/frozen-type-functionAbi-native-diagnostics.json) 补证：`exitCode=0`、`Errors=0`、`Warnings=4`，四条均为普通函数体中的 unused 警告。本轮未重跑该命令；两 parser 报 `WRONG_NUMBER_OF_ARGUMENTS` 是用户提供的运行事实，下文用生产链解释其原因。

**直接结论：**这次调用应具有三个 fixed 绑定与一个独立 extra。`extra` 的实参类型是 `Int32`，检查目标是 `CType`，ABI promotion 为 identity。成功 ArgList 和 SOURCE Call 都必须保留四个实参；callee 函数类型仍只有三个 fixed 参数，`isCFunc=true`、`hasVariableLenArg=true`。native SOURCE CallKind 是 `CALL_DECLARED_FUNCTION`，不是 `CALL_VARIADIC_FUNCTION`。

## 2. CangjieSemanticsAuthority：固定 native 的合同

### 2.1 三种形态必须区分

| 形态 | 形参模型与实参处理 | SOURCE / ABI 含义 |
| --- | --- | --- |
| C foreign `f(p0, p1, ..., ...)` | `...` 不占正式形参槽；所有 fixed 必须提供；其后允许零到多个异质 CType 实参，按原顺序独立传递 | 直接声明调用；函数类型保存 C ABI 与变长标志；仅 extras 做默认 promotions |
| 仓颉最后一个非命名 `Array<T>` 参数 | 数组参数本身是真实 formal；先尝试普通完整数组调用，不匹配才尝试把位置序列收集为一个 ArrayLit | 实际使用数组变参语法糖时，源调用为 `CALL_VARIADIC_FUNCTION`；不获得 C ellipsis 标志 |
| `VArray<T, N>` | 固定 native 的普通变参谓词不接受 VArray；其作为独立 C 实参能否满足 CType，取决于元素 CType | 不因尾参数是 VArray 就放开 arity、收集 elements 或设置 C ellipsis 标志 |

第三行是必须保留的边界：不能把“Array/VArray 都是数组”作为普通 variadic 的依据。固定 [Types.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp) L394–402 的 `IsStructArray` 只接受 core 包的 struct `Array`；[TypeCheckCall.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckCall.cpp) L342–358、452–460 使用该谓词，且函数值路径明确禁止 `hasVariableLenArg` 与仓颉普通变参同时成立。docs MCP 的普通变参规则同样只写 `Array`。

### 2.2 fixed 映射与 extra 检查

| 固定证据 | 实际行为及协议含义 |
| --- | --- |
| [ParseDecl.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseDecl.cpp)，L2237–2275 | `CheckVariableParams` 发布声明/列表的变长标志；合法 `...` 必须在 foreign 的末尾且不是首项；随后将 ellipsis 参数节点从 `params` 删除。不得制造 fake formal。 |
| [PreCheck.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp)，L1798–1802 | 从真实 `paramTys`、返回类型及独立 C/变长标志构造 FuncTy。extra 数量不改变函数声明参数类型列表。 |
| [TypeCheckCall.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckCall.cpp)，L1372–1395、1799–1806 | C ellipsis 不受普通 exact-arity 预检淘汰，但仍继续名字、参数覆盖与类型兼容检查；“跳过 arity 预检”不是完整实现。 |
| [TypeCheckUtil.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckUtil.cpp)，L757–814 | `GetParamTysInArgsOrder` 对 `i < params.size` 取该 fixed formal 的类型；C extras 逐项追加 `GetCTypeTy()` 作为检查目标。最终逐项检查 fixed 覆盖，缺失 fixed 不能被 extras 补齐。追加的是检查类型槽，不是声明参数节点。 |
| [TypeCheckCall.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckCall.cpp)，L625–669、1172–1205 | 位置顺序和名字检查继续执行；每个实参通过 `CheckWithCache` 检查。C call 的直接 Unit 实参报 `sema_unit_cannot_as_cfunc_arg`；foreign variadic 的 extra 在该检查后跳过“C struct 与 formal 类型必须相同”的 autobox 限制。 |
| [TypeCheckCall.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckCall.cpp)，L2154–2173、2189–2230 | 函数类型调用同样只为 extras 追加 CType 检查槽；小于 fixed 数量仍报数量错误，命名实参有专门错误，所有实参继续 Check / ideal-type completion。成功该路径的 kind 是 `CALL_FUNCTION_PTR`。 |

docs MCP 明确说明 foreign 不支持命名参数和默认参数，extras 必须满足 CType，但不必同型。不能从最后一个 fixed 的类型、名称或 `CFunc` 的短名推导 extras 的接收协议。

CType 应作为正常类型约束检查目标，而不是“实参是 primitive 就直接成功”。固定 [Types.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp) L856–884 的集合包含 primitive C 类型、CPointer、CString、C function、`@C struct`、递归满足 CType 的 VArray 和前端 Quest。名义 Array、普通函数类型、普通 class/struct 不因此获得 CType。Unit 是 CType 原子，但 C 调用实参的直接 Unit 禁令是另一条规则；不能改变全局 CType 谓词来实现这条禁令。泛型上界、未决类型、错误类型必须继续由类型系统和实参 completion 处理，不能以一次裸 `isMetCType` 判断替代整个调用检查。

### 2.3 normal argument completion 与默认 promotions

固定 [TypeCheckCall.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckCall.cpp) L2021–2028 的 `ReInferCallArgs` 按合法候选的每个检查目标重新完成实参，包括 CType extras。L2148–2150 实际调用它，随后调用 `ReorderCallArgument`；L82–102 的 `SortCallArgumentByParamOrder` 对超出 fixed 的位置实参执行 `args.push_back`，没有把它们覆盖到最后一个 formal。

**SOURCE 类型完成与 ABI promotion 是两个阶段。**固定 [TranslateCallExpr.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/CHIR/AST2CHIR/TranslateASTNode/TranslateCallExpr.cpp) L578–606 的 foreign 调用保留 fixed 函数签名及独立 args；真正 promotions 在 [ApplyImpl.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/CodeGen/Base/ApplyImpl.cpp) L276–317 的 `HandleVarargTypePromotion` / `HandleForeignFuncCall`，L329 在 C call 生成前实际调用后者。

| extra 的已完成语言类型 | 固定 native 的 ABI 动作 |
| --- | --- |
| Int8、Int16 | sign extension 到 Int32 |
| UInt8、UInt16 | zero extension 到 Int32；不要误写成 UInt32 |
| Bool | zero extension 到 Int32 |
| Float32 | 到 Float64 的浮点扩展 |
| Float64 | 保持 double 目标类型；native 对 `IsFloat()` 统一进入 double 分支 |
| Int32/UInt32、Int64/UInt64、IntNative/UIntNative、pointer、CString、CFunc、C struct、VArray 等其他合法类型 | 该 promotion helper 返回原值；具体 C ABI 布局继续由 CFFI lowering 处理 |

[CHIR Type.h](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/CHIR/IR/Type/Type.h) L179–181 的 `IsFloat` 范围是 Float16 到 Float64；这不推翻前端 CType 集合对 Float16 的限制。不能由 promotion helper 接受它就反推源码允许它。

ABI 扩展只作用于 extra。原本声明为 Int8 的 fixed 参数仍按自己的 formal 签名传递。零大小 C 值有独立 ABI 省略规则（ApplyImpl L310–311、324–326）；不能因此从 SOURCE ArgList 删除源码实参。SOURCE 中也不应把一个 Int8 实参改写成 Int32、把四个实参编码为一个数组，或把 ABI 扩展描述成源语言隐式类型转换。

### 2.4 SOURCE CallKind 和 operands

固定 [TypeCheckCall.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckCall.cpp) L2953–2956 实际调用 `CheckCallKind(ce, decl, ce.callKind)`；L2573–2581 对普通 FuncDecl，包括本 fixture 的 foreign 声明，设置 `CALL_DECLARED_FUNCTION`。固定 [DesugarInTypeCheck.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Desugar/DesugarInTypeCheck.cpp) L513–560 才把普通数组语法糖的源调用改为 `CALL_VARIADIC_FUNCTION`，并构造 ArrayLit。

固定 [ExprWriter.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprWriter.cpp) L179–199 写 callee 加全部 `desugarArgs` 或 `args`，并写真实 `ce.callKind`。本 fixture 的预期因此是：Call arguments 四项，operands 五项（含 callee），kind DECLARED，callee FuncTy 的 params 三项。不能把函数类型的 `hasVariableLenArg` 位直接翻译成 SOURCE VARIADIC。

## 3. KotlinFrameworkMapper：框架应保留的职责与允许差异

| 固定 Kotlin owner | 本库对应 owner | 核实的职责 |
| --- | --- | --- |
| [ResolutionStages.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/ResolutionStages.kt)，L709–756 的 MapArguments | [CfirMapArguments.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirMapArguments.kt)，L87–129 | 每个候选创建自己的 atoms，mapper 发布绑定，Candidate 保存结果，diagnostics 交 sink。 |
| [FirArgumentsToParametersMapper.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/FirArgumentsToParametersMapper.kt)，L40–65、208–224 | CfirMapArguments 的 callable mapper，L385 起 | Kotlin vararg 有真实 `isVararg` formal，多个 atom 映射该 formal。该模型不能直接表示无formal的 C ellipsis。 |
| [Candidate.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/Candidate.kt)，L187–239 | [Candidate.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt)，L289–460 | 完整 atoms 与 formal 映射分别保存；初始化、映射替换和 prefix 替换归 Candidate 所有。 |
| [CheckArguments.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/CheckArguments.kt)，L29–105 | [CfirCheckArguments.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirCheckArguments.kt)，L64–145 | 遍历 Candidate.arguments，准备每个实参的期望类型，再调用 ArgumentCheckingProcessor。 |
| [ArgumentCheckingProcessor.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/stages/ArgumentCheckingProcessor.kt)，L79–96、136 起 | [ArgumentCheckingProcessor.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/ArgumentCheckingProcessor.kt)，L194–215、270–304、767 起 | atom 分派、嵌套调用、postponed work、类型约束属于普通实参链，C extras 必须实际进入它。 |
| [FirCallCompletionResultsWriterTransformer.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt)，L465–485、526–530、757–776、939 起 | [CfirCallCompletionResultsWriterTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt)，L357–365、449–476、739–773、823 起、941 起 | 完成期望类型、改写实参、发布 resolved ArgList。Kotlin 成功调用可从 mapping keys 得到完整实参，因为合法 vararg 也映射真实 formal。C ellipsis 打破该前提，需要明确的数据合同差异。 |
| [FirResolvedArgumentList.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/expressions/impl/FirResolvedArgumentList.kt)，L18–79 | [CfirAbstractArgumentList.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirAbstractArgumentList.kt)，L44–201 | resolved argument list 是跨模块的树产品；Kotlin `arguments=mapping.keys` 前提在 C ellipsis 上不能照搬。 |

应保留 mapper → Candidate → CheckArguments / Processor → normal completion → results writer → resolved ArgList 的框架。C ellipsis 的无formal异质尾部，是固定 native 证明的必要语言差异；不能伪造 Kotlin vararg formal 或在 checker/SOURCE 中另做一套调用解析。

## 4. 本库现状与确定缺口

### 4.1 已有 typed 状态与 API

- [CfirFunction.kt](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirFunction.kt) L41 与 [ConeFunctionType.kt](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeFunctionType.kt) L24 已有 `hasVariableLenArg`；后者另有 `isCFunc`。这些是声明/函数类型事实，不是当前调用的 extras 协议。
- 两 Raw 构建入口已经发布变长位：[PsiRawCfirBuilder.kt](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) L1131、1156；[LightTreeRawCfirDeclarationBuilder.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) L727、758。读取范围未显示需要为本错误重写 parser。
- [CfirInteropInfo.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirInteropInfo.kt) L126–166 已发布 `resolvedAbi`，应消费其 `isCFunction`，避免按文本 `foreign` / `@C` 再造 ABI 判定。[ResolveUtils.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/ResolveUtils.kt) L241–260 的函数类型投影先推进 STATUS，保留真实参数列表及两个位。
- [CfirCTypeSemantics.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/CfirCTypeSemantics.kt) 已有 `isMetCType`；[ConeTypeContext.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeTypeContext.kt) L421–428 有 CType corresponding-supertype 支持。应复用类型系统能力，不能在 mapper 写第二套白名单。
- [Candidate.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt) L313–426 的 `_variadicParameter`、element type、eligible atoms、`usesVariadicCall`，以及 L493 起 `cangjieVariadicParameterForCall`，全部绑定普通数组 formal。不是 C variadic typed 状态。
- [CfirCFuncVariadicReferenceChecker.kt](../../cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirCFuncVariadicReferenceChecker.kt) 管理 variadic C 函数引用限制；不能以其存在证明调用 extras 已被检查。

在读取及检索的 resolve / tree / semantics / semantics.api 调用链中，未发现“C ellipsis fixed 映射＋有序 extras＋成功 resolved ArgList”专用产品或公共接口。现有成功 ArgList 仍把全部实参与 non-null formal 映射绑定在一起。错误调用的 nullable mapping 不是正常 C extras 协议。

### 4.2 当前失败以及数量放宽后仍会失败的节点

| 必须处理的现有节点 | 当前事实 |
| --- | --- |
| CfirMapArguments L160–166、255–285、288 起 | 普通变参识别从最后非命名 Array formal 出发。本 fixture 最后 formal 是 Int32，`variadicParameter=null`；四个位置实参与三个位置参数触发 L259–285 的 `WrongNumberOfArguments` 提前退出。 |
| [CangjieVariadicUtils.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/CangjieVariadicUtils.kt)，L64–80 | 只返回真实数组参数的元素类型；不得把 last:Int32 填入此接口。其所用 [ConeBuiltinTypeUtils.kt](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeBuiltinTypeUtils.kt) L143–162 也明确排除 VArray。 |
| [ArgumentMappingOutcome.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/ArgumentMappingOutcome.kt)；CfirMapArguments L756–775 | 最大 arity 只按普通 variadic formal 设为空；`mappedArgumentCount` 文义是成功绑定 formal 的数量，不能偷偷把无formal extra 也算成绑定。 |
| CfirCheckArguments L81–97、170–177 | 已遍历全部 Candidate.arguments，但找不到 formal 的实参目前直接得到 `expectedType=null`。仅放开数量会使 extra 缺少 CType 目标。 |
| ArgumentCheckingProcessor L789、922–934 | null expected type 直接返回；C struct 与 CType 不相等又会触发现有 autobox 诊断。C extra 需要 typed 身份和 native 精确例外，不能全局关闭 C struct 规则。 |
| results writer L357–365 | 成功 `computeAllArguments` 从 `candidate.argumentMapping.keys` 取实参；fixed-only mapping 会丢 extra。 |
| results writer L847–863、941–973、739–773 | expected-type map 只遍历 formal mapping；正常 builder 只接收 `transformedRegularMapping`。即使在 allArgsMapping 中留下 extra→null，成功输出仍丢失它。 |
| CfirAbstractArgumentList L95–97、169 起、285–290 | 成功列表 `arguments=mapping.keys`，转换也只处理 mapping/context keys。单改 writer 或 originalArgumentList 无法发布完整的成功调用树。 |
| [CfirSourceGraphProducer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt)，L388、412–421 | 从完成 ArgList 读取 operands；L416 把 `target.hasVariableLenArg` 直接转为 VARIADIC，与 native C ellipsis 的 DECLARED 合同冲突。 |

## 5. 正式实参协议与发布层

以下接口名称为建议，不是声称本库已有 API。实现必须一次接通数据发布与消费，不允许先以 fake formal、错误列表或未检查 extras 临时过关。

### 5.1 映射协议

令真实 formal 为 `P=[p0…p(n-1)]`，源码位置实参 atoms 为 `A=[a0…a(m-1)]`。

1. mapper 在候选声明 STATUS/ABI 已完成后确认 C ellipsis：声明的 `hasVariableLenArg` 和已发布 C-function ABI；函数类型入口消费其 `isCFunc && hasVariableLenArg`。缺少 required 状态应推进真实 phase owner，不能以名字、最后形参或文本兜底。
2. 合法调用要求 `m >= n`，fixed 按 `ai → pi`（`0 <= i < n`）建立映射。禁止借用 default/named 规则补 fixed，也不为 C ellipsis 收集 ArrayLit。
3. `E=A.drop(n)` 是独立、有序、允许为空的 extras 产品。成功不变量为 `A = fixedArgs ++ E`，fixed mapping 与 E 不相交，所有 A 都有明确角色。extra 无 formal 是成功语义，不是“未映射错误”。
4. 缺 fixed、命名/顺序或其他调用语法错误走原 resolution diagnostics/sink。普通函数没有 C ellipsis 状态时，原数量和数组语法糖规则继续生效。
5. Outcome 发布 `[required=n, maximum=unbounded]`；`expectedParameterCount=n`、`mappedArgumentCount=n`，另有明确的 extra/accepted 数量或角色视图。不能把 expected 改成 m，也不能让 mapped count 的含义依分支变化。

建议 Candidate 只读合同 `CfirCVariadicCallArguments<A>` 至少有 `fixedParameterCount: Int`、`extraArguments: List<A>`；未采用 C ellipsis 与已采用但 E 为空必须可区分。正式 fixed 映射保留现有 non-null value-parameter 类型，不扩成 fake CType parameter 或 nullable “万能formal”。

### 5.2 类型检查与 completion 协议

每个 fixed 保持原 formal expected type 与 substitutor 链；每个 typed extra 获得真实 CType 检查目标，**在 `parameter == null` 提前返回之前**选定。不能把“mapping 没有参数”的任意 atom 都当 extra。

所有 E 实际调用 `ArgumentCheckingProcessor.resolveArgumentExpression`，保留嵌套 call、ideal literal 固定、引用绑定、postponed atoms、约束系统及原错误传播；current/final substitutor 应用于整组 atoms。CType 目标用于约束，不替换实参完成后的实际语言类型。Unit 调用禁令独立发布；C extra 的 C struct 不执行 fixed formal 同型/autobox 限制，普通 fixed 和其他调用仍保留该规则。

本轮检索未发现对应 `sema_unit_cannot_as_cfunc_arg` 的调用级 resolution diagnostic。现有 [CfirForeignFunctionReturnTypeChecker.kt](../../cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirForeignFunctionReturnTypeChecker.kt) 的 `CFUNC_CANNOT_HAVE_UNIT_ARGS` 检查签名形参，不能直接冒充 extra 的调用诊断。应在现有 resolution diagnostic → factory/rendering 流水线补齐这一真实调用错误及其实参 source，勿在 SOURCE 阶段抛数量/类型异常代替诊断。

completion writer 为 extra 也注册 CType contextual expected type，实际转换其表达式并发布最终类型；不得只做一次 CType 布尔判断就跳过 normal completion。ABI promotions 保持在 C ABI lowering owner，消费完成的 actual type 与 fixed/extra 角色。若前端需要展示 promotion 计划，可发布独立只读语义描述，但不得把该计划写成 Source expression 的类型或 callee formal。

### 5.3 公共框架接口应在哪层发布

| 层 | 应发布/实现的合同 | 禁止的层间依赖 |
| --- | --- | --- |
| `cfir:semantics`，现有 [AbstractCallCandidate.kt](../../cfir/semantics/src/org/cangnova/cangjie/cfir/semantics/AbstractCallCandidate.kt) 同包 | 泛型 atom 的 C extra 只读接口，以及完整 arguments/角色视图；Candidate 实现初始化和后续 remap。跨模块读取者能区分 fixed 与 C extras，不强转具体 Candidate。 | 不让 checkers 反向依赖 resolve；不让语义抽象解析 PSI。 |
| `cfir:resolve/calls/stages`、Candidate、inference | 唯一 mapper owner 分类与发布；CheckArguments 准备 CType；Processor 处理普通实参与精确 C-call 规则；normal completer/final writer 发布完成产品。 | 不在候选外临时合成末尾formal，不在 serializer 补类型解析。 |
| `cfir:semantics.api/types`，现有 CfirCTypeSemantics / ConeTypeContext | 复用 CType 类型关系及已发布 interop 事实。需要的高层查询通过这里的接口/函数暴露。 | 不复制 CType 白名单到 parser、mapper、SOURCE producer。 |
| `cfir:cfir-tree/expressions`，现有 CfirResolvedArgumentList 抽象及 builder | 完整成功 value arguments、fixed/context 映射、有序完成 C extras。公开接口能读取 n 与 E；无 Candidate 对象、无原生 ABI runtime 对象。 | tree 不依赖 resolve 的 atom 实现，也不反向引用 semantics.api 的 SOURCE payload 枚举。 |
| `cfir:resolve/source` → `cfir:semantics.api/source` | 消费完成调用/ArgList 的真实角色，构造现有 CfirSourceCallKind：C direct DECLARED，C function-value FUNCTION_POINTER；普通数组语法糖的 VARIADIC 由实际解糖事实发布。 | 不从 `hasVariableLenArg` 位或名字猜 CallKind；SOURCE 不重新映射/检查 extra。 |
| 真实 C ABI lowering / `compiler:codegen` 对应 owner | 按 fixed/extra 边界实际调用默认 promotions，再执行 CFFI ABI layout；固定签名与 extras 分开传入。 | 不用 normal CFIR implicit-integer-coercion helper替代 C ABI 默认 promotions。 |

最合理的树合同扩展是：成功 resolved ArgList 将“全部已完成 value 实参”与“到真实 formal 的映射”分开，并明确发布 E。完整列表由 mapper/completion 产品构造；不要仅给某个内部 impl 塞一个外部无法读取的字段。ordinary 调用依然符合全实参有formal的旧不变量；C ellipsis 是明确的另一种正常成功状态。

若使用统一 typed argument-role 产品，应明确 `Fixed(parameter)`、`CVariadicExtra` 和 context 角色；错误项另有错误语义。若采用单独 C-extras 接口，也必须保证两份视图共享同一组表达式身份、顺序和转换结果。两种表示选择不改变上述正式协议，不能同时保留多套相互推断的来源。

## 6. 必须实际接入的调用点与可直接实施的差异

1. **CfirMapArguments.mapCallableArguments**：在普通 Array 变参识别及 L259/288 数量分支之前识别 C ellipsis，完成 fixed 映射与 E 发布，并初始化完整 Outcome。新增协议必须被该 owner 实际调用，不能只定义 helper。CangjieVariadicUtils 对 C ellipsis 明确互斥，不复用 `_variadicParameter`。
2. **Candidate 初始化/替换**：`initializeArgumentMapping`、`updateArgumentMapping`、`replaceArgumentPrefix` 同步 canonical atoms 与 E；extra remap 根据完整 atom 身份，不能按 `argumentMapping.keys` 裁掉。跨模块抽象发布只读产品；有状态更新保留 Candidate 单一 owner。
3. **CfirCheckArguments.prepareExpectedType / resolveArgument**：typed E 获取 CType 目标并走 Processor，所有实参继续 normal constraint/completion。**ArgumentCheckingProcessor.checkApplicabilityForArgumentType**：native C-call Unit 禁令和 foreign-C-extra 的精确 autobox 例外均落在真实调用诊断路径，保留 fixed 的原检查。
4. **normal completer**：继续处理所有 Candidate arguments / postponed atoms。现有 [CfirCallCompleter.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt) L540–552 的 `addSameClassifierArgumentTypeConstraints` 是 formal 同构约束，不能把 E 放进它假装有formal；E 的 CType 约束来自实际 argument checking。涉及完整参数遍历的入口应消费 canonical argument-role 产品。
5. **results writer**：`computeAllArguments` 对 C 成功调用取完整已接受 atoms；`createArgumentsMapping` 为 E 注册 CType expected type；`handleVarargsAndReturnResultingArgumentsMapping` 不将 E 收集为 ArrayLit；`rewriteArgumentList` 转换每个实参一次，同时构造完整成功 ArgList 与 fixed mapping/E。不能用 `buildArgumentListForErrorCall` 表示合法 E，也不能依靠其 extra→null 规避成功 builder 合同。
6. **ArgList public API / 生命周期**：修改 `arguments`、`acceptChildren`、`transformArguments`、builder 与 source traversal 的完整产品协议。两处 builder 都要接入：[CfirAbstractArgumentList.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirAbstractArgumentList.kt) L285 起和 [CfirArgumentUtil.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/CfirArgumentUtil.kt) L29 起；保持表达式转换 identity cache。originalArgumentList 保留原语法/source，不能代替完成后的 operands。
7. **owned copy / mutable state**：[CfirOwnedGraphCopyVisitor.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyVisitor.kt) L28–68 目前只分配并复制 fixed/context mapping；[CfirArgumentListMutableState.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/expressions/CfirArgumentListMutableState.kt) 调用 concrete state owner。新增 canonical list/E 的分配、remap、复制和状态恢复必须一起更新，保证所有实参和来源引用完整。
8. **SOURCE producer**：CfirSourceGraphProducer L388 读取包含 E 的完成列表，L416 的 `hasVariableLenArg → VARIADIC` 分支必须改为真实 completed call-kind 合同。`appliedSignature` L424–440 继续用 fixed 签名加 C/变长位；不要追加 CType 或 extra 的 actual types 到参数列表。输出 Call 与 signature 是不同的有序载荷。
9. **arity consumers**：Outcome 的 arityDistance 和 [CfirCallResolver.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt) L794–822 的候选区间消费统一已发布协议：C ellipsis min=n、max无界。后者现在只按普通 `cangjieVariadicParameterForCall` 计算，不能继续另推数量模型。勿修改 overload 规则来隐藏 mapping/type 失败。
10. **ABI owner（能力边界）**：本库 [Cfir2ChirTypeMapper.kt](../../chir/cfir2chir/src/org/cangnova/cangjie/chir/cfir2chir/Cfir2ChirTypeMapper.kt) L119–122 当前明确拒绝 C function/变长函数类型；本轮在 compiler/codegen 和 cfir2chir 未找到 native `HandleVarargTypePromotion` 的实际 counterpart。SOURCE 修复可以在前端完整落地，但不能声称已经拥有可执行 C variadic ABI。未来启用该后端必须实现并实际调用 promotions/CFFI 链，不能删除这个拒绝检查后退化为普通 JVM 函数 lowering。

直接实施应从公共合同与 mapper 开始，连续接通 CheckArguments、completion、ArgList 生命周期和 SOURCE producer。此次无须改变正确 functionAbi.cj、制造额外声明参数、写 fixture 名字特判或改变 source/typeOrigin。定义接口后遗漏上述消费者，仍不是完整框架实现。

## 7. 主线程实施后的判定事实（本轮未执行）

本 fixture 必须能观察到：fixed mapping 三项，E 为 `[extra]`，E 类型检查目标 CType、完成 actual type Int32；完成 ArgList 顺序 `[first, middle, last, extra]`；复制后保持四个完成实参与真实 source；SOURCE Call kind DECLARED、arguments 四项、operands 五项，callee fixed types 按序 `[Int32, Int64, Int32]`、返回 Int32、C 与变长标志为 true。Int32 extra 的 ABI promotion 不变。

框架边界应同时检查零 extras、多个异质 CType extras、缺 fixed、非 CType、直接 Unit、`@C struct` extra、Int8/UInt8/Bool/Float32 promotions、普通非变参额外实参错误、Array 普通调用与实际语法糖、VArray 不获得 Array 语法糖，以及 error call 保留全 args。这些是后续实施验证条件，本报告没有新增测试或执行这些检查。

用于定位读取快照的 SHA-256：CfirMapArguments `DD99000CD618A6656BFA2561595C37CBA328920DCED0C0453C18F041D2434DB6`；Candidate `4368D8104450DF16CDB2AFF6E99A901B17BCFF4EB7359C626F47B266D1EC919A`；ArgumentCheckingProcessor `B75E5E28643E37EE5BB8D4CFDD506009CFBADC25A648E013222636338D1AFE2B`；CfirAbstractArgumentList `3FA77697B6F94DC0BD690A0F1F7B8319E0CC4D0608B6AFA7A15557EEC1F01E93`。后续并行修改后请以实际文件重新定位行号，不能将本轮只读结果用作实现通过证明。
