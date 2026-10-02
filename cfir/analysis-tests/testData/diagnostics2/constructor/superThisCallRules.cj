// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5（准）与 1.1.3 两版实测，静态库模式
// `-o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译（官方整包编译时一处 parse
// 错误会按"首个错误类别"过滤掉其余段的语义诊断，官方 DiagnosticEngine.cpp CanBeEmitted）。两版对本文全部 23 段（17 个非法形态 + 6 个合法对照，18 个内联标记）的
// kind、文案、Range 完全一致，所以不拆 LANGUAGE_VERSION 夹具。1.1.3 本机在 CHIR/codegen 阶段可能 LLVM 崩，崩在语义
// 检查之后，语义诊断 JSON 仍完整产出。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
// 官方锚点口径（include/cangjie/Basic/DiagnosticEngine.h）：非 refactor 的 `Diagnose(node, kind)` 退化为
// `Diagnose(node.GetBegin(), kind)`（:844-857），而 `Diagnose(pos, kind)` 是 `pos..pos+1`（:822-824），所以
// sema_illegal_place_of_calling_this_or_super / sema_illegal_place_of_calling_this_primary_constructor
// （`Diagnose(*ce, …)`，ce 是 this()/super() 调用表达式）锚在关键字首字母 **1 个字符**，不是整条 `super(a)`；
// refactor kind 走 `DiagnoseRefactor(kind, node, range)`（:932-945）用显式 Range：sema_extend_use_super 传 re 整段，
// 锚 `super` **5 个字符**；sema_no_non_param_constructor_in_super_class 传 `MakeRange(fd.identifier.Begin(), fdName)`。
//
// 官方实现（`external/cangjie_compiler`，checkout 在 tag v1.0.0）：
//   - src/Sema/TypeChecker.cpp:1415-1455 CheckCallsInConstructor（遍历构造器体内所有 CallExpr，取 baseFunc 是
//     isThis / isSuper 的那些）：
//     :1436  `fd.TestAttr(PRIMARY_CONSTRUCTOR)` 且 re.isThis → sema_illegal_place_of_calling_this_primary_constructor
//            （`Diagnose(*ce)` → 锚 `this` 1 个字符；先于位置检查，主构造器里的任何 this(...) 都走这一条）
//     :1444  `ce != firstExprOrDeclPtr` → sema_illegal_place_of_calling_this_or_super（`Diagnose(*ce, …, ref.identifier.Val(),
//             isClass ? "class" : "struct", decl.identifier.Val())` → 锚 `super` / `this` 1 个字符）。
//            firstExprOrDecl 是构造器体的第一条语句：TrailingClosureExpr 本身算首条，其内部 desugar 的调用算合法。
//     :1640-1657 CheckSuperCall（仅对编译器合成的 `super()`，即 ce 带 COMPILER_ADD 且实参为空、宿主是 class）：
//             父类 `HasNonParamCtorForClass` 为假 → `DiagnoseRefactor(sema_no_non_param_constructor_in_super_class, fd, range)`，
//             :1655 `fd.TestAttr(IMPLICIT_ADD) ? MakeRange(decl.identifier) : MakeRange(fd.identifier.Begin(), fdName)`：
//             编译器补出的构造器锚**宿主类名**，用户写的 `init` 锚 `init`（4 个字符），主构造器锚**整条签名**（fdName 取
//             identifierForLsp，即 `C(var z!: Int64 = 3)` 这样的全签名）。显式 `super(...)` 不是 COMPILER_ADD，不走这里。
//     :1681-1691 CheckPrimaryCtorForClassOrStruct：第二个及以后的主构造器 `Diagnose(*fd, sema_multiple_primary_constructors, …)`
//             → 锚主构造器声明首字符（`public` 的 p，1 个字符）。
//   - src/Sema/TypeCheckCall.cpp:2289-2305 CheckRefConstructor：this()/super() 调用所在函数体不是构造器
//     → sema_invalid_this_call_outside_ctor（`Diagnose(ce, …, ref.identifier.Val())` → 锚关键字 1 个字符；
//     文案里的 %s 是 `this` 或 `super`，两种调用共用这一条）。
//   - src/Sema/TypeCheckReference.cpp:150-209 CheckUsageOfThis：
//     :187  re.isAlone（裸 `this` 作表达式）且最外层函数带 mut / 是析构函数 / 是 open|abstract 类构造器
//           → sema_use_this_as_an_expression_in_func（`Diagnose(re, …)` → 锚 `this` 1 个字符）。
//           `this.x` 的接收者与 `this(...)` 都不算 isAlone；普通（非 open/abstract）类构造器里的裸 this 合法。
//     :201  struct 里非构造器函数体 / 字段初始化器（GetCurFuncBody 为空）中的 this → sema_illegal_this_outside_struct_constructor。
//   - src/Sema/TypeCheckExpr.cpp:222 open/abstract 类构造器体里访问实例成员函数/属性
//           → sema_illegal_member_used_in_open_constructor（`Diagnose(fd, pos, …)`，pos 取成员名首字符 → 1 个字符）。
//   - src/Sema/TypeCheckReference.cpp:211-241 CheckUsageOfSuper：extend 里 → `DiagnoseRefactor(sema_extend_use_super, re)`
//           （Refactor，锚 re 整段 = `super` 5 个字符）；struct/interface 里 → sema_super_use_error_inside_non_class
//           / sema_use_super_in_interface（1 个字符）；class 里裸 super → sema_illegal_super_alone（:233）。
//   - include/cangjie/AST/AttributePack.h:255 PRIMARY_CONSTRUCTOR、:465 IMPLICIT_ADD；
//     src/Sema/Desugar/DesugarAfterInstantiation.cpp:803 / :978 给补出的 init 打 IMPLICIT_ADD。
// 文案：include/cangjie/Basic/DiagnosticSema.def:216（invalid_this_call_outside_ctor "invalid calling '%s' outside the
//   constructor"）、:220（multiple_primary_constructors "%s '%s' cannot have more than one primary constructor"）、
//   :226（illegal_place_of_calling_this_or_super "call to '%s' must be first expression in constructor of %s '%s'"）、
//   :227（illegal_place_of_calling_this_primary_constructor "invalid calling 'this' in primary constructor "，官方文案尾部
//   带空格）、:207（use_super_in_interface）、:208（super_use_error_inside_non_class）、:248（abstract_method…，本文件未用）、
//   :262（illegal_this_outside_struct_constructor "'this' is only allowed to be used inside constructor or function for
//   struct"）、:271（illegal_member_used_in_open_constructor）；
//   include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:134（no_non_param_constructor_in_super_class "there is no
//   non-parameter constructor in super class, please invoke super call explicitly"）、:148（extend_use_super）。
// 本项目 CFIR 名（cfir/checkers/.../generator/diagnostics/CfirDiagnosticsList.kt 的构造器 / this-super 组；生成文件
// cfir/checkers/gen/.../diagnostics/CfirErrors.kt 同名工厂；文案表 CfirErrorsDefaultMessages.kt）：
//   ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER           列表 :483 / 工厂 :133 / 文案 :324
//       生产者 CfirConstructorDelegationChecker.kt:82，source = `delegation.call.delegationDiagnosticSource()`
//   ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR 列表 :488 / 工厂 :134 / 文案 :329
//       生产者 CfirConstructorDelegationChecker.kt:74（非首语句）与 :95（首语句），同一 source
//   MULTIPLE_PRIMARY_CONSTRUCTORS                   列表 :440 / 工厂 :124 / 文案 :283
//       生产者 CfirConstructorDelegationChecker.kt:162，source = `source?.firstCharacterDiagnosticSource()`
//   NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS         列表 :547 / 工厂 :146 / 文案 :385
//       生产者 CfirConstructorDelegationChecker.kt:191，source = `implicitPrimaryConstructorOwner(context)
//       ?.classLikeNameDiagnosticSource() ?: constructorNameDiagnosticSource()`（CfirDeclarationDiagnosticSources.kt:46 / :362）
//   THIS_AS_EXPRESSION_IN_FUNC                      列表 :511 / 工厂 :138 / 文案 :350
//       生产者 CfirExpressionSemanticsChecker.kt:388（open|abstract 类构造器 :375-392）与 :459（struct/extend 的 mut 成员
//       函数 :444-462），source = `calleeReference.source?.firstCharacterDiagnosticSource()`
//   ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR         列表 :478 / 工厂 :132 / 文案 :320
//       生产者 CfirFieldVariableThisOrSuperInitializerChecker.kt:306-312（struct 字段初始化器路径 :261），
//       source = `expression.calleeReference.source ?: expression.source`
//   ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR         列表 :499 / 工厂 :136 / 文案 :339
//       生产者 CfirExpressionSemanticsChecker.kt:650
//   INVALID_THIS_CALL_OUTSIDE_CTOR                  列表 :470 / 工厂 :130 / 文案 :311
//       生产者 CfirConstructorDelegationCallChecker.kt:41
// 按源码可预判的锚点差异（未跑 CFIR，只写源码能直接看出的）：
//   (1) MULTIPLE_PRIMARY_CONSTRUCTORS：CFIR 用 `source.firstCharacterDiagnosticSource()`，主构造器声明的 source 从
//       `public` 起算 → 锚 `public` 首字母；官方锚 `fd` 首字符（同样是 `public` 的首字母），两者一致（形态 7）。
//   (2) NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS：官方对用户写的 `init` 锚 `init` 4 个字符，CFIR 的
//       `constructorNameDiagnosticSource()` 取 `getInitKeyword()`，同样 4 个字符（形态 5）；官方对主构造器锚**宿主类名**
//       （形态 5b；`fd.TestAttr(IMPLICIT_ADD) ? MakeRange(decl.identifier) : …` 两个分支实测都落在类名整段上），
//       CFIR 对主构造器回落到 `constructorPsi?.nameIdentifier`（类名），预测一致；官方对编译器补出的构造器（IMPLICIT_ADD）同样锚类名。
//   (3) ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER：官方只锚关键字 1 个字符（`Diagnose(*ce)`）；LLT 老用例
//       llt/class/invalid_super_call.cj:26 把整条 `super().a` 包进标记，与本文件按官方 1 字符的写法不同，
//       本文件以官方 1 字符为准。
//   (4) ILLEGAL_THIS_OR_SUPER_CALL（CfirDiagnosticsList.kt:446，工厂 :126，文案 :291 "Illegal place of calling '{0}'."）
//       在 cfir/checkers/src 下**没有任何生产者**（grep 只命中 CfirErrors / 文案表 / 列表声明），且官方 v1.0.0 的
//       *.def 与 src/Sema 都没有名为 illegal_this_or_super_call 的 kind：它对应官方
//       sema_illegal_place_of_calling_this_or_super（DiagnosticSema.def:226，TypeChecker.cpp:1444）——同名但少了
//       "place of"，且 CFIR 文案去掉了 "in constructor of %s '%s'" 后缀。官方无同名 kind，本文件全部形态按
//       ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER 写（该名有生产者），头部此条只作对照说明。
//   (5) EXPLICIT_SUPER_CALL_REQUIRED（CfirDiagnosticsList.kt:544，工厂 :145，文案 :381 "Explicit super constructor call
//       is required."）：全 cfir/checkers/src 无生产者，官方 v1.0.0 的 *.def 与 src/Sema 也**没有**同名或近名的
//       "explicit super call" kind；官方对"父类无无参构造器"只有 sema_no_non_param_constructor_in_super_class 一种
//       文案（"…please invoke super call explicitly"），CFIR 把这一语义拆成了两个名字（EXPLICIT_SUPER_CALL_REQUIRED 与
//       NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS），而生产者只用了后者。本文件所有"必须显式 super"形态一律写
//       NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS，EXPLICIT_SUPER_CALL_REQUIRED 不写期望。
// CFIR 有、官方 v1.0.0 无同名 kind 的名字：见上面 (4) ILLEGAL_THIS_OR_SUPER_CALL、(5) EXPLICIT_SUPER_CALL_REQUIRED；
// 官方有、本项目无对应名字的 kind（按 SUGGESTED_DIAGNOSTIC 处理，段内不写标记）：
//   sema_use_super_in_interface（:207）、sema_super_use_error_inside_non_class（:208）、sema_extend_use_super（:148）。
//   记忆里"本项目无对应名"的两条已经过时：CfirDiagnosticsList.kt 里 ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR（:478）与
//   THIS_AS_EXPRESSION_IN_FUNC（:511）都存在且都有生产者，本文件按这两个名字写标记；mut/mutableFunctionReferenceRules.cj 的
//   classFinalizerThisAlone.cj 段按 SUGGESTED 写的 USE_THIS_AS_AN_EXPRESSION_IN_FUNC 在本项目亦无此名（析构器形态见"已删除的形态 12"）。
// 已有覆盖不重复（这些形态已在下列文件里，本文件不重写）：
//   - constructor/noConstructorThisAndSuper.cj：this()/super() 的实参个数（WRONG_NUMBER_OF_ARGUMENTS）
//   - constructor/curriedConstructor.cj：构造器柯里化（CANNOT_CURRYING）；constructor/noConstructorDirectConstruction.cj：接口无构造器直接实例化
//   - invalid-declaration/explicitSuperCallRequired.cj、invalid-declaration/constructorDelegationMatrix.cj：
//     父类无无参构造器 + 子类 `init()`（NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS）
//   - invalid-declaration/illegalThisCallInMember.cj、illegalDelegationPlacement.cj、illegalSuperCallInMember.cj、
//     inheritance/thisAndSuperOutsideClass.cj、llt/class/class8.cj、llt/class/interface_init_invalid_1.cj、
//     llt/class/invalid_super_call.cj、llt/class/invalid_super_call_2.cj：this()/super() 在成员函数 / 属性初始化器里
//     （INVALID_THIS_CALL_OUTSIDE_CTOR）以及类外（THIS_SUPER_USE_ERROR_OUTSIDE_CLASS）
//   - inheritance/superAlone.cj、llt/class/invalid_super_call.cj、llt/class/super_this/super_this_09.cj：裸 super 表达式（ILLEGAL_SUPER_ALONE）
//   - invalid-declaration/recursiveConstructorCall.cj、llt/record/record_this.cj：构造器自委派（RECURSIVE_CONSTRUCTOR_CALL）
//   - inheritance/classSuperCallAllowed.cj：super 调用父类普通成员函数（合法）
//   - inheritance/illegalSuperInStructAndEnum.cj：struct / enum 里的裸 super（STRUCT_SUPER_NOT_ALLOWED / ENUM_SUPER_NOT_ALLOWED，
//     官方对应 sema_super_use_error_inside_non_class，但那条别的 LLT 用例没有用；本文件只补 interface 形态）
//   - static-init/thisOrSuperInStaticInit.cj：static 成员初始化器里的 this/super
//   - llt/class/constructor/primaryConstructor/primaryConstructor2.cj：多个主构造器（MULTIPLE_PRIMARY_CONSTRUCTORS）、
//     主构造器里 super() 非首语句、主构造器里 this(...)
//   - llt/class/super_this/super_this_11.cj：try 块里的 super()
//   - llt/record/record_this.cj：struct 字段初始化器里的 this（ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR）
//   - llt/class/open_modifier/class_open_ctor3_fail.cj、class_open_ctor4_fail.cj、llt/record/mut/record_mut_invalid_4.cj 等：
//     裸 this 作表达式（THIS_AS_EXPRESSION_IN_FUNC）、open 构造器里访问实例成员（ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR）
//   - llt/class/class_access_control7.cj、llt/InitializationCheck/*.cj：super 成员在 super() 之前被访问（ILLEGAL_USAGE_OF_SUPER_MEMBER）
//   - mut/mutableFunctionReferenceRules.cj：struct 构造器里捕获 this、mut 函数里裸 this、析构器里用实例成员
//
// ── 形态 1：class 构造器里 `super(...)` 不是第一条语句（前面是字段赋值）
// kind：sema_illegal_place_of_calling_this_or_super "call to 'super' must be first expression in constructor of class 'NotFirstSuperSub'"
// 锚点：`super` 首字母 1 个字符（`Diagnose(*ce)` 退化到 ce.GetBegin()）。1.0.5 / 1.1.3 一致 Range (14,9)..(14,10)。探针 a01。
// 显式 super(...) 不是编译器补出的 COMPILER_ADD 调用，所以**不会**叠加 NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS（源码 TypeChecker.cpp:1640）。
// FILE: superNotFirstAfterThisAssign.cj
open class NotFirstSuperBase {
    let b: Int64

    init(b: Int64) {
        this.b = b
    }
}

class NotFirstSuperSub <: NotFirstSuperBase {
    let a: Int64

    init(a: Int64) {
        this.a = a
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper(a)
    }
}

// ── 形态 2：class 构造器里 `this(...)` 不是第一条语句（同类内委派到另一个构造器）
// kind：sema_illegal_place_of_calling_this_or_super，文案里 %s 是 'this'、宿主是 class 'NotFirstThisClass'。锚 `this` 首字母 1 个字符。
// 1.0.5 / 1.1.3 一致 Range (6,9)..(6,10)。探针 a02。
// FILE: thisNotFirstAfterThisAssign.cj
class NotFirstThisClass {
    let a: Int64

    init() {
        this.a = 1
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>t<!>his(1)
    }

    init(a: Int64) {
        this.a = a
    }
}

// ── 形态 3：`super(...)` 位于 if/else 两个分支内（都不是首条语句）→ 官方按调用逐条报告，两个锚点
// 锚点：两处 `super` 首字母各 1 个字符，Range (14,13)..(14,14) 与 (16,13)..(16,14)。1.0.5 / 1.1.3 一致。探针 a08。
// FILE: thisNotFirstInIfBranches.cj
open class BranchSuperBase {
    let b: Int64

    init(b: Int64) {
        this.b = b
    }
}

class BranchSuperSub <: BranchSuperBase {
    let a: Int64

    init(a: Int64) {
        if (a > 0) {
            <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper(a)
        } else {
            <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper(a + 1)
        }
        this.a = a
    }
}

// ── 形态 4：`super(...)` 写在 lambda 体里 + 父类没有无参构造器（本段两条不同 kind）
// 诊断 1：NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS 锚 `init` 4 个字符，Range (12,5)..(12,9)（编译器补 super() 的锚点分支）。
// 诊断 2：ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER 锚 lambda 体内 `super` 首字母 1 个字符，Range (13,22)..(13,23)。
// 1.0.5 / 1.1.3 一致。探针 a09。
// FILE: superInsideLambda.cj
open class LambdaSuperBase {
    let b: Int64

    init(b: Int64) {
        this.b = b
    }
}

class LambdaSuperSub <: LambdaSuperBase {
    let a: Int64

    <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>(a: Int64) {
        let f = { => <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper(a) }
        this.a = a
        let _ = f
    }
}

// ── 形态 5：父类没有无参构造器、子类构造器既不写 super(...) 也不写 this(...)（编译器补 super()）
// kind：sema_no_non_param_constructor_in_super_class "there is no non-parameter constructor in super class, please invoke
//       super call explicitly"，锚用户写的 `init` 4 个字符（官方 `MakeRange(fd.identifier.Begin(), fdName)` 的 fdName="init"）。
// 1.0.5 / 1.1.3 一致 Range (12,5)..(12,9)。探针 a05。
// FILE: noNonParamSuperImplicit.cj
open class NoNonParamImplicitBase {
    let b: Int64

    init(b: Int64) {
        this.b = b
    }
}

class NoNonParamImplicitSub <: NoNonParamImplicitBase {
    let a: Int64

    <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>(a: Int64) {
        this.a = a
    }
}

// ── 形态 5b：形态 5 的两个锚点分支——同一个子类里主构造器与一个独立 `init` 都缺显式 super
// kind：sema_no_non_param_constructor_in_super_class，锚点分支按官方 `MakeRange(fd.identifier.Begin(), fdName)`（TypeChecker.cpp:1655）
//   实测：主构造器（`public SiblingSub(z: Int64)`，源码里没有 init 关键字）→ 锚**宿主类名** `SiblingSub` 10 个字符，
//   Range (12,12)..(12,21)；同类的 `public init(x: Int64, y: Int64)` → 锚 `init` 4 个字符，Range (15,12)..(15,15)。
// 1.0.5 / 1.1.3 一致。探针 S5b。按源码预判 CFIR 侧：`constructorNameDiagnosticSource()`（CfirDeclarationDiagnosticSources.kt:362）
// 对主构造器没有 init 关键字时回落到 `constructorPsi?.nameIdentifier`（即类名），对 `init` 取 getInitKeyword → 两者都与官方一致。
// FILE: noNonParamSuperImplicitSibling.cj
open class SiblingBase {
    let b: Int64

    init(b: Int64) {
        this.b = b
    }
}

class SiblingSub <: SiblingBase {
    var a: Int64 = 0

    public <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>SiblingSub<!>(z: Int64) {
    }

    public <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>(x: Int64, y: Int64) {
        this.a = x + y
    }
}

// ── 形态 6：主构造器（`public C(...)` 形式）里调用 this(...)，即使它是首条语句
// kind：sema_illegal_place_of_calling_this_primary_constructor "invalid calling 'this' in primary constructor "，
//       官方文案尾部带空格。锚点：`this` 首字母 1 个字符。1.0.5 / 1.1.3 一致 Range (9,9)..(9,10)。探针 S6。
//       主构造器里 this(...) 永远非法（源码 TypeChecker.cpp:1436 在位置检查之前），不需要父类可委派目标。
// FILE: thisInPrimaryConstructor.cj
open class PrimaryThisBase {
    init() {}
}

class PrimaryThisClass <: PrimaryThisBase {
    var a: Int64 = 0

    public PrimaryThisClass(z: Int64) {
        <!ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR!>t<!>his()
    }

    public init() {
    }
}

// ── 形态 7：同一 class 里两个主构造器
// kind：sema_multiple_primary_constructors "class 'MultiPrimaryClass' cannot have more than one primary constructor"，
//       锚**第二个**主构造器声明的首字符（`public` 的 p）1 个字符。1.0.5 / 1.1.3 一致 Range (3,5)..(3,6)。探针 a12 / S7。
// FILE: multiplePrimaryConstructors.cj
class MultiPrimaryClass {
    public MultiPrimaryClass(var z!: Int64 = 3) {}
    <!MULTIPLE_PRIMARY_CONSTRUCTORS!>p<!>ublic MultiPrimaryClass(var x!: String = "3") {}
}

// ── 形态 8：open class 构造器里把 `this` 当作独立表达式（`let t = this`）
// kind：sema_use_this_as_an_expression_in_func "'this' cannot be used as an expression in the constructor of open class"，
//       锚点：`this` 首字母 1 个字符。1.0.5 / 1.1.3 一致 Range (5,17)..(5,18)。探针 b01。
//       只有 open / abstract 类构造器触发（源码 `IsInheritableClass(*outerDecl)`），普通 class 构造器合法（见合法对照 L3）。
// FILE: bareThisInOpenCtor.cj
open class BareThisOpenCtorClass {
    var a: Int64 = 0

    init() {
        let t = <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
        this.a = 1
        let _ = t
    }
}

// ── 形态 9：abstract class 构造器里的裸 this（消息里类 kind 变成 abstract）
// kind：sema_use_this_as_an_expression_in_func，锚 `this` 首字母 1 个字符。1.0.5 / 1.1.3 一致 Range (5,17)..(5,18)。探针 b02。
// FILE: bareThisInAbstractCtor.cj
abstract class BareThisAbstractCtorClass {
    var a: Int64 = 0

    public init() {
        let t = <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
        this.a = 1
        let _ = t
    }
}

// ── 形态 10：open class 构造器里把 `this` 作为实参传入成员函数（本段两条 kind，锚点相邻）
// 诊断 1：sema_illegal_member_used_in_open_constructor 锚成员名 `sink` 首字母 1 个字符 Range (5,14)..(5,15)（TypeCheckExpr.cpp:222）。
// 诊断 2：sema_use_this_as_an_expression_in_func 锚实参 `this` 首字母 1 个字符 Range (5,19)..(5,20)。
// 1.0.5 / 1.1.3 一致。探针 b04。`this.a` 这样的成员访问不算 isAlone，官方不报 use_this（见合法对照 L4）。
// FILE: bareThisAsArgumentInOpenCtor.cj
open class ThisArgOpenCtorClass {
    var a: Int64 = 0

    init() {
        this.<!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>s<!>ink(<!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his)
        this.a = 1
    }

    func sink(v: ThisArgOpenCtorClass): Unit {
        let _ = v
    }
}

// ── 形态 11：struct 字段初始化器里用 `this`（初始化器没有函数体上下文，官方 GetCurFuncBody 为空）
// kind：sema_illegal_this_outside_struct_constructor "'this' is only allowed to be used inside constructor or function
//       for struct"，锚点：`this` 首字母 1 个字符。1.0.5 / 1.1.3 一致 Range (3,20)..(3,21)。探针 b07。
//       CFIR 生产者只挂在 struct **字段初始化器**这条路径上（CfirFieldVariableThisOrSuperInitializerChecker.kt:261）。
// FILE: thisInStructFieldInitializer.cj
struct StructFieldInitThis {
    let a: Int64 = 0
    let b: Int64 = <!ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR!>t<!>his.a

    init() {}
}

// ── 形态 12：interface 成员函数体里用 super（官方无对应 CFIR 名，整段按 SUGGESTED 处理）
/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_use_super_in_interface）
SUGGESTED_MESSAGE: 'super' cannot be used in interface
SOURCE: src/Sema/TypeCheckReference.cpp:220（CheckUsageOfSuper，interface 分支，`Diagnose(re, …)` 锚 1 个字符）；include/cangjie/Basic/DiagnosticSema.def:207
锚点：`super` 首字母 1 个字符，Range (3,16)..(3,17)。1.0.5 / 1.1.3 一致。探针 b10。
*/
// FILE: superInInterface.cj
interface SuperInInterface {
    func f(): Int64 {
        return super.f()
    }
}

// ── 形态 13：struct 成员函数体里用 super（官方无对应 CFIR 名，整段按 SUGGESTED 处理）
/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_super_use_error_inside_non_class）
SUGGESTED_MESSAGE: 'super' can only be used in class
SOURCE: src/Sema/TypeCheckReference.cpp:224（CheckUsageOfSuper，outerDecl 不是 CLASS_DECL 分支）；include/cangjie/Basic/DiagnosticSema.def:208
锚点：`super` 首字母 1 个字符，Range (3,16)..(3,17)。1.0.5 / 1.1.3 一致。探针 b11。继承/访问 open 父类成员的写法
（struct 继承 class）在本仓库两版 cjc 都不可表达，见"已删除的形态 10"。
*/
// FILE: superInStruct.cj
struct SuperInStructClass {
    func f(): Int64 {
        return super.g()
    }
}

// ── 形态 14：extend 块里用 super（refactor kind，锚 `super` 整段 5 个字符；官方无对应 CFIR 名）
/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_extend_use_super）
SUGGESTED_MESSAGE: 'super' is not allowed inside an extend declaration
SOURCE: src/Sema/TypeCheckReference.cpp:225（`DiagnoseRefactor(sema_extend_use_super, re)`，Refactor kind 走 MakeRealRange(re)，锚 re 整段）；include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:148
锚点：`super` 整段 5 个字符，Range (9,16)..(9,21)。1.0.5 / 1.1.3 一致。探针 c04。
*/
// FILE: superInExtend.cj
open class ExtendSuperBase {
    func g(): Int64 {
        return 1
    }
}

extend ExtendSuperBase {
    func f(): Int64 {
        return super.g()
    }
}

// ── 形态 15：class 成员函数（非构造器）里调用 this()
// kind：sema_invalid_this_call_outside_ctor "invalid calling 'this' outside the constructor"，锚 `this` 首字母 1 个字符。
// 1.0.5 / 1.1.3 一致 Range (5,9)..(5,10)。探针 b13。
// FILE: thisCallInMemberFunc.cj
class ThisCallInMemberClass {
    init() {}

    func f(): Unit {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>t<!>his()
    }
}

// ── 形态 16：class 成员函数里调用 super(...)，且该类本身有合法构造器（排除 no_non_param 干扰）
// kind：sema_invalid_this_call_outside_ctor，文案里 %s 是 'super'。锚 `super` 首字母 1 个字符，Range (16,9)..(16,10)。
// 1.0.5 / 1.1.3 一致。探针 b14 / S16。
// FILE: superCallInMemberFunc.cj
open class MemberSuperBase {
    let bv: Int64

    init(bv: Int64) {
        this.bv = bv
    }
}

class MemberSuperSub <: MemberSuperBase {
    let a: Int64 = 0

    init() {
        super(1)
    }

    func f(): Unit {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper(1)
    }
}

// 合法对照段（1.0.5 / 1.1.3 两版均零 error）：

// L1：显式 super(...) 写在构造器首条语句，父类构造器带参匹配。探针 a01 反例 / L1。
// FILE: legalSuperFirst.cj
open class LegalSuperBase {
    let b: Int64

    init(b: Int64) {
        this.b = b
    }
}

class LegalSuperSub <: LegalSuperBase {
    let a: Int64

    init(a: Int64) {
        super(a)
        this.a = a
    }
}

// L2：同类内 this(1) 委托给另一个构造器，且是首条语句（`let` 之后才 this(...) 不合法，首条合法）。探针 a02 反例 / b19。
// FILE: legalThisDelegation.cj
class LegalThisClass {
    let a: Int64

    init() {
        this(1)
    }

    init(a: Int64) {
        this.a = a
    }
}

// L3：普通（非 open/abstract）class 构造器里裸 this 作表达式合法；成员函数里 this.x 也合法。探针 b03 / b19 / L3。
// FILE: legalBareThisInNonInheritableClass.cj
class LegalBareThisClass {
    var a: Int64 = 0

    init() {
        let t = this
        this.a = 1
        let _ = t
    }

    func f(): Int64 {
        return this.a
    }
}

// L4：open class 构造器里读 this.a 合法（成员访问不是 isAlone；只是不能调用实例成员函数/属性）。探针 b05 / b19。
// FILE: legalThisMemberInOpenCtor.cj
open class LegalOpenCtorClass {
    var a: Int64 = 0

    init() {
        let v = this.a
        this.a = v + 1
    }

    func f(): Int64 {
        return this.a
    }
}

// L5：struct 构造器与普通成员函数里 this.a 合法（struct-this 规则只禁字段初始化器与嵌套 callable）。探针 b07 反例 / b19。
// FILE: legalStructThis.cj
struct LegalStructThis {
    let a: Int64

    init(a: Int64) {
        this.a = a
    }

    func f(): Int64 {
        return this.a
    }
}

// L6：class / struct 各一个主构造器合法（多个主构造器才报）；不要在类里再声明与主构造器同形的字段，否则官方报
//     sema_redefinition / sema_class_uninitialized_field（探针 L6 首版，见"已删除的形态 12"）。探针 b24 / L6 修正版。
// FILE: legalSinglePrimaryCtor.cj
class LegalPrimaryClass {
    public LegalPrimaryClass(var z!: Int64 = 3) {}
}

struct LegalPrimaryStruct {
    public LegalPrimaryStruct(var y!: Int64 = 3) {}
}

// 已删除的形态（取证 kind 与 Range 见下；1.0.5 与 1.1.3 两版一致，除注明外）：
//   1. struct 构造器里的 `this(...)` 委派（`this.a = a; this(a)`）
//      → sema_illegal_place_of_calling_this_or_super + sema_recursive_constructor_call（锚 (4,5)..(4,9)，带 depends on 附注）
//        + sema_cannot_assign_to_immutable（let 字段二次赋值）。struct 的 `this(...)` 只能自指，噪声太大，本文件不收。探针 a11。
//   2. 构造器里先 `let f = { => super(a) }` 再 `super(...)`：同形态 4，但两条诊断会同时出现在同一段（no_non_param 锚 init
//      + 位置锚 lambda 内的 super），为避免同族重复，形态 4 只保留 lambda 内的非首条 super 这一种。探针 a09。
//   3. `super(...)` 写在裸块 `{ super(a) }` 里
//      → parse_expected_double_arrow_in_lambda（锚 (14,13)..(14,18)）：构造器体里的裸块被解析器当成 lambda 起始，属 parser 族。探针 a07。
//   4. interface 里写 `init() {}`
//      → parse_unexpected_declaration_in_scope "unexpected constructor in interface body"（锚 init），属 parser 族。探针 b09。
//      所以官方 sema_illegal_this_in_interface（`this` 出现在 interface）在本仓库不可用合法形态触发：interface 成员函数里的
//      `this.x` 先被 sema_not_member_of 拦下（探针 b08），interface 里也没有可委派的 this(...) 目标。
//   5. open class 里写 `abstract func g(): Int64`
//      → 1.0.5 parse_illegal_modifier_in_scope；1.1.3 另加 parse_explicitly_abstract_only_for_cjmp_abstract_class。
//        属 parser 族且两版不一致（abstract 是 CJMP 专有），本文件不收。探针 b22。
//   6. struct 构造器里裸 `this` 作表达式
//      → sema_illegal_usage_of_member "'this' is not allowed to be accessed before all member variables are initialized"，
//        属初始化顺序族（ILLEGAL_USAGE_OF_MEMBER），不是 this/super 位置族。探针 b06。
//   7. class 字段初始化器里写 `this.a`
//      → 零诊断（class 字段初始化器里 this 是允许的，官方只对 static 成员与 struct 成员禁 this/super），
//        属 static-init 族，已有 static-init/thisOrSuperInStaticInit.cj 覆盖。探针 b15。
//   8. interface 成员函数体里 `this.a`（a 不是接口成员）
//      → sema_not_member_of 先报，this 本身不触发 sema_illegal_this_in_interface。探针 b08。
//   9. 一个构造器里同时有首条 `super(a)` 与后续 `this(a)`
//      → sema_illegal_place_of_calling_this_or_super + sema_recursive_constructor_call（带 depends on 附注）
//        + sema_class_uninitialized_field。struct 只能自指、class 同类 this(a) 与首条 super 的组合都属这一类。探针 a06。
//  10. struct 继承 class 后在构造器里 super(a)（`struct S<T> <: Base` / `struct S <: OpenBase`）
//      → 1.0.5 / 1.1.3：sema_type_implement_non_interface（struct 只能实现接口）先报，struct 构造器里的 super(...) 无法表达；
//        struct 继承 open class 后 super.g() 则叠加 sema_invalid_member_visibility_in_class。本仓库语法/语义上不可用。探针 c02 / c03。
//  11. 父类无无参构造器、子类主构造器形如 `public C(var z!: Int64 = 3)`，另加同形的 `init(...)`
//      → sema_overload_conflicts（主构造器脱糖成 init，与同形 init 重载冲突）+ sema_class_uninitialized_field。
//        所以形态 5b 的兄弟构造器必须换成不同元数（`init(x: Int64, y: Int64)`）。探针 c01 / S5b。
//  12. class / struct 里同时声明 `var z: Int64` 字段与 `public C(var z!: Int64 = 3)` 主构造器
//      → sema_redefinition（锚形参 z）+ sema_class_uninitialized_field。合法对照 L6 因此不声明同名字段。探针 L6 首版。
//  13. 顶层函数里 `this()`：sema_this_super_use_error_outside_class 锚 this 1 个字符
//      （inheritance/thisAndSuperOutsideClass.cj 已覆盖 super(1) 形态），不重复。探针 b16。
//  14. 递归构造器 this() / this(1)↔this()：RECURSIVE_CONSTRUCTOR_CALL 已有
//      invalid-declaration/recursiveConstructorCall.cj 覆盖，本文件不收。探针 b17 / b18。
//  15. 显式 `super()` 调用父类无参构造器：零诊断（合法），不单独成段。探针 b21。
//  16. 析构函数里裸 `this` 作表达式：sema_use_this_as_an_expression_in_func（消息 "…in the finalizer"），
//      官方有、CFIR 的 THIS_AS_EXPRESSION_IN_FUNC 生产者只覆盖 open/abstract 构造器与 struct/extend 的 mut 成员函数，
//      析构器分支无对应名，且 mut/mutableFunctionReferenceRules.cj 的 classFinalizerThisAlone.cj 段已按
//      SUGGESTED 记录，本文件不重复。探针 b23。
//  17. `super` 单独作表达式：sema_illegal_super_alone 锚 super 1 个字符，inheritance/superAlone.cj 已覆盖。探针 b04 之外的形态。
//  18. open 父类构造器里访问抽象成员 super.g()：需要 `abstract func`，本仓库解析器拒绝（见已删除形态 5），
//      ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY 形态不可用。探针 b22。
//  19. struct 构造器 delegating 到 this(...) 必须是首条：官方对 struct 的 `this(...)` 只能自指（已删除形态 1），
//      所以 struct 版 this 委派没有可写的合法对照。
