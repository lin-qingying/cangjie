// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5（准）与 1.1.3 两版实测，静态库模式
// `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
// （官方整包编译时一处 parse 错误会屏蔽其余段的语义诊断，且 HasPrevDiag 会吞掉同起点同文案的重复诊断）。
// 1.1.3 本机在 CHIR/codegen 阶段偶有 LLVM 崩溃，崩在语义检查之后，语义诊断 JSON 仍完整产出，本文件按语义诊断写期望。
// 除特别注明外，两版 kind、消息、MainHint.Range 完全一致；本文件所有形态都不触发版本差异，无需按 LANGUAGE_VERSION 拆分。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围；
// 官方非 refactor kind 的 `Diagnose(node, kind)` 退化为 `node.GetBegin()` 一个字符
// （include/cangjie/Basic/DiagnosticEngine.h:844-857），refactor kind 走 `DiagnoseRefactor(kind, node)` =
// `MakeRange(node.GetBegin(), node.GetEnd())`（同文件 :908-911）。本文件除 sema_mismatched_types_because 外全是单字符锚点。
//
// 官方实现（external/cangjie_compiler，checkout 在 tag v1.0.0）：
//   - NOT_A_TYPE：三个报告点，全部是 `Diagnose(node 或 position, kind)` 的单字符锚点
//     src/Sema/PreCheck.cpp:404 GetTyFromASTType(RefType&)：引用名能查到顶层声明但不是类型声明 → "'x' is not a type"，
//       锚 RefType 首字符；
//     src/Sema/PreCheck.cpp:497 GetTyFromASTType(QualifiedType&)：限定名的 field 不是类型声明 → 锚 qt.field 首字符；
//     src/Sema/PreCheck.cpp:724 SetTypeAliasDeclTy：typealias RHS 类型树最终 Ty 不正确 → 锚 RHS 根类型首字符。
//     文案 include/cangjie/Basic/DiagnosticSema.def:16。
//   - REF_NOT_BE_TYPE：src/Sema/TypeCheckReference.cpp:626 CheckAccessLegalityOfRefExpr（`re.isAlone && decl->IsTypeDecl()`，
//     附注 sema_found_candidate_decl 指向候选声明）与 :676 CheckStaticMemberAccessLegality（`ma.isAlone && target.IsTypeDecl()`
//     的 MemberAccess 形态）。文案 include/cangjie/Basic/DiagnosticSema.def:129。
//   - MISMATCHED_TYPES_BECAUSE：src/Sema/Diags.cpp:248-253 DiagMismatchedTypes(diag, node, typeNode, because)——
//     仅当 `type.ShouldDiagnose() && !because.empty()` 时才发 sema_mismatched_types_because，
//     锚点传的是 **被检查的节点**（`DiagnoseRefactor(kind, node)` → 整段 node 范围）；否则退回无 because 的
//     sema_mismatched_types。全仓只有两个带非空 because 的调用点：src/Sema/TypeChecker.cpp:134 与 :150
//     （函数体与声明返回类型不匹配，because = "return type"），main hint "expected '%s' because of %s"
//     锚在返回类型节点上。src/Sema/TypeCheckExpr/TryExpr.cpp:113 传的是 Ty& 重载（because 退化成 Note），不是本 kind。
//     文案 include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:14。
//   - TYPE_INCOMPATIBLE：五个报告点
//     src/Sema/TypeCheckExpr/AssignExpr.cpp:378（`**=` 右值不是 Int64/Float64）、:393（普通赋值 Check 失败）、
//     :431 PreCheckCompoundAssign（复合赋值左值类型不在 COMPOUND_ASSIGN_TYPE_MAP（:37）里）、
//     src/Sema/TypeCheckMatchExpr.cpp:136（if-let 两分支类型不等）与 :208（match 分支 join 失败）、
//     src/Sema/TypeCheckPattern.cpp:613 ChkExceptTypePattern（catch 联合类型没有最小公共父类型）。
//     五处都是 `diag.Diagnose(node, kind, "%s")`，锚点为节点首字符。文案 include/cangjie/Basic/DiagnosticSema.def:107。
//   - INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS：src/Sema/TypeCheckReference.cpp:708 IsNamespaceMemberAccessLegality
//     （已显式给出 enum owner 后成员又带类型实参）→ `diag.Diagnose(ma, ...)`，锚 MemberAccess 首字符（owner 的首字母），
//     文案 DiagnosticSema.def:32。
//
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/src/.../diagnostics/CfirDiagnosticsList.kt，
// 名称映射见 cfir/analysis-tests/testFixtures/.../golden/DiagnosticNameMapper.kt，生产者见 cfir/checkers/gen/.../CfirErrors.kt）：
//   NOT_A_TYPE  CfirDiagnosticsList.kt:65（CfirErrors.kt:42）→ sema_not_a_type；
//     生产者 CfirTypeAliasExpandedTypeChecker.kt:47（typealias RHS）与 coneDiagnosticToCfirDiagnostic.kt:2601（ConeNotATypeError）。
//   REF_NOT_BE_TYPE  CfirDiagnosticsList.kt:64（CfirErrors.kt:41）→ sema_ref_not_be_type；
//     生产者 CfirClassifierAsExpressionChecker.kt:53（类型参数）与 :58（class-like，锚 startOffset..startOffset+1）。
//   MISMATCHED_TYPES_BECAUSE  CfirDiagnosticsList.kt:1139（CfirErrors.kt:302）→ sema_mismatched_types_because；
//     生产者 coneDiagnosticToCfirDiagnostic.kt:2726（映射自 ConeMismatchedTypesBecauseError）。
//   TYPE_INCOMPATIBLE  CfirDiagnosticsList.kt:930（CfirErrors.kt:260，DiagnosticNameMapper.kt:51）→ sema_type_incompatible；
//     生产者 CfirCompoundAssignmentSemanticsChecker.kt:66 / :88（复合赋值，锚左值首字符）与
//     CfirAssignmentTypeMismatchChecker.kt:125（赋值表达式首字符）。
//   INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS  CfirDiagnosticsList.kt:1432（CfirErrors.kt:367）→
//     sema_invalid_type_param_of_enum_member_access；生产者 CfirGenericBareClassifierAccessChecker.kt:251
//     （锚 owner access 的 calleeReference.source，官方锚 ma 首字符，两者同为 owner 的首字母）。
//   按源码（未跑 CFIR）可预判的锚点差异：TYPE_INCOMPATIBLE 的两个复合赋值生产者都用
//   `expression.lValue.source` / `expression.source` 的整段源，而官方只锚赋值表达式首字符——当左值不是单标识符
//   （如 `a.b += 1`）时两者会不一致；本文件只用单标识符左值，两侧一致。
//
// 官方 v1.0.0 无同名 kind 的 CFIR 名字（本文件按官方实测写标记，不用该名）：
//   ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR（CfirDiagnosticsList.kt:74、CfirErrors.kt:44、
//   DiagnosticNameMapper.kt:70 映射到 sema_enum_type_cannot_be_used_as_constructor）在官方 v1.0.0 的
//   include/**.def 与 src/** 全仓 grep 不到任何定义或报告点；官方对“把枚举类型当构造器调用”报的是
//   sema_no_match_operator_function_call（DiagnosticSema.def:98），见 enumTypeCalledAsConstructor.cj 段。
//
// 已有覆盖不重复：argumentTypeMismatch.cj / assignmentTypeMismatch.cj / returnTypeMismatch.cj（TYPE_MISMATCH /
//   CANNOT_CONVERT_LITERAL 族）、unresolved/unresolvedSymbolType.cj（UNDECLARED_TYPE_NAME）、
//   generic-access/genericTypeWithoutTypeArgument.cj（GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT）。
//   LLT 侧的 llt/class/declared_supertype_classification.cj、llt/generics/class4_test.cj、llt/enum/enum15_1.cj、
//   llt/generics/enum5.cj 等只作为本文件形态的旁证，不构成 diagnostics2 覆盖。
//   泛型族的 VALUE_TYPE_RECURSIVE 由泛型族夹具负责，本文件不写。

// FILE: funcNameAsParamType.cj
// 形态：把顶层函数名写在形参类型位置。官方 PreCheck.cpp:404（RefType 分支）。
// kind：sema_not_a_type "'trAliasTargetFunc' is not a type"，锚 RefType 首字符（1 个字符）。
// 1.0.5 / 1.1.3 一致，Range 为 `t` 单字符。
func trAliasTargetFunc(): Int64 {
    return 0
}

func trFuncAsParamType(x: <!NOT_A_TYPE!>trAliasTargetFunc<!>): Unit {
    let _ = x
}

// FILE: localNameAsType.cj
// 形态：把同作用域的局部 `let` 变量名写在类型位置。官方 PreCheck.cpp:404（RefType 分支）。
// kind：sema_not_a_type "'loc' is not a type"，锚 `l`（1 个字符）。1.0.5 / 1.1.3 一致。
func trLocalNameAsType(): Unit {
    let loc = 1
    var a: <!NOT_A_TYPE!>loc<!> = 0
    a = 1
}

// FILE: typeAliasRhsIsFunc.cj
// 形态：typealias 的右侧是函数名。官方 PreCheck.cpp:724 SetTypeAliasDeclTy
//（RHS 类型树 Ty 不正确时在 `*tad.type` 上补报，锚 RHS 根类型首字符）。
// kind：sema_not_a_type "'trFuncAliasTarget' is not a type"，锚 `t`（1 个字符）。1.0.5 / 1.1.3 一致。
func trFuncAliasTarget(): Int64 {
    return 0
}

type trFuncAlias = <!NOT_A_TYPE!>trFuncAliasTarget<!>

// FILE: enumConstructorAsType.cj
// 形态：把枚举构造器写在类型位置（`EnumType.Ctor`）。官方 PreCheck.cpp:497（QualifiedType 分支，
// baseType 解析成枚举类型、field 是构造器而非类型声明 → 锚 qt.field 首字符）。
// kind：sema_not_a_type "'A' is not a type"，锚 `A`（1 个字符）。1.0.5 / 1.1.3 一致。
enum trEnumForCtorType {
    | A
    | B
}

func trEnumCtorAsType(): Unit {
    var a: trEnumForCtorType.<!NOT_A_TYPE!>A<!> = trEnumForCtorType.A
    let _ = a
}

// FILE: typeNameAsValue.cj
// 形态：把类型名（class / struct / interface）当值单独引用。官方 TypeCheckReference.cpp:626
// CheckAccessLegalityOfRefExpr（`re.isAlone && decl->IsTypeDecl()` → 锚 RefExpr 首字符，附注 "found candidate"）。
// kind：sema_ref_not_be_type "expected member name or constructor call after 'X' type name"，三处各 1 个字符。
// 1.0.5 / 1.1.3 一致。构造器调用 `trValueClass()` 合法（见 typeReferenceOk.cj 段）。
class trValueClass {
}

struct trValueStruct {
}

interface trValueIface {
}

func trTypeNameAsValue(): Unit {
    let a = <!REF_NOT_BE_TYPE!>t<!>rValueClass
    let b = <!REF_NOT_BE_TYPE!>t<!>rValueStruct
    let c = <!REF_NOT_BE_TYPE!>t<!>rValueIface
    let _ = a
    let _ = b
    let _ = c
}

// FILE: typeAliasAsValue.cj
// 形态：把指向 class 的 typealias 当值单独引用，与 typeNameAsValue 同一条官方检查（RefExpr 首字符）。
// kind：sema_ref_not_be_type，锚 `t`（1 个字符）。1.0.5 / 1.1.3 一致。
class trAliasTargetClass {
}

type trClassAlias = trAliasTargetClass

func trAliasAsValue(): Unit {
    let a = <!REF_NOT_BE_TYPE!>t<!>rClassAlias
    let _ = a
}

// FILE: typeParamAsValue.cj
// 形态：把泛型函数的类型参数当值单独引用（TypeParam 也算 IsTypeDecl）。锚 RefExpr 首字符 `T`。
// kind：sema_ref_not_be_type "… after 'T' type name"，锚 `T`（1 个字符）。1.0.5 / 1.1.3 一致。
func trTypeParamAsValue<T>(x: T): Unit {
    let y = <!REF_NOT_BE_TYPE!>T<!>
    let z = x
    let _ = y
    let _ = z
}

// FILE: genericTypeAsValue.cj
// 形态：把带显式类型实参的泛型类型名当值单独引用（不带 `()`）。锚 RefExpr 首字符。
// kind：sema_ref_not_be_type，锚 `t`（1 个字符）。1.0.5 / 1.1.3 一致。
class trGenericValueClass<T> {
}

func trGenericTypeAsValue(): Unit {
    let a = <!REF_NOT_BE_TYPE!>t<!>rGenericValueClass<Int64>
    let _ = a
}

// FILE: enumTypeCalledAsConstructor.cj
// 形态：把枚举类型名当构造器调用 `EnumName(args)`。
// kind：sema_no_match_operator_function_call "no matching function for operator '()' function call"
//      （DiagnosticSema.def:98），锚被调表达式的首字符 `t`（1 个字符）。1.0.5 / 1.1.3 一致。
// 本段不写 CFIR 的 ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR：官方 v1.0.0 没有同名 kind（见文件头），
// 官方实测量化到 NO_MATCH_OPERATOR_FUNCTION_CALL（DiagnosticNameMapper.kt:69 有该映射）。
enum trCalledEnum {
    | A
    | B
}

func trEnumCalledAsCtor(): Unit {
    let x = <!NO_MATCH_OPERATOR_FUNCTION_CALL!>t<!>rCalledEnum(1)
    let _ = x
}

// FILE: emptyBodyWithValueReturnType.cj
// 形态：声明了非 Unit 返回类型、函数体却是空块。官方 TypeChecker.cpp:134
//（NeedCheckBodyReturn 为真、Check 失败且 `fb.body->body.empty()` → DiagMismatchedTypes(node = 整个函数体块, retType, "return type")）
// → Diags.cpp:248-253 发 sema_mismatched_types_because，锚点传的是被检查节点（函数体块），
// `DiagnoseRefactor(kind, node)` 覆盖 `{` 到 `}` 整段（跨行 2 行锚点）；main hint "expected 'Int64' because of return type"
// 锚在返回类型节点 `Int64` 上（不是错误锚点）。
// kind：sema_mismatched_types_because "mismatched types"，锚整条函数体块。1.0.5 / 1.1.3 一致
//（Range (3,31)..(4,2) 与 (7,34)..(8,2)）。
func trEmptyBodyInt(): Int64 <!MISMATCHED_TYPES_BECAUSE!>{
}<!>

class trEmptyBodyTarget {
}

func trEmptyBodyClass(): trEmptyBodyTarget <!MISMATCHED_TYPES_BECAUSE!>{
}<!>

// FILE: compoundAssignOnClassInstance.cj
// 形态：复合赋值左值是 class 实例（类型不在 COMPOUND_ASSIGN_TYPE_MAP 内）。
// 官方 AssignExpr.cpp:431 PreCheckCompoundAssign → `diag.Diagnose(ae, kind, "compound assignment expression")`，
// 锚赋值表达式首字符（`ae` 首字符 = 左值首字符，1 个字符）。
// kind：sema_type_incompatible "type incompatible in this compound assignment expression"，锚 `p`（1 个字符）。
// 1.0.5 / 1.1.3 一致。
class trCompoundTarget {
}

func trCompoundOnClass(): Unit {
    var p = trCompoundTarget()
    <!TYPE_INCOMPATIBLE!>p<!> += 1
    let _ = p
}

// FILE: exponentAssignWrongRhs.cj
// 形态：`**=` 右值不是 Int64 / Float64。官方 AssignExpr.cpp:378（`diag.Diagnose(ae, kind, "assignment")`，
// 锚赋值表达式首字符）。右值是字面量但 CheckExponentByBaseTy 先失败，所以本段没有 CANNOT_CONVERT_LITERAL。
// kind：sema_type_incompatible "type incompatible in this assignment"，锚 `u`（1 个字符）。1.0.5 / 1.1.3 一致。
func trExponentAssign(): Unit {
    var u = 2
    <!TYPE_INCOMPATIBLE!>u<!> **= true
    let _ = u
}

// FILE: catchUnionNoCommonSupertype.cj
// 形态：catch 的联合异常类型没有最小公共父类型（trCaughtExc 直接继承 Exception，与 Error 分支无公共父类型）。
// 官方 TypeCheckPattern.cpp:613 ChkExceptTypePattern（每个类型各自合法，JoinAndMeet 失败后
// `diag.Diagnose(etp, kind, "pattern")`，锚 ExceptTypePattern 首字符 = 模式变量的首字母）。
// kind：sema_type_incompatible "type incompatible in this pattern"，锚 `e`（1 个字符）。1.0.5 / 1.1.3 一致。
class trCaughtExc <: Exception {
}

func trCatchUnion(): Int64 {
    try {
        throw trCaughtExc()
    } catch (<!TYPE_INCOMPATIBLE!>e<!>: trCaughtExc | Error) {
        return 1
    }
}

// FILE: enumMemberTypeArgsAfterOwner.cj
// 形态：已显式给出 enum owner 的类型实参之后，枚举成员又带一次类型实参。
// 官方 TypeCheckReference.cpp:708 IsNamespaceMemberAccessLegality（`diag.Diagnose(ma, ...)`，锚 MemberAccess 首字符，
// 即 owner `trYearEnum` 的首字母 `t`，不是成员名 `Year`）。
// kind：sema_invalid_type_param_of_enum_member_access
//      "type arguments cannot appear after 'Year' when enum type 'trYearEnum' is given"，锚 `t`（1 个字符）。
// 1.0.5 / 1.1.3 一致。
enum trYearEnum<T> {
    | Year(T)
    | Month
}

var trYearEnumAccess = <!INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS!>trYearEnum<!><Int32>.Year<Int64>(4)

// FILE: typeReferenceOk.cj
// 合法对照：类型名的正确用法——构造器调用、枚举构造器、typealias 作类型、枚举类型作类型。1.0.5 / 1.1.3 零 error。
// 两个写法约束（实测）：`let _ = 非 Unit 值` 会命中 sema_mismatched_types（`let _` 自身的类型是 Unit，
// 且函数最后一条语句若不是 `return` 就充当隐式返回值），class / enum 也没有可用的 `==` / `hashCode`
// （`sema_invalid_binary_expr` / `sema_not_member_of`），所以本段把绑定结果留在局部不消费；
// 1.0.5 的 CHIR DCE 会对未使用的 `let c` / `let d` 额外报 chir_dce_unused_variable warning
//（codegen 阶段，非语义诊断，1.1.3 无；本文件 RUN_PIPELINE_TILL: FRONTEND 不产出该阶段诊断）。
class trOkClass {
}

enum trOkEnum {
    | A
    | B
}

type trOkAlias = trOkClass

func trTypeReferenceOk(): Unit {
    let a = trOkClass()
    let b = trOkEnum.A
    let c: trOkAlias = trOkClass()
    let d: trOkEnum = trOkEnum.A
}

// FILE: compoundAssignOk.cj
// 合法对照：内建整型的复合赋值与幂赋值都在 COMPOUND_ASSIGN_TYPE_MAP / 幂运算规则内。1.0.5 / 1.1.3 零诊断。
func trCompoundOk(): Unit {
    var n = 1
    n += 2
    var m = 2
    m **= 3
    let _ = n
    let _ = m
}

// FILE: catchUnionOk.cj
// 合法对照：catch 的联合异常类型有公共父类型（都继承 trOkExcBase）。1.0.5 / 1.1.3 零诊断。
open class trOkExcBase <: Exception {
}

class trOkExc1 <: trOkExcBase {
}

class trOkExc2 <: trOkExcBase {
}

func trCatchOk(): Int64 {
    try {
        throw trOkExc1()
    } catch (e: trOkExc1 | trOkExc2) {
        return 1
    }
}

// 已删除的形态（1.0.5 / 1.1.3 取证 kind 与 Range 见下，两版一致）：
//   1. 限定类型名的 field 不是类型 → PreCheck.cpp:497 的 QualifiedType 分支。
//      `let a: std.core.print`（即使 `import std.core.*`）→ sema_undeclared_type_name ×3（std / core / print 三处），
//      包级限定名不按类型解析；class / struct / interface / enum 体里既不能声明嵌套 class（parse_unexpected_declaration_in_scope
//      "unexpected class declaration in class body"）也不能声明嵌套 type（"unexpected type alias declaration in class body"），
//      所以 `A.B` 形态里 B 只能是非类型成员。见 enumConstructorAsType.cj（baseType 是枚举类型时该分支可达）。
//   2. `let a = Outer.Nested`（MemberAccess 形态的 sema_ref_not_be_type，TypeCheckReference.cpp:676
//      CheckStaticMemberAccessLegality）——同 1，仓颉没有嵌套类型声明，`A.B` 里 B 是类型声明的形态不可表达。
//   3. 非泛型 enum 给成员加类型实参 `E.A<Int64>` → sema_generic_argument_no_match（锚整段 `E.A<Int64>`），
//      属泛型族；generic-access/ 目录已有覆盖。
//   4. match 分支 join 失败 / if-let 两分支类型不等 → TypeCheckMatchExpr.cpp:136 / :208 的 sema_type_incompatible "MatchCase"。
//      实测（1.0.5 / 1.1.3）：`match (n) { case 1 => 1  case 2 => "s"  case _ => 0 }` 只报 case 体的 sema_mismatched_types
//      （锚 `"s"`，Range (5,19)..(5,22)），match 表达式本身不报；两个无公共父类型的 struct 臂（`case 1 => S1()  case 2 => S2()`）
//      与 struct / class 混合臂都零诊断，join 均能成功。属 match 族，不在本文件。
//   5. catch 联合类型（两个兄弟类都直接继承 Exception）→ 零诊断，join 得到 Exception；
//      只有混入 Error 分支才失败（见 catchUnionNoCommonSupertype.cj）。
//   6. `n = s`（Int64 变量 = String 变量）→ sema_mismatched_types（锚右值首字符），`n = true` → sema_cannot_convert_literal，
//      都不走 AssignExpr.cpp:393 的 sema_type_incompatible（那条要求 `Check` 失败且 `!CanSkipDiag(rightExpr)`），
//      实测未找到能只触发该报告点的写法；assignmentTypeMismatch.cj 已覆盖普通赋值不匹配。
//   7. `throw SomeStruct()`（throw 非异常类型）→ sema_throw_expr_with_wrong_type，锚 throw 的首字符，属 throw 族。
//   8. `func f(): This { return this }` → 零诊断，TypeChecker.cpp:150 的第二个 sema_mismatched_types_because 报告点
//      （CheckReturnThisInFuncBody 失败）在源码形态下未触发，只保留 TypeChecker.cpp:134 的空体形态。
//   9. `let a: Alias.A`（typealias 指向枚举后取成员作类型）→ sema_undeclared_type_name 'A'（typealias 不展开限定名的成员查找）。
