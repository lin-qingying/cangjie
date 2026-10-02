// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5（准）与 1.1.3 两版实测；静态库模式 `cjc <file>.cj -o out.a --output-type=staticlib
// --diagnostic-format json`。每段单独编译，段内行号即 JSON 的 Line（1 起始），列号 1 起始。
// 1.1.3 本机在 CHIR 阶段对本文件涉及的元组 / 枚举 / 数组模式形态会 LLVM ERROR 崩溃（崩在语义检查之后，
//    因此两版的语义诊断一致）；1.1.3 只取语义诊断，1.0.5 为准。文案见 include/cangjie/Basic/DiagnosticSema.def。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
// 官方实现：
//   - src/Sema/TypeCheckDecl.cpp:259-281 CheckVarWithPatternDecl
//     （:271-274 initializer 类型错或无模式 → 直接 return；:275-277 ChkPattern 失败 → sema_mismatched_type_for_pattern_in_vardecl；
//      :278-280 IsIrrefutablePattern 失败 → sema_pattern_can_not_be_assigned，两处锚点都是 irrefutablePattern 的 GetBegin，1 个字符）
//   - src/Sema/TypeCheckMatchExpr.cpp:468-497 IsIrrefutablePattern
//     （CONST / TYPE 不可反驳；WILDCARD / VAR 可反驳；TUPLE 全部分量可反驳；ENUM 只在单个构造器且分量全可反驳时）
//   - src/Sema/TypeCheckPattern.cpp:458-485 ChkTuplePattern（:461 元数不符 → sema_tuple_pattern_with_correct_size_expected，
//     :475 非 match 时初始化值非元组 → sema_tuple_pattern_not_match "initializer"，两处锚点都是元组模式的 GetBegin，1 个字符）
//   - src/Sema/TypeCheckExpr/LoopExprs.cpp:94-134 SynForInExpr（:110-112 迭代对象非 Iterable → sema_expr_in_forin_must_has_iterator，
//     锚 in 表达式 GetBegin，CanSkipDiag 门控；:115 ChkPattern 检查模式与元素类型；:118-121 patternGuard 非 Bool →
//     sema_wrong_forin_guard，锚 guard 表达式 GetBegin；:127-129 模式不可反驳 → sema_forin_pattern_must_be_irrefutable，锚 fie GetBegin）
//   - src/Parse/ParseAtom.cpp:999-1010 for-in 节点的 begin 取 `for` 关键字的 lookahead.Begin()（= fie.GetBegin()）
//   - src/Parse/ParseDecl.cpp:86-105 ParseIrrefutablePattern（`(` → 元组模式；标识符后接 `(` `.` `<` → 枚举模式；裸标识符 → 变量模式）
//   - src/Parse/ParsePattern.cpp:28-49 ParsePattern（字面量 → ParseConstPattern 常量模式；标识符 → 枚举模式或 VarOrEnumPattern）
//   - src/Sema/TypeCheckPattern.cpp:513-547 FindEnumPatternTarget（无子模式的枚举模式只在 `Enum.ctor` 限定名形态下才被解析成 EnumPattern；
//     裸写 `A` 是 ParseIrrefutablePattern 直接产出的 VarPattern，不查构造器、不查不可反驳性）
//   - src/AST/ASTContext.cpp:136-138 IsEnumConstructor、include/cangjie/Basic/DiagnosticEngine.h:822-824 / 844-857：
//     非 refactor 的 Diagnose(node, kind) 一律退化成节点 GetBegin 的一个字符
//   - src/Sema/TypeCheckPattern.cpp:362-420 ChkEnumPattern（:396 / :405 / :415 enum 模式类型不匹配 → sema_pattern_not_match "enum"，锚模式 GetBegin，1 个字符）
// 本文件内联标记用本项目 CFIR 诊断名（cfir/checkers/gen/.../diagnostics/CfirErrors.kt）；官方 kind 见上。
// 已有覆盖不重复：patternLegality.cj（match 里的 TYPE_MISMATCH / CANNOT_CONVERT_LITERAL / ENUM_PATTERN_PARAM_SIZE_ERROR）、
// inference/arrayLiteralAndMultipleAssign.cj（合法的 var (a, b) = (1, 2)）。
// NOT_OVERLOAD_IN_MATCH / MISMATCHED_TYPE_FOR_PATTERN_IN_VARDECL 两条官方规则在变量声明形态里不可达：常量模式在
// ChkVarWithPatternDecl :271-274 会先让 ChkPattern 失败，tuple 模式分量失败只报外层的 sema_pattern_can_not_be_assigned
// （见第 3 段实测），故本文件不写这两条；它们的 match 形态已有覆盖。
// parser 恢复形态（`let 1 = x` → parse_expected_one_of_identifier_or_pattern，`let K() = x` → parse_expected_pattern）
// 不在本文件：两版 cjc 的 MainHint 锚点分别是 `1` 与 `)`，与本文件的模式锚点族无关；PARSE_ 侧规则另立文件。

// FILE: patternCanNotBeAssigned.cj
enum CanNotBeAssignedMulti { A | B | C }

// 形态 1：元组模式里任一分量带类型注解即成为 TYPE_PATTERN → 不可反驳。
// 两版各 1 error：sema_pattern_can_not_be_assigned（锚模式首字符 `(`，6:9..6:10）。
func tuplePatternWithTypeAnnotation(v: (Int64, String)): Unit {
    let <!PATTERN_CAN_NOT_BE_ASSIGNED!>(<!>a: Int64, b: String) = (1, "s")
}

// 形态 2：只给第二个分量加类型注解，同样不可反驳。
func tuplePatternWithOneAnnotation(v: (Int64, String)): Unit {
    let <!PATTERN_CAN_NOT_BE_ASSIGNED!>(<!>a, b: String) = (1, "s")
}

// 形态 3：类型注解与初值分量类型不一致时，官方仍只报不可反驳，不报分量类型不匹配（实测见第 3 段）。
func tuplePatternAnnotationMismatch(v: (Int64, String)): Unit {
    let <!PATTERN_CAN_NOT_BE_ASSIGNED!>(<!>a: Int64, b: String) = (1, 2)
}

// 形态 4：类型注解与初值一致的元组模式仍不可反驳（与 inference/arrayLiteralAndMultipleAssign.cj 的无注解合法对照成对）。
func tuplePatternAnnotationMatch(v: (Int64, String)): Unit {
    let <!PATTERN_CAN_NOT_BE_ASSIGNED!>(<!>a: Int64, b: String) = (1, "s")
}

// FILE: enumPatternWithoutSubPattern.cj
enum BareNameMulti { A | B | C }
enum BareNameSingle { Only(Int64) }

// 裸枚举构造器名、无子模式的声明，官方两版都解析为 VarPattern 而不是 EnumPattern
// （ParseIrrefutablePattern → ParseVarPattern），所以既不做构造器解析也不查不可反驳性。
// 形态 5：多构造器枚举的裸构造器名 + 显式类型注解：1.0.5 零诊断；1.1.3 零诊断（codegen 阶段崩溃）。
func bareMultiConstructorNameTyped(v: BareNameMulti): Unit {
    let A: BareNameMulti = v
}

// 形态 6：多构造器枚举的裸构造器名 + 无类型注解：1.0.5 零诊断；1.1.3 零诊断（codegen 阶段崩溃）。
// 官方把它当普通变量绑定：后续把 A 当枚举值使用时报 sema_mismatched_types（7:13），而不是不可反驳诊断。
func bareMultiConstructorNameInferred(v: BareNameMulti): Unit {
    let A = v
}

// 形态 7：单构造器枚举的裸构造器名 + 显式类型注解：官方可反驳，同样零诊断。
func bareSingleConstructorNameTyped(v: BareNameSingle): Unit {
    let Only: BareNameSingle = v
}

// FILE: enumPatternWithSubPattern.cj
enum SubPatternPayload { C(Int64) | D(String) }

// 形态 8：裸构造器名 + 子模式，多构造器枚举 → EnumPattern → 不可反驳。
// 两版各 1 error：sema_pattern_can_not_be_assigned（1 个字符）。
func bareConstructorWithSubPattern(v: SubPatternPayload): Unit {
    let <!PATTERN_CAN_NOT_BE_ASSIGNED!>C<!>(y) = v
}

// 形态 9：限定名 `Enum.ctor` + 子模式，多构造器枚举 → 同一诊断、同一锚点。
func qualifiedConstructorWithSubPattern(v: SubPatternPayload): Unit {
    let <!PATTERN_CAN_NOT_BE_ASSIGNED!>S<!>ubPatternPayload.C(y) = v
}

// 形态 10：裸写的无参数构造器 `let D = v` 不是枚举模式而是变量绑定（见 enumPatternWithoutSubPattern.cj 段）；
// 写成限定名 `SubPatternPayload.D` 才是枚举模式，但本枚举还有带参构造器，`D` 的类型是函数类型而不是枚举类型自身，
// FindEnumPatternTarget（:513-547）的空子模式分支只接受类型为 EnumTy 的候选，找不到目标即报
// sema_enum_pattern_param_size_error "enum pattern's parameters size is wrong"（锚模式首字符，`SubPatternPayload` 的 `S`）。
// 对照：全为无参构造器的枚举里 `Multi.A` 的类型就是枚举类型自身，落到不可反驳检查，报 sema_pattern_can_not_be_assigned
//（1.0.5 / 1.1.3 实测一致，见 enumPatternWithoutSubPattern.cj 段的 legalBareEnumName 之后的探针）。
func qualifiedConstructorWithoutSubPattern(v: SubPatternPayload): Unit {
    let <!ENUM_PATTERN_PARAM_SIZE_ERROR!>S<!>ubPatternPayload.D = v
}

// FILE: tuplePatternDeclarationShape.cj
// 形态 11：初始化值不是元组（isPatternInMatch=false 分支）。
// 两版各 1 error：sema_tuple_pattern_not_match "initializer isn't a tuple to match tuple pattern"（1 个字符）。
func tuplePatternOnScalar(v: Int64): Unit {
    let <!TUPLE_PATTERN_NOT_MATCH!>(<!>a, b) = v
}

// 形态 12：初值元组元数与模式不一致（比模式多）。
// 两版各 1 error：sema_tuple_pattern_with_correct_size_expected（1 个字符）。
func tuplePatternTooManyElements(): Unit {
    let <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(<!>a, b) = (1, 2, 3)
}

// 形态 13：初值元组元数与模式不一致（比模式少），同一 kind、同一锚点。
func tuplePatternTooFewElements(): Unit {
    let <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(<!>a, b, c) = (1, 2)
}

// 形态 14：元组模式里含常量模式、且模式与初值一致 —— 官方两版零诊断（1.0.5 只有 CHIR 的 unused variable 警告，
// 1.1.3 codegen 崩溃）。常量模式的 ChkConstPattern 通过后 IsIrrefutablePattern 判不可反驳，但
// CheckVarWithPatternDecl :272 的门控先因 ChkPattern 失败而 return，所以这里不会出现任何错误。
// 与本文件第一段的形态 1 对照：唯一差别是类型注解。
func tuplePatternWithConstComponent(s: String): Unit {
    let (a, b) = (1, s)
}

// FILE: forInPatternIrrefutable.cj
enum ForInIrrefutableMulti { A | B | C }
enum ForInIrrefutablePayload { C(Int64) | D(String) }

// for-in 的变量模式必须不可反驳；官方锚点是 fie 的 GetBegin，即 `for` 关键字。
// 形态 15：类型模式（分量带类型注解）。
// 两版各 1 error：sema_forin_pattern_must_be_irrefutable（1 个字符，`for` 的 `f`）。
func typePatternInForIn(): Unit {
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (i: Int64 in [1, 2]) {
    }
}

// 形态 16：常量模式（ParsePattern :28 字面量 → ConstPattern）。
func constPatternInForIn(): Unit {
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (1 in [1, 2]) {
    }
}

// 形态 17：裸构造器名 + 子模式，多构造器枚举 → EnumPattern → 不可反驳；
// 元素类型是 Int64 而模式目标是枚举 `ForInIrrefutablePayload` 的构造器 → ChkEnumPattern 另报
// sema_pattern_not_match "enum"（锚模式首字符 `C`）。
// 两版各 2 error：sema_forin_pattern_must_be_irrefutable（`for` 首字符）+ sema_pattern_not_match（`C` 首字符）。
func bareEnumPatternInForIn(): Unit {
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (<!PATTERN_NOT_MATCH!>C<!>(y) in [1, 2]) {
    }
}

// 形态 18：限定名 + 子模式，多构造器枚举 → 同一诊断、同一锚点。
func qualifiedEnumPatternInForIn(): Unit {
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (ForInIrrefutablePayload.C(y) in [ForInIrrefutablePayload.C(1), ForInIrrefutablePayload.D("s")]) {
    }
}

// 形态 19：裸构造器名、无子模式，在 for-in 里被解析为 EnumPattern（ParseIrrefutablePattern 不产生 VarPattern），
// 且元素类型正确（ForInIrrefutableMulti）→ 只报不可反驳，不报 pattern_not_match。
func bareEnumNameInForIn(): Unit {
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (ForInIrrefutableMulti.A in [ForInIrrefutableMulti.A, ForInIrrefutableMulti.B]) {
    }
}

// FILE: forInPatternType.cj
enum ForInTypeMulti { A | B | C }

// 形态 20：枚举模式与元素类型不匹配（Int64 数组上匹配 ForInTypeMulti）→ 官方 ChkEnumPattern :449。
// 两版各 2 error：sema_pattern_not_match "enum pattern is not matched"（1 个字符，模式首字符）
// + sema_forin_pattern_must_be_irrefutable（1 个字符）。
func enumPatternTypeNotMatch(): Unit {
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (<!PATTERN_NOT_MATCH!>F<!>orInTypeMulti.A in [1, 2, 3]) {
    }
}

// 形态 21：裸枚举构造器名 + 子模式，元素类型 Int64 → 同一组诊断、同一组锚点。
func bareEnumPatternTypeNotMatch(): Unit {
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>f<!>or (<!PATTERN_NOT_MATCH!>C<!>(y) in [1, 2]) {
    }
}

// FILE: forInIterable.cj
// 形态 22：in 表达式类型不实现 Iterable。
// 两版各 1 error：sema_expr_in_forin_must_has_iterator "the type Int64 of expression in for-in expression
// does not implement Iterator"（1 个字符，in 表达式首字符）。
func forInOnScalar(): Unit {
    for (i in <!EXPR_IN_FORIN_MUST_HAS_ITERATOR!>1<!>) {
    }
}

// 形态 23：in 表达式是二元运算，类型仍是 Int64 → 同一 kind、同一锚点族。
func forInOnBinaryScalar(): Unit {
    for (i in <!EXPR_IN_FORIN_MUST_HAS_ITERATOR!>1<!> + 2) {
    }
}

// FILE: legalDeclarationsAndLoops.cj
enum LegalMulti { A | B | C }
enum LegalSingle { Only(Int64) }

// 合法对照（1.0.5 全部零诊断；标注 † 的在 1.1.3 codegen 阶段崩溃但语义阶段零诊断）。
func legalBareEnumName(v: LegalMulti): Unit {
    let A: LegalMulti = v // † 形态 5
}

func legalSingleConstructorEnumName(v: LegalSingle): Unit {
    let Only: LegalSingle = v // † 形态 7
}

func legalTuplePattern(): Unit {
    let (a, b) = (1, "s") // † 形态 14
    println(a)
}

func legalForInOverArray(): Unit {
    for (i in [1, 2]) {
        println(i)
    }
}

func legalForInOverRange(): Unit {
    for (i in 1..3) {
        println(i)
    }
}

func legalForInWithGuard(): Unit {
    for (i in [1, 2] where i > 0) {
        println(i)
    }
}

func legalForInTuplePattern(): Unit {
    for ((a, b) in [(1, 2)]) {
        println(a)
    }
}

// FILE: forInGuard.cj
/*
SUGGESTED_DIAGNOSTIC: WRONG_FORIN_GUARD
SUGGESTED_MESSAGE: pattern guard should be Boolean type
SOURCE: src/Sema/TypeCheckExpr/LoopExprs.cpp:113-119; include/cangjie/Basic/DiagnosticSema.def:133
*/

// 形态 24：guard 是整数字面量。`Check(ctx, Bool, guard)` 报 sema_cannot_convert_literal
// "cannot convert an integer literal to type 'Bool'"（锚 guard 表达式首字符 `1`）；随后 guard 的类型不正确，
// LoopExprs.cpp:117 的 `CanSkipDiag(*fie.patternGuard)` 为真，sema_wrong_forin_guard 被抑制（1.0.5 / 1.1.3
// 两版该行只有这 1 条）。只落 CANNOT_CONVERT_LITERAL 标记。
func guardIsIntegerLiteral(): Unit {
    for (i in [1, 2] where <!CANNOT_CONVERT_LITERAL!>1<!>) {
        let u = i
    }
}

// 形态 25：guard 是字符串字面量。guard 的类型（String）本身正确，`Check` 只报 sema_mismatched_types
// "mismatched types" hint "expected 'Bool', found 'Struct-String'"（锚 guard 表达式整段 `"s"`，3 个字符），
// 随后 `CanSkipDiag` 为假，LoopExprs 继续报 sema_wrong_forin_guard "pattern guard should be Boolean type"
// （锚 guard 表达式首字符）。sema_mismatched_types 在本项目对应 TYPE_MISMATCH（DiagnosticNameMapper 映射，
// patternLegality.cj 同款写法），写标记；WRONG_FORIN_GUARD 在 CfirErrors / CfirDiagnosticsList 里没有对应名字，
// 按 SUGGESTED_DIAGNOSTIC 处理：不写标记。
func guardIsStringLiteral(): Unit {
    for (i in [1, 2] where <!TYPE_MISMATCH!>"s"<!>) {
        let u = i
    }
}