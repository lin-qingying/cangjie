// RUN_PIPELINE_TILL: FRONTEND
// 静态成员变量未初始化（static field initialization rules）语义夹具。
//
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
// `-o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
// （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断，见 HasPrevDiag）。1.1.3 本机在 CHIR/codegen 阶段
// 对某些单元会 LLVM 崩（崩在语义检查之后），语义诊断 JSON 仍完整产出，本文件按语义诊断写期望。除特别注明外，
// 两版 kind、消息、MainHint.Range 完全一致；唯一的版本差异是形态 8（static init 里未被调用的 lambda 赋值）只在 1.1.3 报本族诊断、1.0.5 零诊断（详见形态 8 段注释与已删除的形态 4）。范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**
// （单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
//
// 官方实现（external/cangjie_compiler，当前 checkout 在 tag v1.0.0）：
//   - src/Sema/LegalityOfUsage/InitializationChecker.cpp:473-516 `CheckStaticInitForTypeDecl`
//       `staticMembers` 从哪来：遍历 `id.GetMemberDeclPtrs()`，取 `TestAttr(Attribute::STATIC)` 且
//       `astKind == VAR_DECL` 的成员（**只收 VAR_DECL，不收 PROP_DECL**）——见 :477-484。
//       `staticInit` = 静态初始化器，`IsStaticInitializer`（include/cangjie/AST/Utils.h:53-56，判
//       `STATIC + CONSTRUCTOR` 两个属性）。
//       :489-500 若 static init 存在：先 CheckInitialization(staticInit)；`funcBody == nullptr`（函数被
//       打断）直接 return；函数体以 **THROW_EXPR 直接终止**（`scopeTerminationKinds[funcBody->scopeName] ==
//       THROW_EXPR`，scopeTerminationKinds 由 `UpdateScopeStatus` :300-319 记录，只对控制流表达式登记，且
//       `throw` 在 try 内（tryDepth != 0）不登记）时整段跳过 —— 全类所有静态成员都不报。
//       :501-513 遍历 staticMembers：带 `INITIALIZED` / `TOOL_ADD` 属性、或编译 `.cjd`（opts.compileCjd）则跳过；
//       其余报 `sema_type_uninitialized_static_field`，`DiagnoseRefactor(kind, *decl, decl->identifier)`
//       锚**整个 static 字段声明**（refactor kind → MakeRealRange(decl)），不是只锚标识符。
//       附注：有 static init 时 "you may add an initializer inline or initialize the variable in the 'static init'" +
//       "static init defined here"（后者锚 static init 的 identifier）；无 static init 时单条附注
//       "you may add an initializer inline or define a 'static init' to perform initialization"。
//       两版 cjc 的 MainHint 证实锚点是整条声明：例如 `    static var x: Int64` 报 Range (2,5)..(2,24)
//       （覆盖 `static var x: Int64` 共 19 个字符），带修饰符时从修饰符开始
//       （`public static var x: Int64` → (2,5)..(2,31)）。
//   - 调用点只在 **STRUCT_DECL**（:428-432 preVisiter）和 **CLASS_DECL**（:518-542 `CheckInitInClassDecl`，
//       :529 同文件父类先查、:539 本类）跑本检查；INTERFACE_DECL / EXTEND_DECL / ENUM_DECL 分支都不调用
//       （:396-427）。所以静态字段未初始化只对 class 与 struct 报，interface / extend / enum / 顶层 var 无此诊断
//       （且 interface / extend / enum 体里写 var 本来就是 parse 错误，见"已删除的形态 1/2/3"）。
//   - 变量何时算已初始化：`CheckInitialization` / `UpdateInitializationStatus`（:337-355）——static init 体里
//       对该字段的**直接赋值**（直线可达）置 INITIALIZED；被 `if` 条件、`for` / `while` 循环、`if-else` 单支
//       或**被调函数内部**包住的赋值不算（实见各段）。实例 init（`init()`）里的赋值也不算——本检查在
//       CheckInitInTypeDecl 之前跑。
//   - 文案 include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:132
//     `ERROR(sema_type_uninitialized_static_field, "the static member variable '%s' is not initialized")`。
//
// 本项目 CFIR 名：TYPE_UNINITIALIZED_STATIC_FIELD（cfir/checkers/.../diagnostics/CfirDiagnosticsList.kt:1624，
// CLASS_STRUCT 组，参数 fieldName）；生产者在 cfir/checkers/src/.../declaration/CfirInitializationCheckers.kt:2313-2326
// （`reportUninitializedStaticFields`，`factory = CfirErrors.TYPE_UNINITIALIZED_STATIC_FIELD`，
// `source = field.fieldVariableNameDiagnosticSource()`）；消息表 CfirErrorsDefaultMessages.kt:1039；
// CfirErrors.kt:407。按源码可预判的锚点差异（只写源码能直接看出的，不写 CFIR 实跑结果）：
//   CFIR 用 `fieldVariableNameDiagnosticSource()`（锚字段变量名），而本族官方锚的是**整条 static 字段声明**
//   （含 `static` 与修饰符）。因此 CFIR 侧红是预期的缺口，本文件按官方整条声明写标记。
//
// 已有覆盖不重复清单：
//   - CLASS_UNINITIALIZED_FIELD（实例成员未初始化，官方 sema_class_uninitialized_field，
//     InitializationChecker.cpp:1530-1545 `CheckInitInConstructors`，非 refactor kind，锚构造器 `init` 标识符）
//     已在 diagnostics/initialization/classUninitializedFieldRich.cj（多形态）、diagnostics2/generic/genericBoundRules.cj
//     （值类型递归段的伴随诊断，锚字段声明首字母）覆盖；本文件只在形态 23 / 24 于头部注明其伴随，不重复写标记。
//   - static var 参与 const 族的 CLASS_CONST_INIT_WITH_VAR 已在 diagnostics2/const/constFunctionRules.cj 覆盖。
//   - static init 里 return 已在 diagnostics2/static-init/returnInStaticInit.cj 覆盖。
//   - static init 里 this / super 已在 diagnostics2/static-init/thisOrSuperInStaticInit.cj 覆盖。
//   - COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT（CJMP 专有）已在
//     diagnostics2/common-specific/commonSpecificStaticLetInStaticInit.cj 覆盖（并注明官方 kind 在 1.1.3 无可达
//     触发点，被 assign-to-immutable 先行拦截），本文件不重复，只在"已删除的形态"注明。
//   - llt/class/static_or_global_var/* 是旧目录、不在 diagnostics2 的 inventory 范围；旧目录 case 3/9/10/11/12/14
//     覆盖的是"lambda 内赋值 + USED_BEFORE_INITIALIZATION"交织形态，本文件不写 USED_BEFORE_INITIALIZATION，
//     只按本族四条报告点（宿主种类、赋值可达性、THROW_EXPR 终止、static prop 不收）重排为诊断矩阵。

// ── 形态 1：class 的 static var 无初值、无 static init —— 基础形态
// kind：sema_type_uninitialized_static_field "the static member variable 'x' is not initialized"
// 锚点：整条 static 字段声明（refactor kind → MakeRealRange(decl)）。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// 附注：无 static init → "you may add an initializer inline or define a 'static init' to perform initialization"。
// FILE: classStaticVarNoInit.cj
class StaticFieldClassNoInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
}

// ── 形态 2：class 的 static let 无初值、无 static init（var / let 同一 kind、同一锚点）
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// 官方 CheckStaticInitForTypeDecl 只看 ASTKind::VAR_DECL + STATIC 属性，var / let 一视同仁。
// FILE: classStaticLetNoInit.cj
class StaticFieldClassLetNoInit {
    static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>y<!>: Int64
}

// ── 形态 3：struct 的 static var 无初值、无 static init（struct 与 class 同走本检查）
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// 官方调用点 InitializationChecker.cpp:428-432（STRUCT_DECL preVisiter）同样调 CheckStaticInitForTypeDecl。
// FILE: structStaticVarNoInit.cj
struct StaticFieldStructNoInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
}

// ── 形态 4：struct 的 static let 无初值（struct + let 交叉形态）
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// FILE: structStaticLetNoInit.cj
struct StaticFieldStructLetNoInit {
    static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>y<!>: Int64
}

// ── 形态 5：class 有 static init 但字段只在 static init 的 if 单支里赋值 —— 条件赋值不算初始化
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// 附注：有 static init → "…or initialize the variable in the 'static init'" + "static init defined here"。
// FILE: classStaticInitConditionalAssign.cj
class StaticFieldConditionalAssign {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64

    static init() {
        if (true) {
            x = 1
        }
    }
}

// ── 形态 6：class 有 static init 但字段在被调 static 函数内部赋值 —— 函数内赋值不算初始化
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// FILE: classStaticInitIndirectAssign.cj
class StaticFieldIndirectAssign {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64

    static func setIt(): Unit {
        x = 1
    }

    static init() {
        setIt()
    }
}

// ── 形态 7：class 有 static init 但字段在 for 循环里赋值 —— 循环体赋值不算初始化（循环可能不执行）
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// FILE: classStaticInitLoopAssign.cj
class StaticFieldLoopAssign {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64

    static init() {
        for (i in 0..3) {
            x = i
        }
    }
}

// ── 形态 8：class 有 static init 但字段在 static init 的 lambda 里赋值、lambda 从未调用 —— 不算初始化
// kind：sema_type_uninitialized_static_field，锚整条声明 Range (2,5)..(2,24)。
// **两版差异**：1.1.3 报本诊断；1.0.5 实测本形态**零诊断**（1.0.5 未把"lambda 内赋值但 lambda 未调用"识别为
// 未初始化）。默认语言版本为 1.1.3，故本段按 1.1.3 写标记；1.0.x 的差异在文件末"已删除的形态 4"注明。
// 旧目录 llt/class/static_or_global_var/static_or_global_var12.cj 覆盖了同族 lambda 形态但与
// USED_BEFORE_INITIALIZATION 交织，本段只聚焦"未调用 lambda 不算初始化"。
// FILE: classStaticInitLambdaNotCalled.cj
class StaticFieldLambdaNotCalled {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64

    static init() {
        let f = { => x = 1 }
        let _ = f
    }
}

// ── 形态 9：class 的 static var 在实例 init() 里赋值、无 static init —— 实例 init 不触发本检查
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// 官方 CheckStaticInitForTypeDecl 在 CheckInitInTypeDecl（含实例 init 检查）之前跑，实例 init 里的
// static 字段赋值来不及置 INITIALIZED。实例字段 y 有初值，不伴随 CLASS_UNINITIALIZED_FIELD。
// FILE: classStaticVarAssignedInInstanceInit.cj
class StaticFieldAssignedInInstanceInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
    var y: Int64 = 0

    init() {
        x = 1
    }
}

// ── 形态 10：同一 class 里两个 static 字段都未初始化 —— 每个字段一条诊断
// kind：sema_type_uninitialized_static_field × 2，锚各自整条声明。
// 1.0.5 / 1.1.3 一致 Range (2,5)..(2,24) 与 (3,5)..(3,24)。
// FILE: classTwoStaticFieldsNoInit.cj
class StaticFieldTwoNoInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64
    static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>b<!>: Int64
}

// ── 形态 11：class 有 static init 但只给另一个 static 字段赋值，未赋值的那个报错
// kind：sema_type_uninitialized_static_field，锚未赋值字段的整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// b 已由 static init 赋值，不报。
// FILE: classStaticInitAssignsOther.cj
class StaticFieldInitAssignsOther {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64
    static var b: Int64

    static init() {
        b = 1
    }
}

// ── 形态 12：class 的 static var 带 public 修饰符 —— 锚点含修饰符，从 public 起算
// kind：sema_type_uninitialized_static_field，锚整条声明（含 public）。1.0.5 / 1.1.3 一致 Range (2,5)..(2,31)。
// FILE: classStaticVarPublicNoInit.cj
class StaticFieldPublicNoInit {
    public static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
}

// ── 形态 13：同文件两个 class 各有一个未初始化 static 字段 —— 各自独立检查、各报一条
// kind：sema_type_uninitialized_static_field × 2。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24) 与 (6,5)..(6,24)。
// FILE: twoClassesEachStaticNoInit.cj
class StaticFieldTC1 {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64
}

class StaticFieldTC2 {
    static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>b<!>: Int64
}

// ── 形态 14（合法对照 A）：class 的 static var 有内联初值 —— 不报
// 1.0.5 / 1.1.3 零诊断。官方 :501 判 INITIALIZED 属性后跳过。
// FILE: classStaticVarInlineInitOk.cj
class StaticFieldInlineInitOk {
    static var x: Int64 = 0
}

// ── 形态 15（合法对照 B）：class 的 static var 在 static init 里直接赋值 —— 不报
// 1.0.5 / 1.1.3 零诊断。static init 体里对 x 的直线可达赋值置 INITIALIZED。
// FILE: classStaticInitAssignOk.cj
class StaticFieldAssignOk {
    static var x: Int64

    static init() {
        x = 1
    }
}

// ── 形态 16（合法对照 C）：struct 的 static let 在 static init 里赋值 —— 不报
// 1.0.5 / 1.1.3 零诊断。struct 的 static init 赋值与 class 同逻辑（都走 CheckStaticInitForTypeDecl）。
// FILE: structStaticInitAssignOk.cj
struct StaticFieldStructAssignOk {
    static let x: Int64

    static init() {
        x = 1
    }
}

// ── 形态 17（合法对照 D）：static init 体以 throw 直接终止 —— 整段跳过，所有静态字段都不报
// 零诊断。官方 :493-499：funcBody 以 THROW_EXPR 直接终止时整段 return。本段用两个未初始化字段（a、b）
// 证明是"整段跳过"而非"逐字段跳过"；throw 不在 try 内（try 内 tryDepth != 0 不登记终止，见 UpdateScopeStatus
// :304-306），两版一致。
// FILE: classStaticInitThrowSkipsCheck.cj
class StaticFieldThrowSkips {
    static var a: Int64
    static var b: Int64

    static init() {
        throw Exception()
    }
}

// ── 形态 18（边界：static prop 不收）：只有未初始化的 static prop —— 不报
// static prop 是 PROP_DECL，CheckStaticInitForTypeDecl :480-483 只收 ASTKind::VAR_DECL。1.0.5 / 1.1.3 零诊断。
// FILE: classStaticPropNoInitOk.cj
class StaticFieldPropNoInitOk {
    static prop q: Int64 {
        get() {
            return 0
        }
    }
}

// ── 形态 19（边界：static var 与 static prop 混存，只有 static var 报）
// kind：sema_type_uninitialized_static_field，锚 static var 整条声明；同类的 static prop 不额外报。
// 1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// FILE: classStaticVarAndPropNoInit.cj
class StaticFieldVarAndProp {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64

    static prop q: Int64 {
        get() {
            return 0
        }
    }
}

// ── 形态 20：open class 的 static var 未初始化 —— 与普通 class 同诊断
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// FILE: openClassStaticVarNoInit.cj
open class StaticFieldOpenNoInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
}

// ── 形态 21：abstract class 的 static var 未初始化 —— 与普通 class 同诊断
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// FILE: abstractClassStaticVarNoInit.cj
abstract class StaticFieldAbstractNoInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
}

// ── 形态 22：泛型 class 的 static var 未初始化（字段类型不依赖泛型参数）—— 与普通 class 同诊断
// kind：sema_type_uninitialized_static_field，锚整条声明。1.0.5 / 1.1.3 一致 Range (2,5)..(2,24)。
// 字段类型若取泛型参数 T 会先触发 sema_static_variable_use_generic_parameter（见"已删除的形态 5"），
// 本段刻意用 Int64 排除那条。
// FILE: genericClassStaticVarNoInit.cj
class StaticFieldGenericNoInit<T> {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
}

// ── 形态 23：子类继承含未初始化 static 字段的父类 —— 伴随一条 CLASS_UNINITIALIZED_FIELD（不写标记）
// kind：sema_type_uninitialized_static_field，锚父类 static var 整条声明 Range (2,5)..(2,24)（两版一致）。
// **伴随诊断不写标记**：父类的 static var 也被子类的隐式构造器算作未初始化实例字段，附带一条
// sema_class_uninitialized_field（CFIR 名 CLASS_UNINITIALIZED_FIELD，官方 CheckInitInConstructors :1530-1545，
// 此处锚在父类 static 声明首字符 (2,5)..(2,6)）。该 kind 已在 diagnostics/initialization/classUninitializedFieldRich.cj
// 与 diagnostics2/generic/genericBoundRules.cj 覆盖，本文件不重复。
// FILE: classInheritsStaticFieldNoInit.cj
open class StaticFieldSuperNoInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>x<!>: Int64
}

class StaticFieldSubInherits <: StaticFieldSuperNoInit {
}

// ── 形态 24：class 同时有未初始化 static 字段与未初始化实例字段 —— 伴随 CLASS_UNINITIALIZED_FIELD（不写标记）
// kind：sema_type_uninitialized_static_field，锚 static var 整条声明 Range (2,5)..(2,24)（两版一致）。
// **伴随诊断不写标记**：实例字段 b 未初始化额外报 sema_class_uninitialized_field（锚实例构造器 `init`，
// Range (8,5)..(8,6)），已在 diagnostics/initialization/classUninitializedFieldRich.cj 覆盖，本文件不重复。
// FILE: classStaticAndInstanceFieldNoInit.cj
class StaticFieldAndInstanceNoInit {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64
    var b: Int64

    static init() {
    }

    init() {
    }
}

// 已删除的形态（1.0.5 / 1.1.3 取证 kind 与 Range 见下，未注明"两版一致"者为已核对两版相同）：
//   1. interface / extend / enum 体里写 static 字段
//      → 报 parse 错误而非 sema_type_uninitialized_static_field：
//        interface `static var x: Int64` → parse_unexpected_declaration_in_scope "unexpected variable declaration in
//        interface body"（锚 `var`，Range (2,12)..(2,15)，两版一致）；extend 体同形（锚 `var` (6,12)..(6,15)）；
//        enum 体 → "unexpected variable declaration in enum body"（锚 `var` (5,12)..(5,15)，两版一致）。
//        官方 CheckStaticInitForTypeDecl 只对 STRUCT_DECL / CLASS_DECL 调用（:428-432 / :518-542），
//        interface / extend / enum 分支不调（:396-427），写不出可达的静态字段未初始化形态，故不写这些段。
//   2. 顶层 var / let 无初值
//      → parse_var_must_be_initialized "variable in top-level scope must be initialized "（锚行尾 (1,14)..(1,15)，
//        两版一致），不是本族诊断。顶层变量无宿主 InheritableDecl，不走 CheckStaticInitForTypeDecl，且语法上
//        就要求必须内联初始化。
//   3. static init 里同一字段既内联初值又条件赋值 / lambda 赋值（static var x: Int64 = 0 + static init { if … x = 1 }）
//      → 内联初值已置 INITIALIZED，条件赋值不改变结论，两版零诊断（可由形态 1 + 形态 5 的语义合并推出，
//        未单独取形态）。
//   4. 形态 8（static init 的 lambda 里赋值但从未调用）在 1.0.x 语言版本
//      → 1.0.5 实测**零诊断**，1.1.3 报本诊断 Range (2,5)..(2,24)，是本族唯一的版本差异。默认语言版本
//        （LATEST_STABLE = 1.1.3）的期望已写在形态 8；1.0.5 无诊断可标，故不为它单独拆
//        `LANGUAGE_VERSION: 1.0.0` 夹具，差异在此注明。
//   5. static 字段类型依赖泛型参数（class G<T> { static var x: T }）
//      → 先报 sema_static_variable_use_generic_parameter "static member cannot depend on generic parameter 'T'"
//        （锚 `T` 单字符 Range (2,19)..(2,20)，附注 "generic argument declaration here"，两版一致），**同时**报
//        sema_type_uninitialized_static_field（锚整条声明 Range (2,5)..(2,20)，两版一致）。前者属泛型参数族，
//        与本族交织；本文件用类型为 Int64 的泛型 class（形态 22）代表"泛型 class 里的普通 static 字段"，
//        不取这条交织形态。
//   6. 一个 static 声明语句里并列多个变量（static var a: Int64, b: Int64）
//      → parse_expected_character "expected ';' or '<NL>', found ','"（锚 `,` Range (2,24)..(2,25)，两版一致）。
//        仓颉变量声明不支持逗号并列，静态字段只能一条一语句，"多个未初始化字段"改由形态 10 的两条语句表达。
//   7. 一个 class 里写两个 static init
//      → sema_overload_conflicts "static constructor 'init' has overload conflicts"（锚第二个 `init`
//        Range (8,12)..(8,16)，两版一致），不是本族诊断；static init 至多一个（官方 CheckStaticInitForTypeDecl
//        也只保留最后一个 staticInit）。
//   8. class 字段里嵌套 class（class Outer { class Inner { static var x: Int64 } }）
//      → parse_unexpected_declaration_in_scope "unexpected class declaration in class body"（锚 `class`
//        Range (2,5)..(2,10)，两版一致）；函数体内局部 class 同样不可声明（"unexpected class declaration in
//        function body"，锚 `class` (2,5)..(2,10)，两版一致）。静态字段的嵌套宿主形态写不出。
//   9. static init 里 if / else 两支都赋值（static init { if (c) { x = 1 } else { x = 2 } }）
//      → 两版均**不报** sema_type_uninitialized_static_field（两分支都赋值即算已初始化）；1.0.5 另有一条
//        codegen 阶段的 chir_dce_unreachable_block_in_expression warning "unreachable block in 'if' expression"
//        （锚 else 块 (7,16)..(9,10)，常量 true 使 else 不可达），1.1.3 无该 warning。因 1.0.5 会多一条与本族
//        无关的 warning，不单列成段以免引入噪声，事实记录于此。
//  10. static init 里嵌套裸块赋值（static init { { x = 1 } }）
//      → 两版均报 parse_expected_double_arrow_in_lambda "expected '=>' in lambda expression, found 'x'"
//        （锚 `x` Range (6,13)..(6,14)）：仓颉把 `{ … }` 解析为 lambda 字面量，裸块不可表达，故"嵌套块内直线
//        赋值"形态写不出。
//  11. static init 的函数体被打断（static init 缺右花括号等 parse 错误）+ 未初始化 static 字段
//      → 官方 :490-492 `staticInit->funcBody == nullptr` 时直接 return（静态字段不报），但整包编译里一处 parse
//        错误会通过 HasPrevDiag 吞掉其余诊断（见头部取证说明），故本文件不取"parse 错误 + 语义诊断并存"的形态。
