// RUN_PIPELINE_TILL: FRONTEND
// 泛型调用实参映射规则语义夹具：官方 `sema_parameters_and_arguments_mismatch` / `sema_ambiguous_arg_type`
// 两个 kind 的**可映射歧义**形态（多份类型映射存活 / 全部映射被实参检查淘汰），覆盖构造器调用、静态方法、
// 实例方法、`extend` 成员、类型别名宿主、嵌套泛型宿主、`Option<T>` / `Nothing` 形参、多类型形参等位置。
//
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
// `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
// （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断，见 DiagnosticEngine.cpp 的 CanBeEmitted）。
// **两版对本文件全部段的 kind、文案、MainHint.Range 完全一致**（逐段比对），因此不拆 LANGUAGE_VERSION 夹具。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**，官方报告点在本族使用单字符起点；本文件内联标记按 CFIR
// 定位策略标记最小完整名称 token，不能把官方单字符范围裁剪后直接用作 CFIR 期望。
// 内联标记写法：由"小于感叹号 + 诊断名 + 大于感叹号 + 锚点文本 + 结束标记"五段构成；全文只在 `// FILE:` 段内出现，
// 头部注释一律用文字描述，不出现该序列的字面量。
//
// 官方实现（external/cangjie_compiler，checkout 在 tag v1.0.0）：
//   A. 主路径 `CheckGenericCallCompatible`（src/Sema/TypeCheckCall.cpp:989-1050），唯一调用点 :1369-1372
//      （`if (IsGenericCall(ctx, candidate.ce, candidate.fd))`；非泛型调用走 :1373 的 `CheckCallCompatible`）：
//      :997 `GenerateTypeMappingForCall` 产出**多份** `MultiTypeSubst`；
//      :998 `FilterAndReduceSubstPacks`（:918-930）**丢掉仍含未解 tyVar 的映射**（`HasTyVarsToSolve` 为真者）；
//      :999-1001 过滤后为空 ⇒ 直接 `return false`，**不报本族任何 kind**（改由更早的推导失败诊断接手）；
//      :1007-1015 `ExpandMultiTypeSubst`（src/Sema/MultiTypeSubstUtils.cpp:250-261）把每份映射再展开成若干份
//      `SubstPack`，累积进 `resMappings`；
//      :1016-1038 逐份映射做实参检查（`CheckWithCache`），失败者 `resMappings.erase(mapping)` 淘汰；注意
//      :1030 `if (matched || resMappings.size() == 1) ds.ReportDiag();` —— 淘汰**最后一份**映射时仍会放行它此前
//      被 `DiagSuppressor` 压住的实参级诊断，所以本族常与实参级 mismatched types **同段共存**；
//      :1040 `ResolveTypeMappings`（:265-303）两两比较候选函数类型：`CompareParamTys`（:230-263）单向可赋值即淘汰
//      一份，**互不可赋值的两份都保留**；
//      :1041-1044 存活恰好 1 份 ⇒ 取该映射并 `CheckCandidateConstrains`，**无诊断**；
//      :1045 否则进入 `DiagnoseForMultiMapping`（:305-323）并 `return false`。
//   B. `DiagnoseForMultiMapping`（src/Sema/TypeCheckCall.cpp:305-323）三分支，**全部**是
//      `diag.Diagnose(ce, DiagKind::…)`（非 refactor），`ce` 是 CallExpr 节点，经 DiagnosticEngine.h:844-857
//      `Diagnose(const Node&, kind)` 退化成 `Diagnose(pos, kind)`，再由 :822-824 取 `pos..pos+1`：
//      **锚调用表达式首字符 1 个字符**——顶层 `f(...)` 锚函数名首字母；`Type.method(...)` 锚 `Type` 的首字母；
//      `recv.method(...)` 锚**接收者**首字母（不是方法名）；`Enum.Ctor(...)` 锚 `Enum` 的首字母。
//      :309-314 前置门控：实参里**已经**有类型 invalid 的项时**一条都不报**（更早的诊断已经报过）；
//      :316-317 `resMappings.empty()` ⇒ `sema_parameters_and_arguments_mismatch`（**映射全被淘汰**的形态）；
//      :318-319 `resMappings.size() > typeMappings.size()` ⇒ `sema_generic_ambiguous_method_match_in_upper_bounds`；
//      :320-321 否则 ⇒ `sema_ambiguous_arg_type`（**多份映射存活**的形态）。
//   C. `sema_generic_ambiguous_method_match_in_upper_bounds` 的 :318 分支是**死分支**：`ResolveTypeMappings`
//      只从 `matchMark[i] == true` 的下标里收集结果（:294-299），返回的 `resMappings` 是 `typeMappings` 的
//      **子序列**，`resMappings.size() > typeMappings.size()` 恒不成立。该 kind 的**活**报告点是
//      src/Sema/TypeCheckCall.cpp:2579 的 `GetErrorKindForCall`（:2560-2587），经
//      `IsSearchTargetInMemberAccessUpperBound`（:532-537，要求 `ce.baseFunc` 是 `isExposedAccess` 的
//      `MemberAccess`，该标志由 `GetMemberAccessExposedTarget` NameReferenceExpr.cpp:908 置位，入口是
//      NameReferenceExpr.cpp:687 / :1008，**只在接收者类型是 GenericsTy（类型形参 / 占位泛型）时**成立）。
//      本轮实测（探针 p73 / p74 / p76）用 `where T <: I1 & I2` + `t.pick(...)` 三种写法都先被
//      `sema_unable_to_infer_generic_func` 拦下，**到不了**该报告点。故本文件对该 kind 按 SUGGESTED 记录，
//      不写标记（见"本文件的 SUGGESTED 段"）。
//   D. 第二个 `sema_parameters_and_arguments_mismatch` 报告点 src/Sema/TypeCheckCall.cpp:2183-2199
//      （`CheckFuncPtrCall`，调用**函数类型值**的路径）：逐个 `Check(ctx, param, ce.args[i])` 之后，若
//      `ce.args[i]->expr` 仍 `HasIdealTy()`（实参类型在理想类型阶段没解开）且 `ce.ShouldDiagnose()` 为真，
//      报一次并 `return false`；随后 `ce.args[i]->ty = ce.args[i]->expr->ty`。实测（探针 p28 / p29 / p45 / p61 / p75）：
//      `var fp: (Int32) -> Int32 = gamOverload` / `fp(1)` / `gamFp(gamOverload)` / `gamFp(gamOverload<Int32>)`
//      全部先被 `sema_generic_type_without_type_argument` 或 `sema_mismatched_types` 拦下，**无一例到达** :2195
//      的独立报告点。本文件所有 `sema_parameters_and_arguments_mismatch` 都来自 (B) 的 :317 分支，头部逐段注明。
//   E. 文案：include/cangjie/Basic/DiagnosticSema.def:89 `sema_ambiguous_arg_type`
//      "ambiguous arguments type in call expression"；:94 `sema_parameters_and_arguments_mismatch`
//      "parameters and arguments mismatch"；:278 `sema_generic_ambiguous_method_match_in_upper_bounds`
//      "ambiguous method '%s' match for generic member access function call when searching in upper bounds"
//      （DiagKind 定义在 `ERROR(...)` 表内，是普通 kind，不是 refactor kind）。
//      伴随 kind：`sema_mismatched_types` "mismatched types"（实参级）、`sema_no_match_function_declaration_for_ref`
//      "no matching function declaration for function reference"（函数引用实参位置）。
//   F. v1.0.0 全树（`include/` + `src/`）grep 结果：`sema_cannot_infer_parameter_type` / `sema_no_value_for_parameter` /
//      `sema_too_many_arguments` **三个 kind 在 v1.0.0 里根本不存在**（def 表里没有、src 里也没有），不是"死条目"
//      而是尚未引入的诊断名，本文件不收。`sema_cannot_have_parameter`（def:152 "%s cannot have parameter"）存在，
//      但它的两个报告点 src/Sema/TypeCheckDecl.cpp:369（getter）与 src/Sema/TypeChecker.cpp:1675（finalizer）
//      都是**声明级**检查，与本文件的调用实参映射族正交，本文件不收。
//
// 本项目 CFIR 名及文案定义见 `CfirDiagnosticsList.kt` 与 `CfirErrorsDefaultMessages.kt`：
//   PARAMETERS_AND_ARGUMENTS_MISMATCH ← 官方 sema_parameters_and_arguments_mismatch；
//   AMBIGUOUS_ARG_TYPE ← 官方 sema_ambiguous_arg_type；
//   NO_MATCH_FUNCTION_DECLARATION_FOR_REF ← 官方 sema_no_match_function_declaration_for_ref；
//   TYPE_MISMATCH ← 官方 sema_mismatched_types。
//
// CFIR 生产者与锚点：
//   AMBIGUOUS_ARG_TYPE 由 `coneDiagnosticToCfirDiagnostic.kt` 的 `AmbiguousArgumentType` 分支映射。该诊断工厂使用
//   `REFERENCED_NAME_BY_QUALIFIED`；映射层把完整调用 source 交给 PSI / LightTree 策略，范围落在被调用的名称 token 上。
//   构造器 callee 也通过该策略取其引用名，不取包含类型实参的整个类型引用。
//   PARAMETERS_AND_ARGUMENTS_MISMATCH 由同一映射器的 `parametersAndArgumentsMismatchDiagnostic` 统一产生：
//     (a) GET 下标脱糖调用存在字面量类型错误时，source 是接收者，策略标记接收者的完整名称 token；
//     (b) 泛型映射中函数引用实参使候选失效时，source 是完整调用，策略标记被调用的完整名称 token。该路径覆盖
//         `ArgumentTypeMismatch`（函数类型实参与非函数形参不兼容）及结构化 callable-reference no-match；后者把
//         外层候选随 `ConeNoMatchingFunctionReferenceError` 传到映射器，以保留调用映射语义。
//   NO_MATCH_FUNCTION_DECLARATION_FOR_REF 使用相同的完整名称定位策略，锚函数引用实参的完整名称 token。
//   不通过 offset-only source 构造或截短 token。上述策略在 PSI 与 LightTree 下共用同一名称 token 语义。
//
// CFIR 定向输出（LightTree、PSI、WithoutAliasExpansion 三个入口的目标用例均已实际运行）：
//   (1) 形态 1–12 的 AMBIGUOUS_ARG_TYPE 均报告在完整被调用名上：顶层函数名、构造器名、静态/实例方法名、
//       enum 构造器名分别由引用策略从完整调用 source 中选出；不再按官方 CallExpr 首字符放置。
//   (2) 形态 13 的 expected type 是 `Int64`、actual type 是函数类型，CFIR 同时报告实参 TYPE_MISMATCH 和
//       调用级 PARAMETERS_AND_ARGUMENTS_MISMATCH，后者锚在完整 `GAMBox13` token。
//   (3) 形态 14 的函数引用 no-match 与外层泛型构造调用级 mismatch 并存；两者分别锚在完整 `gamS14Height` 与
//       `GAMBox14` token。它不产生 TYPE_MISMATCH。
//   (4) 形态 15 / 16 覆盖静态 / 实例泛型方法；两者的调用级 mismatch 都锚在完整 `build` token。
//   (5) 形态 13–16 的实参级诊断均锚在完整实参标识符上。
//   官方 CJC 仍在这些调用诊断的起点给出单字符 MainHint；该信息用于语义取证，不决定 CFIR 标记范围。
//
// 已有覆盖（testData 下已 grep 确认）：
//   - PARAMETERS_AND_ARGUMENTS_MISMATCH：既有 LLT 覆盖下标脱糖与函数类型构造器；本文件新增泛型构造器、静态方法
//     与实例方法三类映射失效情形。既有两处分别是：
//     llt/desugar_error_report/subscribe_in_binary.cj（`b[1.01]`，脱糖下标 → CFIR 路径 (a)）与
//     llt/generics/class5_test.cj（`Data<(Int64) -> Int64>(getHeight)`，构造器 + 函数类型实参 + 实参级 TYPE_MISMATCH
//     干扰）。形态 13–16 另覆盖函数类型不匹配、函数引用 no-match、静态方法与实例方法。
//   - AMBIGUOUS_ARG_TYPE：既有 LLT 只有 llt/enum/enum11_3.cj:17 一处（enum 构造器形态，且同段还有
//     GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT 干扰）；本文件新增 diagnostics2 覆盖。形态 1–12 覆盖
//     顶层函数、多类型形参、`Option<T>` / `Nothing` 形参、类型实参推导、构造器、静态方法、类型别名与嵌套宿主、
//     `extend` 成员及类方法内的调用。
//   - `WRONG_NUMBER_OF_ARGUMENTS` ×2（arityMismatch.cj）、`NEED_NAMED_ARGUMENT` / `UNKNOWN_NAMED_ARGUMENT` /
//     `NAMED_ARGUMENTS_NOT_ALLOWED` / `MIXING_NAMED_AND_POSITIONAL_ARGUMENTS` / `ARGUMENT_PASSED_TWICE`
//     （call/namedArgumentsAndArity.cj 与 calls/namedArgumentsAndArity.cj）、`AMBIGUOUS_FUNCTION_CALL`
//     （calls/ambiguousFunctionCall.cj）、`ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED` /
//     `MISMATCHED_TYPES_MULTIPLE_ASSIGN`（inference/arrayLiteralAndMultipleAssign.cj）：属别的族，本文件不收。
//   - 泛型推导失败（`UNABLE_TO_INFER_GENERIC_FUNC`）、泛型类型缺类型实参（`GENERIC_TYPE_WITHOUT_TYPE_ARGUMENT` /
//     `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`）、重载歧义（`AMBIGUOUS_MATCH` / `AMBIGUOUS_USE`）、
//     类型上界访问（`GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS` / `GENERIC_AMBIGUOUS_METHOD_MATCH_IN_UPPER_BOUNDS`）
//     属别的族；本轮探针确认多上界 / 上界歧义这类输入**先**被其中之一拦下，本文件不收（见下方"已排除的形态"）。
//
// 本文件的 SUGGESTED 段共 1 个：形态 C（`sema_generic_ambiguous_method_match_in_upper_bounds`，见 (C)）。
// 该 kind 没有 CFIR 名（`CfirDiagnosticsList.kt` 里不存在 `GENERIC_AMBIGUOUS_METHOD_MATCH_IN_UPPER_BOUNDS`），
// 且本轮两版 cjc 都构造不出可达输入，所以不写段、不写标记，只在此登记。其余 18 段的 kind 与伴随诊断均经两版 cjc 核实；
// CFIR 期望标记按完整名称 token 定位，不照搬官方的单字符 MainHint。

// ── 形态 1：顶层泛型函数，形参是裸类型形参 `T`，实参是"显式类型实参后仍对应两个重载"的泛型函数引用
// kind：sema_ambiguous_arg_type "ambiguous arguments type in call expression"（普通 kind，
// `diag.Diagnose(ce, …)` → CallExpr 首字符 1 个字符，TypeCheckCall.cpp:320-321）；Range (14,13)..(14,14)，1.0.5 / 1.1.3 一致。
// 源码依据：`gamS1Overload<Int32>` 同时匹配 2 形参与 1 参两个重载 ⇒ 实参类型有**两份**互不可赋值的候选
// ⇒ `GenerateTypeMappingForCall` 产出 2 份 `MultiTypeSubst`，`ResolveTypeMappings` 的 `CompareParamTys`
// 两边互不可赋值 ⇒ 两份都保留 ⇒ `resMappings.size() != 1` ⇒ `sema_ambiguous_arg_type`。
// 探针 p32 / p83 / p84（p84 把外层也写成显式类型实参后歧义消失，零本族诊断）。
// FILE: genericAmbiguousArgTypePlainFunction.cj
func gamS1Overload<V>(x: V, z: V): V {
    return x
}

func gamS1Overload<V>(x: V): V {
    return x
}

func gamS1Take<T>(f: T): Int32 {
    return 0
}

func gamMainS1(): Int64 {
    let r = <!AMBIGUOUS_ARG_TYPE!>gamS1Take<!>(gamS1Overload<Int32>)
    return r
}
// ── 形态 2：两个类型形参 `<A, B>`，只有 `A` 被歧义实参约束、`B` 由第二个普通实参确定 —— 仍是两份存活映射
// 官方 CJC MainHint：`gamS2Pair` 起点单字符，Range (14,13)..(14,14)。CFIR 标记完整方法名 token。探针 p41。
// FILE: genericAmbiguousArgTypeTwoTypeParameters.cj
func gamS2Overload<V>(x: V, z: V): V {
    return x
}

func gamS2Overload<V>(x: V): V {
    return x
}

func gamS2Pair<A, B>(a: A, b: B): A {
    return a
}

func gamMainS2(): Int64 {
    let r = <!AMBIGUOUS_ARG_TYPE!>gamS2Pair<!>(gamS2Overload<Int32>, 1)
    return r
}
// ── 形态 3：同一个类型形参 `T` 出现在**两个**形参上，两个实参都是同一份歧义引用（映射候选数相同，仍不唯一）
// 官方 CJC MainHint：`gamS3Take` 起点单字符，Range (14,13)..(14,14)。CFIR 标记完整函数名 token。探针 p50。
// FILE: genericAmbiguousArgTypeSharedTypeParameter.cj
func gamS3Overload<V>(x: V, z: V): V {
    return x
}

func gamS3Overload<V>(x: V): V {
    return x
}

func gamS3Take<T>(f: T, g: T): Int32 {
    return 0
}

func gamMainS3(): Int64 {
    let r = <!AMBIGUOUS_ARG_TYPE!>gamS3Take<!>(gamS3Overload<Int32>, gamS3Overload<Int32>)
    return r
}
// ── 形态 4：形参类型是 `Option<T>`（`T` 藏在脱糖后的枚举里），实参是歧义泛型函数引用
// 官方 CJC MainHint：`gamS4Opt` 起点单字符，Range (17,13)..(17,14)。CFIR 标记完整函数名 token。探针 p46。
// 与 LLT enum11_3 的差别：那边歧义来自**枚举构造器**的 `T`，本段歧义来自**普通函数形参**的 `T`，
// 调用宿主与 `ce` 的起点都不同。`Option` 由 `?T` 脱糖 / 显式 `Option<T>` 给出，不改锚点。
// FILE: genericAmbiguousArgTypeOptionParameter.cj
func gamS4Overload<V>(x: V, z: V): V {
    return x
}

func gamS4Overload<V>(x: V): V {
    return x
}

func gamS4Opt<T>(o: Option<T>): Int32 {
    match (o) {
        case Some(_) => return 1
        case None => return 0
    }
}

func gamMainS4(): Int64 {
    let r = <!AMBIGUOUS_ARG_TYPE!>gamS4Opt<!>(gamS4Overload<Int32>)
    return r
}
// ── 形态 5：形参表里有一个 `Nothing` 形参（`b: Nothing`）——`Nothing` 是**底类型**、不贡献类型映射，
// 歧义仍然只由 `a: T` 决定；实参第二项是 `throw` 表达式
// 官方 CJC MainHint：`gamS5Nothing` 起点单字符，Range (14,13)..(14,14)。CFIR 标记完整函数名 token。探针 p26 / p82。
// 对照探针 p09（实参换成普通值 `1`）零诊断，证明歧义源与 `Nothing` 形参本身无关。
// FILE: genericAmbiguousArgTypeNothingParameter.cj
func gamS5Overload<V>(x: V, z: V): V {
    return x
}

func gamS5Overload<V>(x: V): V {
    return x
}

func gamS5Nothing<T>(a: T, b: Nothing): T {
    return a
}

func gamMainS5(): Int64 {
    let r = <!AMBIGUOUS_ARG_TYPE!>gamS5Nothing<!>(gamS5Overload<Int32>, throw Exception())
    return r
}
// ── 形态 6：**类型实参由实参推导**（没有显式 `<…>`）的泛型类构造器 —— 推导出的 `T` 有两份候选
// 官方 CJC MainHint：构造器名起点单字符，Range (17,13)..(17,14)。CFIR 标记完整构造器名 token。探针 p57。
// 与形态 13（显式类型实参 + 映射全被淘汰 ⇒ PARAMETERS_AND_ARGUMENTS_MISMATCH）构成"有无显式类型实参"的对照。
// FILE: genericAmbiguousArgTypeInferredTypeArgument.cj
class GAMBox6<T> {
    let value: T
    init(x: T) {
        this.value = x
    }
}

func gamS6Overload<V>(x: V, z: V): V {
    return x
}

func gamS6Overload<V>(x: V): V {
    return x
}

func gamMainS6(): Int64 {
    let b = <!AMBIGUOUS_ARG_TYPE!>GAMBox6<!>(gamS6Overload<Int32>)
    return b.value(1)
}
// ── 形态 7：`Type.method(...)` 形态 —— 泛型类的**静态**泛型方法，实参歧义；官方 MainHint 起于类型名，CFIR 标记 `build`
// kind：sema_ambiguous_arg_type；Range (17,13)..(17,14)。探针 p03。
// FILE: genericAmbiguousArgTypeStaticMethod.cj
class GAMHolder7<T> {
    static func build<U>(u: U): GAMHolder7<T> {
        return GAMHolder7<T>()
    }
    init() {}
}

func gamS7Overload<V>(x: V, z: V): V {
    return x
}

func gamS7Overload<V>(x: V): V {
    return x
}

func gamMainS7(): Int64 {
    let h = GAMHolder7<Int64>.<!AMBIGUOUS_ARG_TYPE!>build<!>(gamS7Overload<Int32>)
    return 0
}
// ── 形态 8：**类型别名**作为泛型宿主（`GAMAlias8` = 泛型类实例化），再走静态方法调用；官方 MainHint 起于别名，CFIR 标记 `build`
// kind：sema_ambiguous_arg_type；Range (19,13)..(19,14)。探针 p43。
// 别名先在官方 `CheckTypeAlias`（src/Sema/TypeCheckType.cpp）合成，`ce` 的起点是别名引用而非类名。
// FILE: genericAmbiguousArgTypeAliasHost.cj
class GAMHolder8<T> {
    static func build<U>(u: U): GAMHolder8<T> {
        return GAMHolder8<T>()
    }
    init() {}
}

type GAMAlias8 = GAMHolder8<Int64>

func gamS8Overload<V>(x: V, z: V): V {
    return x
}

func gamS8Overload<V>(x: V): V {
    return x
}

func gamMainS8(): Int64 {
    let h = GAMAlias8.<!AMBIGUOUS_ARG_TYPE!>build<!>(gamS8Overload<Int32>)
    return 0
}
// ── 形态 9：**嵌套泛型**宿主（类型实参本身是泛型类实例化）+ 静态方法；官方 MainHint 起于类型名，CFIR 标记 `build`
// kind：sema_ambiguous_arg_type；Range (17,13)..(17,14)。探针 p60。
// 对照探针 p49 / p31（嵌套宿主但构造器调用、实参不歧义）零本族诊断。
// FILE: genericAmbiguousArgTypeNestedGenericHost.cj
class GAMBox9<T> {
    static func build<U>(u: U): GAMBox9<T> {
        return GAMBox9<T>()
    }
    init() {}
}

func gamS9Overload<V>(x: V, z: V): V {
    return x
}

func gamS9Overload<V>(x: V): V {
    return x
}

func gamMainS9(): Int64 {
    let b = GAMBox9<GAMBox9<Int64>>.<!AMBIGUOUS_ARG_TYPE!>build<!>(gamS9Overload<Int32>)
    return 0
}
// ── 形态 10：`extend` 块里定义的泛型方法，调用点是**实例接收者**；官方 MainHint 起于接收者，CFIR 标记 `pick`
// kind：sema_ambiguous_arg_type；Range (19,13)..(19,14)。探针 p42。
// 对照探针 p06（`b.pick(1)` 实参不歧义）零诊断；探针 p71（`extend` 在调用点之后声明）同样只报本条。
// FILE: genericAmbiguousArgTypeExtendMember.cj
class GAMBase10<T> {}

extend GAMBase10<Int64> {
    func pick<V>(a: V): V {
        return a
    }
}

func gamS10Overload<V>(x: V, z: V): V {
    return x
}

func gamS10Overload<V>(x: V): V {
    return x
}

func gamMainS10(): Int64 {
    let b = GAMBase10<Int64>()
    let r = b.<!AMBIGUOUS_ARG_TYPE!>pick<!>(gamS10Overload<Int32>)
    return r
}
// ── 形态 11：调用点**在另一个类的成员函数体内**（不是顶层函数），形参类型形参 `T`、实参歧义；CFIR 标记完整函数名
// kind：sema_ambiguous_arg_type；Range (15,17)..(15,18)（缩进 8 空格 + `let r = `，故列号 17 而非 13）。
// 探针 p51。目的是钉住"锚点跟着调用表达式的词法起点走、与所在函数无关"这一点。
// FILE: genericAmbiguousArgTypeInsideClassMember.cj
func gamS11Overload<V>(x: V, z: V): V {
    return x
}

func gamS11Overload<V>(x: V): V {
    return x
}

func gamS11Take<T>(f: T): Int32 {
    return 0
}

class GAMUser11 {
    func use(): Int32 {
        let r = <!AMBIGUOUS_ARG_TYPE!>gamS11Take<!>(gamS11Overload<Int32>)
        return r
    }
}

func gamMainS11(): Int32 {
    let u = GAMUser11()
    return u.use()
}
// ── 形态 12：`Enum.Ctor(...)` 形态 —— 泛型枚举的构造器，实参是歧义泛型函数引用；官方 MainHint 起于枚举类型名，CFIR 标记 `Day`
// kind：sema_ambiguous_arg_type；Range (15,13)..(15,14)。探针 p23。
// 本段与 LLT llt/enum/enum11_3.cj:17 属同一构造族，但那边枚举只有一个构造器、没有显式类型实参，
// 且同段被 GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT 干扰；本段在 diagnostics2 下单独钉住锚点。
// FILE: genericAmbiguousArgTypeEnumConstructor.cj
enum GAMTime12<T> {
    | Day(T)
    | Night
}

func gamS12Overload<V>(x: V, z: V): V {
    return x
}

func gamS12Overload<V>(x: V): V {
    return x
}

func gamMainS12(): Int64 {
    let t = GAMTime12.<!AMBIGUOUS_ARG_TYPE!>Day<!>(gamS12Overload<Int32>)
    match (t) {
        case Day(x) => return 0
        case _ => return 1
    }
}
// ── 形态 13：泛型类构造器 + **显式类型实参**，但实参（裸函数引用）与该类型实参不符
// ⇒ 唯一一份映射被 `CheckWithCache` 淘汰 ⇒ `resMappings` 变空 ⇒ `DiagnoseForMultiMapping` 的 :317 分支。
// 两条官方诊断：sema_parameters_and_arguments_mismatch 的 MainHint 是调用起点单字符 (13,13)..(13,14)；
//   sema_mismatched_types 锚**整个函数引用实参** `gamS13Height` (13,29)..(13,41)，由 :1030 的 `ds.ReportDiag()` 在淘汰
//   最后一份映射时放行。CFIR 分别锚完整 `GAMBox13` 与完整实参名。探针 p30 / p15。
// CFIR 名：PARAMETERS_AND_ARGUMENTS_MISMATCH + TYPE_MISMATCH。
// FILE: genericParametersMismatchConstructorExplicitTypeArgument.cj
class GAMBox13<T> {
    let value: T
    init(x: T) {
        this.value = x
    }
}

func gamS13Height(h: Int64): Int64 {
    return h * 2
}

func gamMainS13(): Int64 {
    let b = <!PARAMETERS_AND_ARGUMENTS_MISMATCH!>GAMBox13<!><Int64>(<!TYPE_MISMATCH!>gamS13Height<!>)
    return b.value
}
// ── 形态 14：同上，但**显式类型实参是函数类型** `(Int32) -> Int32`，与实参 `gamS14Height: (Int64) -> Int64` 不符
// 两条官方诊断：sema_parameters_and_arguments_mismatch 的 MainHint 是调用起点单字符 (13,13)..(13,14)；
//   sema_no_match_function_declaration_for_ref 的 MainHint 是实参起点单字符 (13,40)..(13,41)——
//   这里**没有** sema_mismatched_types：函数引用本身先在 `Check` 里匹配不到声明，诊断由函数引用路径给出。
// CFIR 锚完整 `GAMBox14` 与 `gamS14Height` 名称 token。探针 p75。
// FILE: genericParametersMismatchConstructorFunctionTypeArgument.cj
class GAMBox14<T> {
    let value: T
    init(x: T) {
        this.value = x
    }
}

func gamS14Height(h: Int64): Int64 {
    return h * 2
}

func gamMainS14(): Int64 {
    let b = <!PARAMETERS_AND_ARGUMENTS_MISMATCH!>GAMBox14<!><(Int32) -> Int32>(<!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>gamS14Height<!>)
    return 0
}
// ── 形态 15：泛型类的**静态**泛型方法 + 显式类型实参 `<Int64>`，实参函数引用类型不符 ⇒ 映射全被淘汰
// 两条官方诊断：sema_parameters_and_arguments_mismatch 的 MainHint 是调用起点单字符 (13,13)..(13,14)；
//   sema_mismatched_types 锚**整个函数引用实参** `gamS15Height` (13,45)..(13,57)。
// 与形态 13 的差别是宿主：静态方法（`Type.method<T>(...)`）而不是构造器；CFIR 调用级诊断锚完整 `build`。探针 p77。
// FILE: genericParametersMismatchStaticMethodExplicitTypeArgument.cj
class GAMHolder15<T> {
    static func build<U>(u: U): GAMHolder15<T> {
        return GAMHolder15<T>()
    }
    init() {}
}

func gamS15Height(h: Int64): Int64 {
    return h * 2
}

func gamMainS15(): Int64 {
    let h = GAMHolder15<Int64>.<!PARAMETERS_AND_ARGUMENTS_MISMATCH!>build<!><Int64>(<!TYPE_MISMATCH!>gamS15Height<!>)
    return 0
}
// ── 形态 16：泛型类的**实例**泛型方法 + 显式类型实参 `<Int64>`，实参函数引用类型不符 ⇒ 映射全被淘汰
// 两条官方诊断：sema_parameters_and_arguments_mismatch 的 MainHint 是接收者起点单字符 `h` (14,13)..(14,14)；
//   sema_mismatched_types 锚整个实参 `gamS16Height` (14,28)..(14,40)。
// CFIR 对本调用通过引用策略锚完整方法名 `build`；与形态 10（`extend` 成员且映射存活）成对照。探针 p78。
// FILE: genericParametersMismatchInstanceMethodExplicitTypeArgument.cj
class GAMHolder16<T> {
    func build<U>(u: U): U {
        return u
    }
    init() {}
}

func gamS16Height(h: Int64): Int64 {
    return h * 2
}

func gamMainS16(): Int64 {
    let h = GAMHolder16<Int64>()
    let r = h.<!PARAMETERS_AND_ARGUMENTS_MISMATCH!>build<!><Int64>(<!TYPE_MISMATCH!>gamS16Height<!>)
    return r
}
// ── 形态 17：合法对照 L1 —— 泛型类构造器三种类型实参写法（显式标量 / 由实参推导 / 显式函数类型）全部零诊断
// 源码依据：`resMappings.size() == 1` ⇒ :1041-1044 直接取映射并返回，**不进入** `DiagnoseForMultiMapping`。
// 1.0.5 / 1.1.3 两版 JSON 为空。探针 p81（与形态 6 / 13 / 14 同宿主的正反对照）。
// FILE: legalGenericCallTypeArgumentResolution.cj
class GAMBox17<T> {
    let value: T
    init(x: T) {
        this.value = x
    }
}

func gamS17Height(h: Int64): Int64 {
    return h * 2
}

func gamMainS17(): Int64 {
    let a = GAMBox17<Int64>(1)
    let b = GAMBox17(gamS17Height)
    let c = GAMBox17<(Int64) -> Int64>(gamS17Height)
    return a.value + b.value(1) + c.value(1)
}
// ── 形态 18：合法对照 L2 —— 给外层泛型调用补上**显式类型实参**后歧义消解，零诊断
// 源码依据：显式类型实参把 `T` 钉死成 `(Int32) -> Int32`，`gamS18Overload<Int32>` 据此唯一选中 1 参重载
// ⇒ 只剩一份映射 ⇒ :1041 早退。探针 p83 / p84（对照形态 1 的同一段只差 `<(Int32) -> Int32>`）。
// 1.0.5 / 1.1.3 两版 JSON 为空（探针 p83 的 `let s = gamTake(gamOverload)` 那行会额外报
// sema_generic_type_without_type_argument，本段按官方事实不收该写法）。
// 与形态 1 放在相邻文件便于 diff 阅读，但**是两个独立段、分别编译**。
// FILE: legalExplicitTypeArgumentDisambiguates.cj
func gamS18Overload<V>(x: V, z: V): V {
    return x
}

func gamS18Overload<V>(x: V): V {
    return x
}

func gamS18Take<T>(f: T): Int64 {
    return 0
}

func gamMainS18(): Int64 {
    let r = gamS18Take<(Int32) -> Int32>(gamS18Overload<Int32>)
    return r
}
//
// 已排除的形态（取证 kind 与 Range 见下；1.0.5 与 1.1.3 两版一致，除注明外；探针编号见正文各处引用）：
//   1. **接口多继承 + 调用继承来的同名泛型方法**（`class C<T> <: I1<T> & I2<T>`，I1/I2 都声明 `pick<V>(a: V): V`）：
//      本轮试了四种写法，**全部**到不了本族——
//      (a) 具体类不实现（p01 / p05 / p14）⇒ `sema_class_need_abstract_modifier_or_func_need_impl`；
//      (b) 抽象类（p20 / p59）⇒ 调用点 `sema_abstract_class_can_not_be_instantiated`；
//      (c) 接口给默认实现（p21 / p33）⇒ 调用点 `sema_ambiguous_match`（锚方法名 `pick`，非 `ce` 起点）+ 类级
//          `sema_interface_member_must_be_implemented`；
//      (d) `extend` 补实现（p52）⇒ `sema_ambiguous_match` + `sema_weak_visibility` + 类级缺实现。
//      结论：多上界同名方法由**候选重载层**（`sema_ambiguous_match`）拒绝，`DiagnoseForMultiMapping` 在这条路上不可达。
//      本文件改用形态 6–12 的"多份类型映射"形态覆盖同一诊断。
//   2. `sema_generic_ambiguous_method_match_in_upper_bounds`：见 (C)，:318 分支是死分支，活报告点要求接收者
//      是 GenericsTy，探针 p73 / p74 / p76 都被 `sema_unable_to_infer_generic_func` 先拦下。无 CFIR 名，SUGGESTED。
//   3. `:2195` 的"实参类型推不出来"入口（`CheckFuncPtrCall`）：见 (D)，五种函数类型值调用写法都被更早的诊断拦下。
//   4. lambda 实参：泛型调用 `gamTake<T>(f: T)` 收 `{ x => x + 1 }` ⇒ `sema_lambdaExpr_must_have_type_annotation`
//      （锚 lambda 首字符，探针 p44）；补上参数类型标注后调用本身合法（探针 p64，只剩返回类型失配的干扰项）。
//      所以 lambda 实参在本族上没有可达形态。
//   5. `Nothing` 形参的合法形态（探针 p09 `gamNothing(1, throw Exception())`）：零诊断，说明形态 5 的歧义源
//      确实只在 `a: T` 上，不是 `Nothing` 带来的。
//   6. 形状更简单的"构造函数映射全被淘汰"：`init(x: T)` 但**不带**显式类型实参（`GAMBox(gamS13Height)`）时
//      `T` 由实参直接推导成功，映射存活 ⇒ 零本族诊断；显式类型实参才是让映射被淘汰的**必要条件**（形态 13）。
//   7. `GAMBox<Int64>(gamS15Height)` 里若构造器形参类型**不是** `T` 而是无关类型（探针 p55）⇒ 只有
//      `sema_mismatched_types`（两条），**没有** `sema_parameters_and_arguments_mismatch`——因为此时
//      `IsGenericCall` 为假、走 `CheckCallCompatible` 而非 `CheckGenericCallCompatible`。故形态 13–16 的
//      构造器/方法形参类型都必须是泛型类/方法的类型形参。
//   8. 不可赋值性方向：`CompareParamTys`（TypeCheckCall.cpp:230-263）对两个都是数值的形参用
//      `CompareIntAndFloat` 判 `>` 就直接返回 false（:250-255），所以形参类型都是数值时两份映射也会互相淘汰。
//      但本文件所有歧义来源的形参都是单个裸类型形参 `T`（元数 1），走不到这条数值分支。
