// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5（准）与 1.1.3 两版实测，静态库模式
// `-o out.a --output-type=staticlib --diagnostic-format json`（1.1.3 在本机 codegen 阶段可能 LLVM 崩，
// 只取它的解析 / 语义诊断）。除特别注明外，两版 kind、消息、Range 完全一致。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
//
// 官方实现：
//   - src/Sema/TypeCheckAccess.cpp:389-445 CheckLetInstanceAccessMutableFunc
//     （沿接收者链上溯，链上无 let / 不可变 prop 且 ma.callOrPattern == nullptr → sema_use_mutable_func_alone，
//      锚 ma.field，即成员名；附注 "'x' is a mutable funciton"（官方自带拼写错误））；
//     src/Sema/TypeCheckAccess.cpp:325-350 CheckMutationInStruct → :91-133 CheckMutationInStructNonMut
//     （非 mut 函数里改 struct 值类型实例成员 → sema_cannot_modify_var，锚整条赋值表达式；
//      this.x = ... 走 RefExpr isThis 分支；class 类型字段不触发）→ CFIR CANNOT_MODIFY_VAR。
//     src/Sema/Diags.cpp:333-350 DiagImmutableAccessMutableFunc
//     （锚 *ma.baseExpr，即接收者；hint "is immutable"，let/const 接收者附注 "'s' is a variable declared with 'let'"）。
//   - src/Sema/TypeCheckExpr.cpp:143-168 CanTargetOfRefBeCapturedCaseNominalDecl
//     （可继承类构造器里的 lambda / 内部函数捕获 this 或成员 → sema_illegal_capture_this；
//      同一函数里 decl.IN_STRUCT 的分支是 struct 构造器的对应形态）→ CFIR ILLEGAL_CAPTURE_THIS。
//   - src/Sema/TypeCheckExpr.cpp:170-192 CanTargetOfRefBeCapturedCaseMutFunc
//     （mut 函数里的 lambda / 内部函数捕获 this 或 struct 字段、finalizer 里捕获成员函数 / 属性
//      → sema_capture_this_or_instance_field_in_func）；
//     src/Sema/TypeCheckReference.cpp:150-210 CheckUsageOfThis
//     （lambda / 内部函数里的 `this` RefExpr 走 CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC，锚 `this` 首字符；
//      `this` 作为独立表达式（re.isAlone）→ sema_use_this_as_an_expression_in_func）。
//   - src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1261-1282 CheckMutModifierCompatible
//     （接口继承路径调用点 :821；struct 或 struct 的 extend 实现接口成员时 mut 不一致
//      → sema_incompatible_mut_modifier_between_struct_and_interface，锚子声明首字符）→ CFIR
//     INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE。
//   - 文案 include/cangjie/Basic/DiagnosticSema.def:245（sema_cannot_modify_var）、:246（sema_illegal_capture_this）、
//     :287（sema_capture_this_or_instance_field_in_func）、:288（sema_use_this_as_an_expression_in_func）、
//     :289-290（sema_incompatible_mut_modifier_between_struct_and_interface）、
//     :291（sema_immutable_function_cannot_access_mutable_function）；
//     include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:42（sema_use_mutable_func_alone）、
//     :45（sema_immutable_access_mutable_func，"cannot use mutable function on immutable value"）、
//     :133（sema_instance_func_cannot_be_used_in_finalizer）、:181（sema_inout_modify_heap_variable）。
// CFIR 侧对应名（CfirErrors.kt:162-163、:322、:389、:1629；生产者 CfirFunctionReferenceLegalityChecker.kt:32-77 /
// CfirMutabilityCheckers.kt:177-225 / CfirInheritanceDeepChecker.kt:398-424）：
//   USE_MUTABLE_FUNC_ALONE 锚被引用的成员名（与官方 ma.field 一致）；
//   ILLEGAL_CAPTURE_THIS / CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC 锚 `this`（隐式接收者时回落到字段名，与官方 nre 一致）；
//   INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE 锚实现成员声明（extend 内优先锚 `public` 修饰符，其首字符即声明首字符）。
// 范围规则（1.0.5 / 1.1.3 实测两版完全一致）：除 sema_use_mutable_func_alone 用 `MakeRange(ma.field)` 报成员名整段
// （`inc`，3 个字符）外，本文件涉及的官方 kind 都经 `diag.Diagnose(pos)` / `diag.Diagnose(node, kind)` 报在单个字符上
//（NameReferenceExpr 首字母、成员声明首字母、成员名首字母）；1.1.3 的 `Diagnose(node, kind)` 在含带参构造器的枚举等
// 形态上覆盖整条声明，本文件的 kind 未触发该差异。

// FILE: structConstructorLambdaCaptureThis.cj
// 形态：struct 构造器 `init()` 里定义 lambda 并引用实例成员。
// kind：sema_illegal_capture_this "'this' is not allowed to be captured in constructor of struct"
// 锚点：NameReferenceExpr 首字符，即 MemberAccess 里的 `this`（`CanTargetOfRefBeCaptured` 传 nre = field 的 MemberAccess）。
// 1.0.5 / 1.1.3 一致：var 字段与 let 字段各 1 条，Range 均为 `this` 单字符。
// 内部函数（`init()` 里 `func` 声明）形态见 structConstructorNestedFuncCaptureThis.cj。
struct CtorLambdaCaptureStruct {
    var value: Int64 = 0
    let tag: Int64 = 1

    init() {
        let g = { => <!ILLEGAL_CAPTURE_THIS!>t<!>his.value }
        let h = { => <!ILLEGAL_CAPTURE_THIS!>t<!>his.tag }
        let _ = g
        let _ = h
    }
}

// FILE: structConstructorNestedFuncCaptureThis.cj
// 形态：struct 构造器里定义内部函数并引用实例成员。
// kind：sema_illegal_capture_this（消息同上，struct 构造器）
// 锚点：内部函数体里 MemberAccess 的 `this`。1.0.5 / 1.1.3 一致，Range 为 `this` 单字符。
struct CtorNestedFuncStruct {
    var value: Int64 = 0

    init() {
        func inner(): Int64 {
            return <!ILLEGAL_CAPTURE_THIS!>t<!>his.value
        }
        let _ = inner()
    }
}

// FILE: structMemberFuncLambdaCaptureThisOk.cj
// 合法对照：struct 的普通成员函数里 lambda 捕获 `this` / 字段合法——只有构造器被禁止
// （`CheckUsageOfThis` 的 referenceInStruct 分支要求 outMostFunc 是构造器）。1.0.5 / 1.1.3 零诊断。
struct MemberFuncLambdaOkStruct {
    var value: Int64 = 0

    func maker(): () -> Int64 {
        return { => this.value }
    }
}

// FILE: classConstructorNestedFuncCaptureThisOk.cj
// 合法对照：普通（非 open / abstract）class 构造器里的内部函数捕获 `this` / 成员合法
// （`CanTargetOfRefBeCapturedCaseNominalDecl` 只对 IN_STRUCT 或可继承类报错）。1.0.5 / 1.1.3 零诊断。
class CtorNestedFuncClass {
    var value: Int64 = 0

    init() {
        func inner(): Int64 {
            return this.value
        }
        let _ = inner()
    }
}

// FILE: openClassConstructorLambdaCaptureThis.cj
// 形态：open class 构造器里的 lambda 捕获实例成员。
// kind：sema_illegal_capture_this "'this' is not allowed to be captured in constructor of inheritable class"
// 锚点：`this` 首字符。1.0.5 / 1.1.3 一致，Range 为 `this` 单字符。
open class OC {
    var value: Int64 = 0

    init() {
        let g = { => <!ILLEGAL_CAPTURE_THIS!>t<!>his.value }
        let _ = g
    }
}

// FILE: abstractClassConstructorLambdaCaptureThis.cj
// 形态：abstract class 构造器里的 lambda 捕获实例成员，与 open class 同一条诊断。
// kind：sema_illegal_capture_this "…constructor of inheritable class"，锚 `this` 首字符。1.0.5 / 1.1.3 一致。
abstract class AC {
    var value: Int64 = 0

    init() {
        let g = { => <!ILLEGAL_CAPTURE_THIS!>t<!>his.value }
        let _ = g
    }
}

// FILE: classMemberFuncLambdaCaptureThisOk.cj
// 合法对照：class 的普通成员函数里 lambda 捕获 `this` / 字段合法（finalizer 除外，见 classFinalizer* 段）。
// 1.0.5 / 1.1.3 零诊断。
class MemberFuncLambdaOkClass {
    var value: Int64 = 0

    func maker(): () -> Int64 {
        return { => this.value }
    }
}

// FILE: mutStructFuncLambdaCaptureThis.cj
// 形态：struct 的 mut 成员函数里 lambda 捕获 `this`。
// kind：sema_capture_this_or_instance_field_in_func "'this' cannot be captured in the mutable function 'maker'"
// 锚点：`this` 首字符（`CheckUsageOfThis`，outMostFunc 带 MUT 且 curFuncBody.funcDecl != outMostFunc）。
// 1.0.5 / 1.1.3 一致，Range 为 `this` 单字符。
struct MutLambdaThisStruct {
    var value: Int64 = 0

    mut func maker(): () -> Int64 {
        return { => <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.value }
    }
}

// FILE: mutStructFuncLambdaCaptureField.cj
// 形态：struct 的 mut 成员函数里 lambda 直接引用字段（隐式 this，无 `this.` 前缀）。
// kind：sema_capture_this_or_instance_field_in_func "'value' cannot be captured in the mutable function 'inc'"
// 锚点：字段名（`CanTargetOfRefBeCapturedCaseMutFunc`，nre 是字段的 RefExpr）。1.0.5 / 1.1.3 一致。
struct MutLambdaFieldStruct {
    var value: Int64 = 0

    mut func inc(): Unit {
        let g = { => <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>v<!>alue }
        let _ = g
    }
}

// FILE: mutStructFuncNestedFuncCaptureField.cj
// 形态：struct 的 mut 成员函数里定义内部函数并引用字段。
// kind：sema_capture_this_or_instance_field_in_func "'value' cannot be captured in the mutable function 'inc'"，
// 锚字段名。1.0.5 / 1.1.3 一致。
struct MutNestedFuncFieldStruct {
    var value: Int64 = 0

    mut func inc(): Unit {
        func inner(): Int64 {
            return <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>v<!>alue
        }
        let _ = inner()
    }
}

// FILE: mutStructFuncNestedFuncCaptureThis.cj
// 形态：struct 的 mut 成员函数里定义内部函数并通过 `this.` 引用字段。
// kind：sema_capture_this_or_instance_field_in_func "'this' cannot be captured in the mutable function 'inc'"，
// 锚 `this` 首字符。1.0.5 / 1.1.3 一致。
struct MutNestedFuncThisStruct {
    var value: Int64 = 0

    mut func inc(): Unit {
        func inner(): Int64 {
            return <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.value
        }
        let _ = inner()
    }
}

// FILE: mutStructFuncLocalCaptureOk.cj
// 合法对照：mut 成员函数里 lambda 捕获局部 let 变量合法（`CanTargetOfRefBeCapturedCaseMutFunc` 只看
// IN_STRUCT 的 VAR_DECL 与 `this`）。1.0.5 / 1.1.3 零诊断。
struct MutLocalOkStruct {
    var value: Int64 = 0

    mut func inc(): Unit {
        let t = 1
        let g = { => t }
        let _ = g
    }
}

// FILE: classFinalizerCaptureFieldOk.cj
// 合法对照：class 析构函数（`~init`）里 lambda 捕获实例字段合法
// （`CanTargetOfRefBeCapturedCaseMutFunc` 的 finalizer 分支只看 FUNC_DECL / PROP_DECL，字段不触发）。
// 1.0.5 / 1.1.3 零诊断。（1.0.5 静态库 codegen 对含 finalizer 且无错误诊断的单元会 LLVM 崩，只影响取证的链接阶段。）
class FinalizerFieldOkClass {
    var value: Int64 = 0
    let tag: Int64 = 1
    var sink: Int64 = 0

    ~init() {
        let g = { => this.value }
        let h = { => this.tag }
        this.sink = 1
        let _ = g
        let _ = h
    }
}

// FILE: classFinalizerCaptureMemberFunc.cj
// 形态：class 析构函数里 lambda 捕获实例成员函数。
// kind：sema_capture_this_or_instance_field_in_func "'read' cannot be captured in the finalizer"
//      + sema_instance_func_cannot_be_used_in_finalizer "instance function cannot be used in the finalizer"
// 锚点（两版一致，各 1 个字符）：CAPTURE… 来自 CanTargetOfRefBeCaptured 收到的 NameReferenceExpr（`this` 首字母）；
//      INSTANCE_FUNC… 来自 CheckForbiddenFuncReferenceAccess（src/Sema/TypeCheckExpr.cpp:210-232，调用点
//      src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:1080 传 ma.field.Begin()），落在成员名首字母 `r`。
class FinalizerMemberFuncClass {
    var value: Int64 = 0

    public func read(): Int64 {
        return value
    }

    ~init() {
        let k = { => <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.<!INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER!>r<!>ead }
    }
}

// FILE: classFinalizerThisAlone.cj
/*
SUGGESTED_DIAGNOSTIC: USE_THIS_AS_AN_EXPRESSION_IN_FUNC
SUGGESTED_MESSAGE: 'this' cannot be used as an expression in the %s
SOURCE: src/Sema/TypeCheckReference.cpp:176-188（CheckUsageOfThis）；include/cangjie/Basic/DiagnosticSema.def:289
*/
// 形态：class 析构函数里把 `this` 当作独立表达式使用（不是 `this(...)` 调用、也不是 `this.x` 的基表达式）。
// kind：sema_use_this_as_an_expression_in_func "'this' cannot be used as an expression in the finalizer"
// 锚点：`this` 首字符。1.0.5 / 1.1.3 一致。CheckUsageOfThis 在 CfirErrors.kt 里没有同名诊断，按
// SUGGESTED_DIAGNOSTIC 处理：本段不写内联断言。同一检查在 mut 函数、可继承类构造器里同样成立
// （消息分别为 "mutable function 'f'" 与 "constructor of open/abstract class"）。
class FinalizerThisAloneClass {
    var value: Int64 = 0
    var sink: Int64 = 0

    ~init() {
        let t = this
        this.sink = 1
        let _ = t
    }
}

// FILE: useMutableFuncAloneOnThis.cj
// 形态：struct 成员函数里把 `mut` 成员函数当值引用（ma.callOrPattern == nullptr）。
// kind：sema_use_mutable_func_alone "mutable function 'inc' cannot be used alone as reference"，
// 附注 "'inc' is a mutable funciton"（官方拼写），锚点 ma.field 即成员名 `inc`（3 字符，标记只包住 `inc`，`this.` / `s.` 在标记外）。
// 1.0.5 / 1.1.3 一致，Range 均为 `inc` 的 3 个字符。
// 同一个 `this.inc` 写在非 mut 成员函数里，官方还会多报 sema_immutable_function_cannot_access_mutable_function
// （锚 `this` 首字符，1.0.5 实测），该名字已由 immutableFunctionRestrictions.cj 覆盖。
struct MutableFuncAloneThisStruct {
    var value: Int64 = 0

    mut func inc(): Unit {
        value = 1
    }

    mut func hold(): Unit {
        let f = this.<!USE_MUTABLE_FUNC_ALONE!>inc<!>
        inc()
        let _ = f
    }
}

// FILE: useMutableFuncAloneOnVarReceiver.cj
// 形态：struct 接收者是 var 局部变量时，`s.inc`（赋值、带类型的赋值、作为函数实参）都只报
// sema_use_mutable_func_alone，锚成员名 `inc`（标记只包住 `inc`，`s.` 在标记外）；接收者 `s` 是 var（可变），不触发
// sema_immutable_access_mutable_func。1.0.5 / 1.1.3 一致，Range 均为 `inc` 的 3 个字符。
// 官方条件（CheckLetInstanceAccessMutableFunc）：沿接收者链上溯，只有链上出现 let / const / 只读 prop
// 或值类型临时量才改报 sema_immutable_access_mutable_func（锚接收者）；struct 形参与 let 局部都属
// immutable 接收者，见文件末"已删除的形态 1"。
func applyInt(f: (Int64) -> Int64, v: Int64): Int64 {
    return f(v)
}

struct MutableFuncAloneVarStruct {
    var value: Int64 = 0

    mut func inc(step: Int64): Int64 {
        value = value + step
        return value
    }
}

func varReceiver(): Unit {
    var s = MutableFuncAloneVarStruct()
    s.inc(1)
    let f = s.<!USE_MUTABLE_FUNC_ALONE!>inc<!>
    let g: (Int64) -> Int64 = s.<!USE_MUTABLE_FUNC_ALONE!>inc<!>
    applyInt(s.<!USE_MUTABLE_FUNC_ALONE!>inc<!>, 2)
    let _ = f
    let _ = g
}

// FILE: structInterfaceMutModifierMismatch.cj
// 形态：struct 实现接口的 mut 成员函数时漏写 mut（接口 `mut func f`，实现写成非 mut `func f`）。
// kind：sema_incompatible_mut_modifier_between_struct_and_interface
//       "'mut' modifier of 'f' is incompatible with that in interface 'StructMutMismatchIMut'"
// 锚点：实现成员声明首字符（`public` 的 `p`，`diag.Diagnose(decl, kind, ...)` 的 decl 是实现成员）。1.0.5 / 1.1.3 一致，1 个字符。
// 实现成员必须写 `public`：接口成员隐式 public，实现成员不写 public 会额外报
// sema_weak_visibility（锚成员名 `f`），那是可见性诊断，不属本文件范围。
interface StructMutMismatchIMut {
    mut func f(): Unit
}

struct StructMutMismatchStruct <: StructMutMismatchIMut {
    var value: Int64 = 0

    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic func f(): Unit {
        let _ = value
    }
}

// FILE: structInterfaceMutModifierMismatchOtherDirection.cj
// 形态：反方向——接口 `func g`（非 mut），struct 实现写成 `mut func g`。
// kind：sema_incompatible_mut_modifier_between_struct_and_interface
//       "'mut' modifier of 'g' is incompatible with that in interface 'StructMutOtherIImm'"，锚 `g` 首字符。1.0.5 / 1.1.3 一致。
interface StructMutOtherIImm {
    func g(): Unit
}

struct T <: StructMutOtherIImm {
    var value: Int64 = 0

    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func g(): Unit {
        this.value = 1
    }
}

// FILE: structInterfaceMutModifierConsistentOk.cj
// 合法对照：struct 同时实现 mut 与非 mut 两个接口，实现成员的 mut 与接口一致，零诊断（1.0.5 / 1.1.3）。
// （对照写法不能在被实现的非 mut 成员里改字段，否则先命中 sema_cannot_modify_var。）
interface StructMutOkIMut {
    mut func f(): Unit
}

interface StructMutOkIImm {
    func g(): Unit
}

struct U <: StructMutOkIMut & StructMutOkIImm {
    var value: Int64 = 0

    public mut func f(): Unit {
        this.value = 1
    }

    public func g(): Unit {
        let _ = value
    }
}

// FILE: extendStructInterfaceMutModifierMismatch.cj
// 形态：struct 的 extend 块（`extend ExtendMutMismatchStruct <: ExtendMutMismatchIMut`）里实现接口成员时漏写 mut。
// CheckMutModifierCompatible 对 child.outerDecl 是 EXTEND_DECL 且 `ed->ty->IsStruct()` 的路径同样生效。
// kind：sema_incompatible_mut_modifier_between_struct_and_interface，锚 extend 内实现成员首字符。1.0.5 / 1.1.3 一致。
interface ExtendMutMismatchIMut {
    mut func f(): Unit
}

struct ExtendMutMismatchStruct {
    var value: Int64 = 0

    public mut func setValue(): Unit {
        this.value = 1
    }
}

extend ExtendMutMismatchStruct <: ExtendMutMismatchIMut {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic func f(): Unit {
        let _ = value
    }
}

// FILE: extendStructInterfaceMutModifierMismatchOtherDirection.cj
// 形态：反方向——extend 里给非 mut 接口成员加 mut。
// kind：sema_incompatible_mut_modifier_between_struct_and_interface，锚 extend 内 `g` 首字符。1.0.5 / 1.1.3 一致。
interface ExtendMutOtherIImm {
    func g(): Unit
}

struct ExtendMutOtherStruct {
    var value: Int64 = 0

    public mut func setValue(): Unit {
        this.value = 1
    }
}

extend ExtendMutOtherStruct <: ExtendMutOtherIImm {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func g(): Unit {
        this.value = 1
    }
}

// FILE: extendStructInterfaceMutModifierConsistentOk.cj
// 合法对照：extend 块里实现接口成员时 mut 与接口一致，零诊断（1.0.5 / 1.1.3）。
// 同一 struct 上多个 extend 块不能重复实现同一接口（sema_extend_duplicate_interface），
// 也不能让后一个 extend 遮蔽前一个的成员（sema_extend_member_cannot_shadow），故两个方向各用不同的接口 / 成员名。
interface ExtendMutOkIMut {
    mut func f(): Unit
}

interface ExtendMutOkIImm {
    func g(): Unit
}

struct ExtendMutOkStruct {
    var value: Int64 = 0

    public mut func setValue(): Unit {
        this.value = 1
    }
}

extend ExtendMutOkStruct <: ExtendMutOkIMut {
    public mut func f(): Unit {
        this.setValue()
    }
}

extend ExtendMutOkStruct <: ExtendMutOkIImm {
    public func g(): Unit {
        let _ = value
    }
}

// 已删除的形态（1.0.5 / 1.1.3 取证 kind 与 Range 见下，两版一致）：
//   1. `func f(s: S) { let g = s.inc }`（struct 形参）、`let s = S(); let g = s.inc`、
//      `let g: (Int64) -> Int64 = s.inc`、`applyInt(s.inc, 1)`、`let g = S().inc`
//      → 官方只报 sema_immutable_access_mutable_func "cannot use mutable function on immutable value"，
//        hint "is immutable"；形参 / let 局部附注 "'s' is a variable declared with 'let'"；锚接收者 `s`（单字符）。
//        `S().inc` 的临时量锚整个 `S()`（3 字符）。1.0.5 实测：形参形态三处引用各 1 条（Range 均为 `s` 单字符）；
//        `var s = S(); let f = s.inc` → sema_use_mutable_func_alone @ `inc`（Range (12,15)..(12,18)），
//        `let g = S().inc` → sema_immutable_access_mutable_func @ `S()`（Range (12,13)..(12,16)）。
//        CheckLetInstanceAccessMutableFunc 在接收者是 let / const / 只读 prop / 值类型临时量时先走
//        DiagImmutableAccessMutableFunc 并 return，根本到不了 sema_use_mutable_func_alone 分支。
//        CFIR 侧对应的 IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION（锚接收者）已由
//        immutableFunctionRestrictions.cj 的 `let s = S(); s.set(1)` 覆盖，故不重复。
//   2. class 体里的 `mut func`（`class C { public mut func inc(): Unit { … } }`，包括在 finalizer 里
//      捕获 mut 成员函数的写法）→ parse_illegal_modifier_in_scope "unexpected modifier 'mut' on function
//      declaration in class body"（锚 `mut`；1.0.5 Range (4,12)..(4,15)，1.1.3 同）。
//      class 成员函数不带 mut，所以"class 里的 mut 函数"没有官方语义检查。
//   3. class / struct 里的"函数字面量"写法 `let k = func(): Int64 => this.value`
//      → parse_expected_expression "expected expression after '=', found keyword 'func'" 等 4 条解析错误
//        （1.0.5 Range (6,17)..(6,21) 等），仓颉没有这种字面量语法；内部函数必须写成 `func name() { … }` 声明。
//   4. struct 里的析构函数 `~init() { … }` → parse_unexpected_declaration_in_scope "unexpected finalizer in
//      struct body"（锚 `~init`，Range (4,5)..(4,10)）。CanTargetOfRefBeCapturedCaseMutFunc 的 finalizer 分支
//      在 struct 上不可达，finalizer 形态只在 class 段覆盖。
//   5. class 析构函数里把 mut 成员函数当值引用 `let k = { => this.set }` → 先被
//      parse_illegal_modifier_in_scope 挡住（见 2）；非 mut 成员函数的同一写法见 classFinalizerCaptureMemberFunc.cj。
//   6. `let f = this.value`（class 成员函数里把字段当值引用）→ 官方零诊断（1.0.5 实测），
//      只有 lambda / 内部函数里才触发捕获检查。
//   7. struct 里的非 mut 成员函数实现接口 mut 成员且不写 `public`
//      → 额外报 sema_weak_visibility "a deriving member must be at least as visible as its base member"
//        （锚成员名 `f`，hint "the visibility of the deriving 'f' is 'internal'"，附注 base 'f' 是 public）。
//        属可见性诊断（`visibility/` 域），本文件一律给实现成员写 `public` 以排除。
//   8. `return { => this.value }`（struct 非 mut 成员函数返回捕获 this 的 lambda）→ 官方零诊断（1.0.5 实测），
//      structConstructorLambdaCaptureThis 只在构造器里成立；该探针里唯一的诊断是 sema_weak_visibility（见 7）。
//   9. 在 `init()` / `~init()` / lambda 之外（如 struct 字段初始化器）引用 `this` → 官方报
//      sema_illegal_this_outside_struct_constructor（`CheckUsageOfThis:199-202`），本项目无对应名字，与本文件
//      的四个诊断无关，未取形态。
