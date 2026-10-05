// RUN_PIPELINE_TILL: FRONTEND
//
// 【取证方式】官方 cjc 1.0.5 与本机 cjc 1.1.3 两版实测，静态库模式
// `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
// （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断）。1.1.3 在 CHIR / codegen 阶段可能 LLVM 崩
// （崩在语义检查之后），语义诊断 JSON 仍完整产出，本文件按语义诊断写期望。除特别注明外两版 kind、
// 消息、Range 完全一致。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符
// 对齐官方范围。
//
// 【官方实现】（external/cangjie_compiler，checkout 在 tag v1.0.0）
//   - src/Sema/TypeCheckExpr/BinaryExpr.cpp:1085-1106 IsCoalescingLeftTyValid：左操作数类型必须同时满足
//     ① Ty::IsTyCorrect ② realTy->IsEnum() ③ typeArgs.size() == 1 ④ enumTy->declPtr->fullPackageName ==
//     core 包名 ⑤ identifier == "Option"；占位类型（IsPlaceholder）先按 core 的 Option 构造器做
//     ConstrainByCtor（:1089-1092）再判。任一条不满足即“不是 Option”。
//   - :1108-1142 ChkCoalescingExpr。报告点 :1117-1119：
//     `if (!CanSkipDiag(*be.leftExpr)) diag.Diagnose(*be.leftExpr, DiagKind::sema_invalid_coalescing)`。
//     Diagnose(node, kind) 对非 refactor kind 退化为 GetBegin 一个字符
//     （include/cangjie/Basic/DiagnosticEngine.h:844-857 → :822-825 Diagnose(pos) → pos+1），
//     所以锚点是**左操作数表达式的第一个字符**（含左括号；成员访问时是基表达式的首字符，不是成员名）。
//     CanSkipDiag（src/Sema/TypeCheckUtil.cpp:230-233）= `!Ty::IsTyCorrect(node.ty)`：左操作数自身类型
//     已经是 invalid 时不再报本诊断（见文件末“已删除的形态”）。左操作数非法时直接 return，
//     **不再检查右操作数**，右操作数类型错误不会级联出现。
//   - :1127-1133 声明的目标类型比元素类型窄 → DiagMismatchedTypesWithFoundTy(diag, *be.leftExpr, ...)，
//     锚**左操作数**（本文件按官方写 TYPE_MISMATCH）。
//   - :1135-1140 右操作数在 realTgtTy（缺省 = 左 Option 的元素类型）下检查失败 → DiagMismatchedTypes(
//     diag, *be.rightExpr, *realTgtTy)（src/Sema/Diags.cpp:248-260 → :196-205 DiagnoseRefactor(node)，
//     锚**整个右操作数表达式**）；字面量右操作数由 CheckWithNegCache 先报 sema_cannot_convert_literal
//     （1 字符；浮点字面量只锚首字符），此时 Coalescing 路径不再追加 mismatched types
//     （Diags.cpp:250-252 在 node.ty 已 invalid 时提前返回）。
//   - 入口：:583-584 Check 路径 / :966-967 Syn 路径。类型检查之后才 desugar 成 match
//     （src/Sema/Desugar/AfterTypeCheck/Coalescing.cpp:115-141：`case Some(x) => x` / `case $None => 右`）。
//   - 结合性：src/Parse/ParseExpr.cpp:539-543，同优先级时只有 `??`（与 `**`）走**右结合**。
//     `??=` 不是 token（src/Lex/LexerImpl.h:161 只合并 QUEST+QUEST → COALESCING，ParserImpl.h:275 同）。
//   - 文案 include/cangjie/Basic/DiagnosticSema.def:40-41（sema_invalid_coalescing）。
//
// 【本项目 CFIR 诊断名与生产者】
//   - cfir/checkers/checkers-component-generator/src/.../diagnostics/CfirDiagnosticsList.kt:1685
//     INVALID_COALESCING（生产者 cfir/checkers/src/.../checkers/expression/CfirCoalescingTypeMismatchChecker.kt:35-41，
//     报在 expression.left.source）；:889 TYPE_MISMATCH（同文件 :43-49 右操作数非字面量）；
//     :981 CANNOT_CONVERT_LITERAL（同文件 :43-49 右操作数字面量）。
//   - 生成物 cfir/checkers/gen/.../diagnostics/CfirErrors.kt:421 / :255 / :267；
//     文案表 CfirErrorsDefaultMessages.kt:1058。官方 ↔ CFIR 名对照
//     cfir/analysis-tests/testFixtures/.../golden/DiagnosticNameMapper.kt:247 / :38 / :40。
//   【按源码可预判的锚点差异（未跑 CFIR）】左操作数锚点官方是 GetBegin 1 字符，CFIR 报在整个左操作数元素上：
//     标识符 / 调用 / 下标 / 字面量 / 括号表达式的起点与官方一致；**成员访问**（`b.v`、`g.coalM()`）官方锚
//     基表达式首字符 1 字符，CFIR 元素可能覆盖 `b.v` 整段。右操作数 TYPE_MISMATCH 官方是整段右操作数
//     （`"x"` 3 字符），CFIR 报 expression.right.source，一致；CANNOT_CONVERT_LITERAL 官方 1 字符，
//     CFIR 报在字面量元素上。`let _: Int8 = o ?? 0` 的 TYPE_MISMATCH 按官方锚左操作数 `o`，CFIR 侧没有
//     coalescing 专用生产者（由通用类型不匹配路径承载），实际锚点以实跑为准。
//
// 【已有覆盖不重复】testData/diagnostics/constructor/interopMutabilityProbe.cj（P3 互操作/可变性回归语料，
//   已有 INVALID_COALESCING 标在 `i ?? 1` 的 `i` 上，本文件不再重复该最小形态）；
//   testData/llt/ErrMsgs/coalescing_0.cj / coalescing_1.cj（右操作数的 CANNOT_CONVERT_LITERAL /
//   NO_MATCH_OPERATOR_FUNCTION_CALL / INVALID_BINARY_OPERATOR 已在这些用例里内联标记，本文件只写
//   Coalescing 检查器自己的两个报告点，不重复那些用例）；testData/llt/box/option_box_*.cj、
//   llt/enum/enum39_coalescing_box.cj、llt/enum/enum27.cj、llt/binary/binaryexpr_and_funcargoverload.cj、
//   llt/binary/binary_error_report_07.cj、llt/array/array_access2.cj（`??` 合法形态）；
//   testData/diagnostics2/syntax/recovery/coalescingAssign.cj（rawBuilder，`??=` 的 parse 恢复形态）。
//   泛型约束族（GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT 等）在 llt 已大量覆盖，本文件不涉及。
//
// 【两版差异】全部形态 1.0.5 与 1.1.3 的 kind、消息、Range 完全一致，未拆 LANGUAGE_VERSION。

// 形态 1：左操作数是标量 / String / Bool / Unit 变量（都不是 Option）。
// kind：sema_invalid_coalescing（CFIR INVALID_COALESCING）
// 锚点：左操作数表达式的 GetBegin 1 字符（i / s / b / u），官方 Diagnose(node, kind) 退化为 pos+1。
// 1.0.5 / 1.1.3 一致：4 条，Range 均为 (行,13)..(行,14)。
// FILE: coalLeftScalar.cj
func coalLeftScalarCheck(): Unit {
    let i: Int64 = 1
    let _ = <!INVALID_COALESCING!>i<!> ?? 1
    let s = "a"
    let _ = <!INVALID_COALESCING!>s<!> ?? ""
    let b = true
    let _ = <!INVALID_COALESCING!>b<!> ?? false
    let u: Unit = ()
    let _ = <!INVALID_COALESCING!>u<!> ?? ()
}

// 形态 2：左操作数是 class 实例 / class 字段 / class 方法调用 / struct 形参 / struct 构造器 / struct 字段。
// kind：sema_invalid_coalescing
// 锚点：1 字符；成员访问（`b.v`、`b.coalM()`、`p.x`）锚的是**基表达式首字符**（b / p），不是成员名——
// 官方 BinaryExpr.cpp:1118 传的是 MemberAccess 节点本身，其 GetBegin 在基表达式。
// 1.0.5 / 1.1.3 一致：6 条，均 1 字符。
// FILE: coalLeftClassStruct.cj
class CoalBox {
    public var v: Int64 = 0

    public func coalM(): Int64 {
        return v
    }
}

struct CoalPoint {
    public let x: Int64 = 0
    public let y: Int64 = 0
}

func coalLeftClassStructCheck(b: CoalBox, p: CoalPoint): Unit {
    let _ = <!INVALID_COALESCING!>b<!> ?? 0
    let _ = <!INVALID_COALESCING!>b.v<!> ?? 0
    let _ = <!INVALID_COALESCING!>b.coalM()<!> ?? 0
    let _ = <!INVALID_COALESCING!>p<!> ?? 0
    let _ = <!INVALID_COALESCING!>CoalPoint()<!> ?? 0
    let _ = <!INVALID_COALESCING!>p.x<!> ?? 0
}

// 形态 3：左操作数是本包 enum 值 / Array / 元组 / 字符串下标 / 数组下标（结果类型不是 Option）。
// kind：sema_invalid_coalescing
// 锚点：左操作数 GetBegin 1 字符（下标表达式的 GetBegin 也在基表达式）。
// 合法对照：Option<本包 enum> ?? enum 构造器、Option<Array<T>> ?? Array 均零诊断（p13 / p30 实测）。
// 1.0.5 / 1.1.3 一致：6 条 INVALID_COALESCING，均 1 字符；2 条合法对照零诊断。
// FILE: coalLeftEnumArrayTuple.cj
enum CoalEnum { C1 | C2 }

func coalLeftEnumArrayTupleCheck(oEnum: Option<CoalEnum>, oArr: Option<Array<Int64>>): Unit {
    let e = CoalEnum.C1
    let _ = <!INVALID_COALESCING!>e<!> ?? CoalEnum.C2
    let arr = [1, 2]
    let _ = <!INVALID_COALESCING!>arr<!> ?? arr
    let tup = (1, 2)
    let _ = <!INVALID_COALESCING!>tup<!> ?? (1, 2)
    let s = "ab"
    let _ = <!INVALID_COALESCING!>s[0]<!> ?? 0
    let _ = <!INVALID_COALESCING!>arr[0]<!> ?? 0
    let _ = oEnum ?? CoalEnum.C1
    let _ = oArr ?? arr
}

// 形态 4：左操作数是函数名（函数类型）/ lambda 变量 / lambda 调用结果。
// kind：sema_invalid_coalescing（函数类型既不是 enum 也不是 Option）
// 锚点：1 字符（函数名首字母 c / lambda 变量 f / 调用表达式首字母 f）。
// 1.0.5 / 1.1.3 一致：3 条，均 1 字符。
// FILE: coalLeftFunctionValue.cj
func coalG1(): Int64 {
    return 1
}

func coalLeftFunctionValueCheck(): Unit {
    let _ = <!INVALID_COALESCING!>coalG1<!> ?? coalG1
    let f = { x: Int64 => x }
    let _ = <!INVALID_COALESCING!>f<!> ?? 0
    let _ = <!INVALID_COALESCING!>f(1)<!> ?? 0
}

// 形态 5：左操作数是括号表达式（GetBegin 含左括号）/ if 表达式的结果类型不是 Option / 括号包裹的 `??` 结果。
// kind：sema_invalid_coalescing
// 锚点：1 字符；括号表达式的 GetBegin 是**最外层左括号**（`(i)`、`(((s)))` 都锚在第 13 列的左括号）。
// `(x ?? 2) ?? 3` 用括号强制左结合，外层左操作数是括号内的 Int64 结果 → 锚左括号；
// 不加括号的 `x ?? 2 ?? 3` 见形态 6（右结合，内层左操作数 `2` 才是锚点）。
// 合法对照：if 两个分支都是 Option 时零诊断。
// 1.0.5 / 1.1.3 一致：6 条 INVALID_COALESCING，均 1 字符；1 条合法对照零诊断。
// FILE: coalLeftParenAndIfExpr.cj
func coalLeftParenAndIfExprCheck(): Unit {
    let i = 1
    let _ = (<!INVALID_COALESCING!>i<!>) ?? 1
    let s = "a"
    let _ = (((<!INVALID_COALESCING!>s<!>))) ?? ""
    let _ = (<!INVALID_COALESCING!>1 + 1<!>) ?? 2
    let a = if (true) { 1 } else { 2 }
    let _ = <!INVALID_COALESCING!>a<!> ?? 0
    let _ = if (true) { 1 } else { <!INVALID_COALESCING!>2<!> ?? 3 }
    let x = Some(1)
    let _ = (<!INVALID_COALESCING!>x ?? 2<!>) ?? 3
    let c = if (true) { Some(1) } else { Some(2) }
    let _ = c ?? 0
}

// 形态 6：`??` 同优先级右结合（官方 src/Parse/ParseExpr.cpp:539-543，同优先级时只有 `??` 与 `**` 不走左结合分支）。
//   `oInt ?? 2 ?? 3` 解析为 `oInt ?? (2 ?? 3)`：外层左操作数 oInt 合法，**内层** `2 ?? 3` 的左操作数 `2`
//   报 INVALID_COALESCING（锚 `2`）；左操作数非法后右操作数不再检查，也不追加 mismatched types。
// 合法对照：Option ?? Option ?? 字面量链（左结合 / 右结合两种解析下都合法），两版零诊断。
// 1.0.5 / 1.1.3 一致：1 条 INVALID_COALESCING（1 字符）。
// FILE: coalChainRightAssoc.cj
func coalChainRightAssocCheck(oInt: Option<Int64>, pInt: Option<Int64>): Unit {
    let _ = oInt ?? <!INVALID_COALESCING!>2<!> ?? 3
    let _ = oInt ?? pInt ?? 0
    let _ = pInt ?? oInt ?? pInt ?? 7
}

// 形态 7：元组解构得到的绑定变量作为左操作数（仍是普通 Int64 变量）。
// kind：sema_invalid_coalescing
// 合法对照：元组类型的可选 `?(Int64, Int64)` 是单类型实参的 Option，零诊断。
// 1.0.5 / 1.1.3 一致：2 条 INVALID_COALESCING，均 1 字符；1 条合法对照零诊断。
// FILE: coalLeftPatternBinding.cj
func coalLeftPatternBindingCheck(): Unit {
    let (a, b) = (1, 2)
    let _ = <!INVALID_COALESCING!>a<!> ?? b
    let _ = <!INVALID_COALESCING!>b<!> ?? a
    let m: ?(Int64, Int64) = None
    let _ = m ?? (1, 2)
}

// 形态 8：左操作数是**单类型参数的枚举**，但不是 core 包的 Option：
//   本段自定义的 `CoalSameNameOption<T>` 与 core 的 `Option` 同名（命中 IsCoalescingLeftTyValid :1102 的 identifier 条件），
//   但 declPtr 所在包不是 core（:1099 fullPackageName 分支）；`CoalOtherOption<T>` 名字不同，两条条件都不满足。
//   官方文案不变（不会额外提示“不是 core Option”）。
// kind：sema_invalid_coalescing
// 1.0.5 / 1.1.3 一致：3 条 INVALID_COALESCING，均 1 字符。
// FILE: coalLeftNonCoreOption.cj
enum CoalSameNameOption<T> { OptSome(T) | OptNone }

enum CoalOtherOption<T> { OSome(T) | ONone }

func coalLeftNonCoreOptionCheck(): Unit {
    let o: CoalSameNameOption<Int64> = OptSome(1)
    let _ = <!INVALID_COALESCING!>o<!> ?? 0
    let n: CoalSameNameOption<Int64> = OptNone
    let _ = <!INVALID_COALESCING!>n<!> ?? 0
    let oo: CoalOtherOption<Int64> = OSome(1)
    let _ = <!INVALID_COALESCING!>oo<!> ?? 0
}

// 形态 9：左操作数是合法 Option，右操作数与元素类型（realTgtTy）不符——Coalescing 检查器的第二个报告点。
//   · 非字面量右操作数 → sema_mismatched_types（CFIR TYPE_MISMATCH），锚**整个右操作数表达式**
//     （Diags.cpp:196-205 的 DiagnoseRefactor(node) 用 MakeRange，`"x"` 3 字符、`i8v` 3 字符）。
//   · 字面量右操作数 → CheckWithNegCache 先报 sema_cannot_convert_literal（CFIR CANNOT_CONVERT_LITERAL），
//     锚字面量 1 字符；浮点字面量 `1.5` 也只锚首字符 `1`。
//   本段左操作数都合法，所以右操作数错误一定会报；官方不会在这里再报 INVALID_COALESCING。
// 1.0.5 / 1.1.3 一致：3 条 TYPE_MISMATCH（3/3/4 字符）+ 3 条 CANNOT_CONVERT_LITERAL（1 字符）。
// FILE: coalRightOperandMismatch.cj
func coalRightOperandMismatchCheck(oi: Option<Int64>, ob: Option<Bool>, os: Option<String>, i8v: Int8, pOpt: Option<Int64>): Unit {
    let _ = oi ?? <!TYPE_MISMATCH!>"x"<!>
    let _ = oi ?? <!TYPE_MISMATCH!>i8v<!>
    let _ = oi ?? <!TYPE_MISMATCH!>pOpt<!>
    let _ = ob ?? <!CANNOT_CONVERT_LITERAL!>1<!>
    let _ = os ?? <!CANNOT_CONVERT_LITERAL!>1<!>
    let _ = oi ?? <!CANNOT_CONVERT_LITERAL!>1.5<!>
}

// 形态 10：声明的目标类型比 Option 的元素类型窄（官方 BinaryExpr.cpp:1127-1133，锚**左操作数**）。
// kind：sema_mismatched_types（CFIR TYPE_MISMATCH）
// 合法对照：目标类型与元素类型一致（Int64）或更宽（Any）时零诊断。
// 1.0.5 / 1.1.3 一致：1 条 TYPE_MISMATCH，Range (3,19)..(3,20)（锚左操作数 `o` 1 字符）。
// FILE: coalTargetTypeNarrower.cj
func coalTargetTypeNarrowerCheck(o: Option<Int64>): Unit {
    let _: Int8 = <!TYPE_MISMATCH!>o<!> ?? 0
    let _: Int64 = o ?? 0
    let _: Any = o ?? 0
}

// 形态 11：左操作数是**返回类型为类型参数**的泛型调用（占位类型）：官方 IsCoalescingLeftTyValid 对
//   IsPlaceholder 的类型先 ConstrainByCtor 到 core Option（BinaryExpr.cpp:1089-1092），实参 Int64 确定后
//   不是 Option → INVALID_COALESCING，锚在调用表达式首字符。
// 合法对照：同一泛型函数的实参是 Option 时零诊断。
// 1.0.5 / 1.1.3 一致：1 条 INVALID_COALESCING（1 字符）。
// FILE: coalGenericLeftNonOption.cj
func coalIdtNonOption<T>(x: T): T {
    return x
}

func coalGenericLeftNonOptionCheck(): Unit {
    let _ = <!INVALID_COALESCING!>coalIdtNonOption(1)<!> ?? 0
    let _ = coalIdtNonOption(Some(1)) ?? 0
}

// 形态 12（合法对照）：IsCoalescingLeftTyValid 四个条件都满足的形态两版零诊断——显式 Option<T>、
//   可选类型语法 T?（两者是同一个 core 枚举）、Option<T> 的类型别名、None 初值的 ?String、
//   嵌套 Option（元素类型本身是 Option）、Option 链 `a ?? b ?? c ?? 4`（右结合解析下外层左 Option 合法，
//   内层先算出元素类型再交给外层）。注意 **`Option<T> ?? Option<U>` 本身不是合法形态**
//   （右操作数要的是元素类型），见形态 9。
// FILE: coalLegalOptionForms.cj
type CoalIntOpt = Option<Int64>

func coalLegalOptionFormsCheck(): Unit {
    let a: Option<Int64> = Some(1)
    let b: ?Int64 = Some(2)
    let c: CoalIntOpt = Some(3)
    let n: ?String = None
    let z: Option<Option<Int64>> = Some(Some(6))
    let _ = b ?? 1
    let _ = c ?? 0
    let _ = n ?? "z"
    let _ = z ?? Some(7)
    let _ = a ?? b ?? c ?? 4
}

// 形态 13（合法对照）：泛型 Option<T>（含占位类型 ConstrainByCtor 后合法）、let / var 接收者、
//   var 赋值右侧、调用实参位置；两版零诊断。
// FILE: coalLegalGenericReceivers.cj
func coalMk<T>(v: T): Option<T> {
    return Some(v)
}

func coalPick<T>(a: Option<T>, b: Option<T>): Option<T> {
    return a
}

func coalIdt<T>(x: T): T {
    return x
}

func coalUse(x: Int64): Int64 {
    return x
}

class CoalGen<T> {
    public let value: T

    init(v: T) {
        value = v
    }

    public func coalAsOption(): Option<T> {
        return Some(value)
    }
}

func coalLegalGenericReceiversCheck(): Unit {
    var w = Some(1)
    w = w ?? 2
    let _ = w ?? 0
    let _ = coalMk(3) ?? 0
    let _ = coalPick(Some(1), Some(2)) ?? 0
    let _ = coalIdt(Some(4)) ?? 0
    let g = CoalGen<Int64>(5)
    let _ = g.coalAsOption() ?? 0
    let _ = coalUse(coalMk(6) ?? 0)
}

// ── 已删除的形态（探到但不可写 / 被前置错误屏蔽，官方 kind 与 Range 记录在此，不放标记）──
// 1. `a ??= b`：仓颉没有 `??=` 运算符。LexerImpl.h:161 / ParserImpl.h:275 只把 QUEST+QUEST 合并成
//    COALESCING，`??` 之后的 `=` 被当成表达式起始 → 官方 parse_expected_expression
//    "expected expression after '', found '='"（前置 token `??` 的文案为空），Range 锚 `=` 1 字符
//    （`    a ??= Some(2)` 的第 9 列）。1.0.5 / 1.1.3 一致。它是 parse 阶段诊断，不是语义族；
//    `??=` 的语法恢复形态见 testData/diagnostics2/syntax/recovery/coalescingAssign.cj（rawBuilder）。
// 2. `None ?? 0`（裸 None 作为左操作数）：先报 sema_generic_type_without_type_argument
//    "generic type should be used with type argument"，锚 `N` 1 字符（形如 `    let _ = None ?? 0` 的第 13 列）；
//    左操作数类型已经是 invalid，CanSkipDiag 为真 → **不报** sema_invalid_coalescing。1.0.5 / 1.1.3 一致。
//    要写 `??` 就得让 None 有类型上下文（`var w = Some(1); w = w ?? 2`，见形态 13）。
// 3. `unknownName ?? 0`（左操作数未解析）：只报 sema_undeclared_identifier "undeclared identifier
//    'unknownName'"（锚 `u` 1 字符），INVALID_COALESCING 被 CanSkipDiag 屏蔽。1.0.5 / 1.1.3 一致。
// 4. 左操作数非法时右操作数不再检查：`oi ?? 1.5` 里若 `oi` 本身非法，官方只报 sema_invalid_coalescing，
//    不会同时报 sema_cannot_convert_literal（BinaryExpr.cpp:1119 的 return false）。