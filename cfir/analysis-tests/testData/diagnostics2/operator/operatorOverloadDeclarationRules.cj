// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_WITH_PARSER: Psi
//
// 【取证方式】官方 cjc 1.0.5 与本机 cjc 1.1.3 两版实测，静态库模式
// `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
// （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断）。1.1.3 在 CHIR / codegen 阶段可能 LLVM 崩
// （崩在语义检查之后），语义诊断 JSON 仍完整产出；**合法对照段**在 1.1.3 下崩在 codegen、拿不到 JSON，
// 它们的“合法”结论取自 1.0.5 的零诊断以及源码推导。除特别注明外两版 kind、消息、Range 完全一致。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记
// 逐字符对齐官方范围。
//
// 【官方实现】（external/cangjie_compiler，checkout 在 tag v1.0.0）
//   入口 src/Sema/TypeCheckDecl.cpp:155-193 CheckOperatorOverloadFunc：
//   - :157-159 fd.ty 不是 FuncTy / fd.op 是 ILLEGAL / `(`（invoke）→ 直接返回，**不做任何检查**。
//     `()` 的“不可重载”由 parser 拦（见文件末“已删除的形态”）。
//   - :160-162 `[]` → 转入 HandIndexOperatorOverload（:196-236）。
//   - :163 按 funcTy->paramTys.size() 分派：
//     ① 0 个形参：IsUnaryOperator(op)（BuiltInOperatorUtil.cpp:107-110，即 UNARY_EXPR_TYPE_MAP 的键）
//        且 IsBuiltinUnaryExpr(op, baseTy)（:118-129，宿主类型在该 token 的候选类型表里）→
//        sema_operator_overload_built_in_unary_operator（:171，参数 fd.identifier / baseTy）；不是一元运算符 →
//        sema_operator_overload_invalid_num_parameter（:176）。
//     ② 1 个形参：IsBinaryOperator(op)（:112-115，即 BINARY_EXPR_TYPE_MAP 的键）否则 invalid_num_parameter（:184）；
//        是二元且 `fd.fullPackageName != CORE_PACKAGE_NAME` 且 IsBuiltinBinaryExpr(op, baseTy, paramTys[0])（:131-175）→
//        sema_operator_overload_built_in_binary_operator（:187，参数 fd.identifier / baseTy / paramTys[0]）。
//     ③ ≥2 个形参：invalid_num_parameter（:193）。
//     三个报告点都**不检查 operator 的返回类型**。
//   - :196-236 HandIndexOperatorOverload：`:201-204` 无形参 → invalid_num_parameter 后 return；
//     `:206-209` **最后一个形参不是具名**（无 `!`）→ 视为读取运算符，直接 return（后面三条都不检查）；
//     `:210-220` 具名形参里名字不是 `value` 的 → sema_invalid_subscript_assign_parameter（refactor），
//     锚第一个非法具名形参的 identifier（`MakeRange(identifier)`，其余作为 AddHint）；
//     `:221-225` 形参总数 ≤ 1 → sema_invalid_subscript_assign_parameter_num（refactor，`MakeRange(fd.identifier)` 即 `[]`）
//     后 return；`:226-235` 具名形参全部合法但返回类型不是 Unit → sema_invalid_subscript_assign_return（refactor），
//     锚**返回类型整段**（`fd.funcBody->retType` 非空）或无显式返回类型时锚 `[]`。
//     官方**不校验下标形参的类型**（p27 / r5 / r6 实测：`index: Int32` / `index: String` 的 setter 合法）。
//   - 锚点机制：普通 kind 的 `diag.Diagnose(fd, kind, ...)` 走 include/cangjie/Basic/DiagnosticEngine.h:844-857
//     → :822-825 的 `Diagnose(pos)` = GetBegin 一个字符；`FuncDecl` 的 GetBegin 含修饰符，所以锚点是
//     **声明第一个修饰符的首字母**。本文件所有 operator 声明都写在 4 空格缩进的成员位且**不带 `public`**，
//     于是锚点恒为 `operator` 关键字首字符（第 5 列）。refactor kind 用 DiagnoseRefactor 传显式 range。
//   - 一元 / 二元分类表 src/Sema/BuiltInOperatorUtil.cpp：
//     UNARY_EXPR_TYPE_MAP（:49-55）键只有 SUB `-`、NOT `!`、INCR `++`、DECR `--`：
//       `-` → 全部算术类型（Int8/16/32/64/Native、UInt8/16/32/64/Native、IdealInt、Float16/32/64、IdealFloat）；
//       `!` → 全部整数类型 + Bool；`++` / `--` → 全部整数类型。
//     BINARY_EXPR_TYPE_MAP（:59-83）键：ADD `+`、SUB `-`、MUL `*`、DIV `/`、MOD `%`、EXP `**`、
//       EQUAL `==`、NOTEQ `!=`、LT `<`、LE `<=`、GT `>`、GE `>=`、LSHIFT `<<`、RSHIFT `>>`、
//       BITAND `&`、BITXOR `^`、BITOR `|`、AND `&&`、OR `||`；类型：算术 / 整数（取余、位运算、位移）/
//       `**` 只 Int64×UInt64→Int64 与 Float64×Int64|Float64→Float64（SEMA_EXP_TYPES :95-98）/
//       比较与相等为 GENERAL_RELATION_NUMERIC_TYPE_MAP（整数 + Float + Rune），`==` / `!=` 另含 Bool、Unit；
//       `<<` / `>>` 两侧类型可不同（:157-170）。注意 `+` **不在**一元表里：仓颉没有一元正号运算符。
//   - 文案 include/cangjie/Basic/DiagnosticSema.def:54（invalid_num_parameter）、:55-56（built_in_unary_operator）、
//     :57-58（built_in_binary_operator）；refactor 文案 include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:36 / :37-38 / :39。
//
// 【本项目 CFIR 诊断名与生产者】
//   - cfir/checkers/checkers-component-generator/src/.../diagnostics/CfirDiagnosticsList.kt:292
//     INVALID_OPERATOR_PARAMETER_COUNT（参数 operator / expectedCount / actualCount）；
//     :299 OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR（operator / receiverType）；
//     :305 OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR（operator / receiverType / parameterType）；
//     :1214 INVALID_SUBSCRIPT_ASSIGN_PARAMETER；:1217 INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM；:1220 INVALID_SUBSCRIPT_ASSIGN_RETURN。
//   - 生产者 cfir/checkers/src/.../checkers/declaration/CfirOperatorDeclarationChecker.kt：
//     :83-95 形参个数（expectedParameterCount :154-158 按名称取 0/1）；
//     :106-149 内建命中（checkBuiltinPrimitiveOperatorOverload，:130 二元额外要求不在 std.core 包）；
//     :140-148 返回类型与内建不一致时追加 RETURN_TYPE_INCOMPATIBLE；
//     :167-195 `[]` 三条（:78-81 只在名称 == *operator_set 时进入）；
//     锚点 helper :202-203 operatorDiagnosticSource（`operator` 修饰符整 token）、
//     :209-211 builtinOperatorOverloadDiagnosticSource（有 `public` 时取 `public` 整 token）、
//     CfirDeclarationDiagnosticSources.kt:267-279 functionNameDiagnosticSource（函数名 token）、
//     :323-335 subscriptAssignmentReturnDiagnosticSource（返回类型 token，无显式返回类型时函数名 token）。
//   - 形参个数规则用 expectedParameterCount（按运算符名称），官方用 paramTys.size() 分派；本文件按源码推导
//     出两者一致的结论（见【可预判差异】），未跑 CFIR。
//
// 【可预判的锚点与覆盖差异（未跑 CFIR）】
//   1. 锚点长度：本项目 range policy 用完整 token，官方用 GetBegin 单字符。本文件**按官方**在 `operator`
//      关键字首字母写 1 字符标记；本项目 CfirOperatorDeclarationChecker.kt:202-203 / :209-211 报的是
//      `operator` 整 token（有 `public` 时是 `public` 整 token，llt/Extend/extend_overload_builtin_op1.cj 即如此），
//      正式跑 CFIR 时这三类（个数诊断、内建一元、内建二元）的标记范围要按实测调整。
//      `[]` 返回类型锚点（CjOffsetsOnlySourceElement，OffsetsOnly）与官方一致；PARAM_NUM 的锚点差异见第 4 条。
//   2. 一元正号 `+`：官方 0 形参 `+` 因不在 UNARY_EXPR_TYPE_MAP 报 INVALID_OPERATOR_PARAMETER_COUNT（b8 / b9 实测）；
//      本项目 LightTreeRawCfirDeclarationBuilder.kt:2866-2867 / CjNamedFunction.kt:122-123 把 0 形参 `+`
//      归一化为 `*operator_unaryPlus`，CfirOperatorDeclarationChecker.unaryOperatorNames :35-41 含
//      UNARY_PLUS → 期望 0 形参、**不会**报个数诊断；且 BuiltinPrimitiveOperators.unaryNumericNames
//      （:104-107）含 UNARY_PLUS、int/numeric 宿主都有候选 → 可能反而报 OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR。
//      这是可预判缺口（形态 8 的第一条）。
//   3. `[]` 具名形参规则：官方允许“一个位置下标 + 恰好一个名为 `value` 的具名形参”。本项目把 `[]` 命名为
//      `*operator_set` 的前提就是“最后一个形参具名且名字为 value”（LightTreeRawCfirDeclarationBuilder.kt:2884-2892、
//      CjNamedFunction.kt:80-84），所以 `v!` / `w!` 这类形态被折叠为 GET，:167-195 的三条里前两条对它们**不可达**
//      （与 diagnostics2/operator/subscriptAssignNamedParameter.cj 头部记录一致）。只有返回类型检查可达。
//   4. `[]` 只有具名形参（`operator func [](value!: Int64)`）：官方锚 `[]` 2 字符；本项目 :172-177 锚 `operator` 整 token
//      （与 diagnostics2/operator/subscriptAssignNoPositional.cj 记录一致）。
//   5. “只有一个具名形参且名为 v”形态：官方同一条声明上**同时**报 PARAM（锚 v）与 PARAM_NUM（锚 `[]`）；
//      本项目折叠为 GET → 两条都不报（可预判缺口，形态 16）。
//   6. 无显式返回类型、函数体是赋值语句（类型 Unit）：官方 funcTy.retTy 是 Unit → 零诊断（e3 实测）；本项目按隐式
//      返回类型计算，行为一致（形态 14 用表达式体 `{ this.data[index] }` 覆盖推导为 Int64 的情况）。
//   7. 官方 `fd.fullPackageName != CORE_PACKAGE_NAME`（TypeCheckDecl.cpp:186）在本文件所有段都成立（用户包名），
//      本项目 :130 用 `isStdCoreFile()` 判断，语义一致。
//   8. 内建返回类型不一致：官方三个报告点都不看返回类型（r1 / r2 实测各只有一条 built_in_* 诊断；
//      官方 sema_type_incompatible 只在赋值表达式 / MatchCase 语境产生：DiagnosticSema.def:107、
//      AssignExpr.cpp:378/393/431、TypeCheckMatchExpr.cpp:136/208），operator 重载返回类型不匹配时官方零诊断。
//      本项目 CfirOperatorDeclarationChecker.kt:140-148 **额外**报 RETURN_TYPE_INCOMPATIBLE（锚函数名 token），
//      该诊断是 CFIR 侧补充规则 → 按规则不写内联标记（形态 7 只标官方诊断；跑 CFIR 时该段会多出这一条）。
//
// 【已有覆盖不重复】
//   - diagnostics/coverage/declaration-status/operatorParameterCountRich.cj：INVALID_OPERATOR_PARAMETER_COUNT ×3，
//     class 宿主：`!` 1 形参、`+` 2 形参、`<<` 2 形参，标在 `operator` 上。
//   - llt/class/class_access_control.cj:5：INVALID_OPERATOR_PARAMETER_COUNT ×1（`private operator func +(b, c)` 2 形参）。
//   - llt/Extend/extend_overload_builtin_op1.cj：extend 宿主内建重载，BINARY ×5（`+`(Int64)、`==`(Int64)、
//     `<<`(Int64)、`-`(Int64)、`==`(Bool)）、UNARY ×1（`-`），全部标在 `public` 上；含 RETURN_TYPE_INCOMPATIBLE ×1。
//   - diagnostics2/operator/subscriptAssignNamedParameter.cj（PARAM，class 宿主，`v!` 唯一具名形参）、
//     subscriptAssignNoPositional.cj（PARAM_NUM，`[](value!: Int64)`）、subscriptAssignReturnType.cj
//     （RETURN，class 宿主显式 `: Int64`）。
//   - llt/operator_overload/err_subscript_assign_03.cj（RETURN，class 宿主显式返回类型）、
//     err_subscript_assign_04.cj（RETURN，无显式返回类型、表达式体，锚 `[]`）；
//     llt/generics/operatorOverload/operatorOverload_indexSet2.cj（RETURN，class 宿主 `: Int64`）。
//   - diagnostics2/generic/genericBoundRules.cj：GENERIC_IN_OPERATOR_OVERLOAD（operator 不得声明类型参数）。
//   本文件只补它们没写的：0 形参的一元 / 非一元运算符、extend 宿主上的内建一元/二元多种 token、
//   形参类型不同（位移、幂）的二元命中、一元表边界、内建返回类型不一致、`[]` 在 **extend 宿主**下的全部形态、
//   多个非法具名形参、`value!` + `w!`、最后一个形参为位置形参的 getter 门控、返回类型推导为非 Unit 的隐式返回类型。
//
// 【两版差异】全部形态 1.0.5 与 1.1.3 的 kind、消息、Range 完全一致，未拆 LANGUAGE_VERSION。
// 合法对照段在 1.1.3 下于 codegen 阶段崩（无 JSON），1.0.5 下零 error（可能有 chir_dce_unused_function warning，
// Severity WARNING，不参与框架比较、不写标记）。

// 形态 1：extend 内建一元运算符重载——宿主类型命中 UNARY_EXPR_TYPE_MAP。
//   `-`（算术全表）/ `!`（整数表 + Bool）。锚点：`operator` 关键字首字符。
// 1.0.5 / 1.1.3 一致：4 条 OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR，Range 均为 (行,5)..(行,6)。
// FILE: oprBuiltinUnary.cj
extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>o<!>perator func -(): Int64 {
        return 1
    }

    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>o<!>perator func !(): Int64 {
        return 1
    }
}

extend Bool {
    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>o<!>perator func !(): Bool {
        return true
    }
}

extend Float64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>o<!>perator func -(): Float64 {
        return 1.0
    }
}

// 形态 2：一元表边界——宿主类型不在候选表内则**合法**（IsBuiltinUnaryExpr 为假，不报内建重载）。
//   `!` 只覆盖整数 + Bool，Float64 不在内；`-` 只覆盖算术类型，Bool 不在内。
// 1.0.5 / 1.1.3 一致：零 error（codegen 阶段给 chir_dce_unused_function 警告：未被使用的 operator 函数，
//   Severity WARNING，不参与框架比较、不写标记）。
// FILE: oprBuiltinUnaryMissLegal.cj
extend Float64 {
    operator func !(): Bool {
        return true
    }
}

extend Bool {
    operator func -(): Bool {
        return true
    }
}

// 形态 3：同一宿主上一元与二元 `-` 并存——形参个数决定走哪条分派（0 → 一元表，1 → 二元表）。
// 1.0.5 / 1.1.3 一致：(2,5)..(2,6) 一元 + (6,5)..(6,6) 二元。
// FILE: oprUnaryAndBinarySameToken.cj
extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>o<!>perator func -(): Int64 {
        return 1
    }

    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func -(a: Int64): Int64 {
        return a
    }
}

// 形态 4：extend 内建二元运算符重载——算术 token（`/`、`%`、`*`）与位运算（`&`、`^`、`|`）。
//   `UInt8` 上取余同属整数表（MOD 只覆盖整数类型）。
// 1.0.5 / 1.1.3 一致：7 条 OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR，Range 均为 (行,5)..(行,6)。
// FILE: oprBuiltinBinaryArith.cj
extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func /(a: Int64): Int64 {
        return a
    }

    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func %(a: Int64): Int64 {
        return a
    }
}

extend UInt8 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func %(a: UInt8): UInt8 {
        return a
    }
}

extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func *(a: Int64): Int64 {
        return a
    }

    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func &(a: Int64): Int64 {
        return a
    }
}

extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func ^(a: Int64): Int64 {
        return a
    }

    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func |(a: Int64): Int64 {
        return a
    }
}

// 形态 5：位移与幂——形参类型与宿主类型**不同**也命中（IsBuiltinBinaryExpr :157-170 对 LSHIFT/RSHIFT 分支；
//   `**` 走 SEMA_EXP_TYPES :131-136，只允许 `Int64 ** UInt64 → Int64` 与 `Float64 ** Int64|Float64 → Float64`）。
// 1.0.5 / 1.1.3 一致：4 条 OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR，Range 均为 (行,5)..(行,6)。
// FILE: oprBuiltinBinaryShiftExp.cj
extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func <<(a: Int32): Int64 {
        return this
    }
}

extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func >>(a: Int64): Int64 {
        return this
    }
}

extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func **(a: UInt64): Int64 {
        return 1
    }
}

extend Float64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func **(a: Float64): Float64 {
        return a
    }
}

// 形态 6：二元表“形参类型不在候选表”→ 合法（IsBuiltinBinaryExpr 为假）。`+` 只覆盖同类型算术。
// 1.0.5 / 1.1.3 一致：零 error（1 条 chir_dce_unused_function warning，不写标记）。
// FILE: oprBuiltinBinaryMissLegal.cj
extend Int64 {
    operator func +(a: Int32): Int64 {
        return this + Int64(a)
    }
}

// 形态 7：内建命中但声明的返回类型与内建返回类型不一致（`/` on Int64 内建返回 Int64、`!` on Bool 内建返回 Bool）。
//   官方三个报告点都不检查返回类型 → 只有 built_in_* 一条（r1 / r2 实测）；本项目额外报
//   RETURN_TYPE_INCOMPATIBLE（官方无对应 kind，按规则不写标记，见头部第 8 条）。
// 1.0.5 / 1.1.3 一致：(2,5)..(2,6) 二元 + (9,5)..(9,6) 一元。
// FILE: oprBuiltinReturnTypeMismatch.cj
extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>o<!>perator func /(a: Int64): Bool {
        let _ = a
        return true
    }
}

extend Bool {
    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>o<!>perator func !(): Int64 {
        return 1
    }
}

// 形态 8：形参个数违规。
//   · 0 形参 + 非一元运算符（`+`、`==`、`[]`）→ invalid_num_parameter（TypeCheckDecl.cpp:176 / :203）。
//     `+` 不在 UNARY_EXPR_TYPE_MAP：仓颉没有一元正号运算符（本项目却把 0 形参 `+` 归一化成 unaryPlus，见头部第 2 条）。
//   · 1 形参 + 一元运算符（`!`）→ invalid_num_parameter（:184）。
// 1.0.5 / 1.1.3 一致：3 条 INVALID_OPERATOR_PARAMETER_COUNT，Range 均为 (行,5)..(行,6)。
// FILE: oprZeroParamNotUnary.cj
extend Int64 {
    <!INVALID_OPERATOR_PARAMETER_COUNT!>o<!>perator func +(): Int64 {
        return 1
    }
}

struct OPRZeroParamEq {
}

extend OPRZeroParamEq {
    <!INVALID_OPERATOR_PARAMETER_COUNT!>o<!>perator func ==(): Bool {
        return true
    }
}

struct OPRZeroParamIndex {
}

extend OPRZeroParamIndex {
    <!INVALID_OPERATOR_PARAMETER_COUNT!>o<!>perator func [](): Int64 {
        return 1
    }
}

// 形态 9：形参个数违规——一元运算符带 1 个形参、二元运算符带 2 个形参（≥2 走 :193 的 default 分支）。
// 1.0.5 / 1.1.3 一致：2 条 INVALID_OPERATOR_PARAMETER_COUNT，(5,5) 与 (14,5) 各 1 字符。
// FILE: oprOneParamUnary.cj
struct OPROneParamNot {
}

extend OPROneParamNot {
    <!INVALID_OPERATOR_PARAMETER_COUNT!>o<!>perator func !(a: Int64): Bool {
        return true
    }
}

struct OPROneParamPlus {
}

extend OPROneParamPlus {
    <!INVALID_OPERATOR_PARAMETER_COUNT!>o<!>perator func +(a: Int64, b: Int64): Int64 {
        return a
    }
}

// 形态 10：形参个数违规——class 宿主（baseTy 是类类型，不会命中内建表；class 成员不加修饰符时隐式 private，
//   a3 实测零诊断）。
// 1.0.5 / 1.1.3 一致：2 条 INVALID_OPERATOR_PARAMETER_COUNT，(2,5) 与 (6,5) 各 1 字符。
// FILE: oprClassHostArity.cj
class OPRClassHostArity {
    <!INVALID_OPERATOR_PARAMETER_COUNT!>o<!>perator func +(a: OPRClassHostArity, b: OPRClassHostArity): OPRClassHostArity {
        return a
    }

    <!INVALID_OPERATOR_PARAMETER_COUNT!>o<!>perator func ==(): Bool {
        return true
    }
}

// 形态 11：合法对照——自定义 struct 的一元（0 形参 `!`）与二元（1 形参 `+`）。
//   IsUnaryOperator / IsBinaryOperator 只看 token，IsBuiltinUnaryExpr / IsBuiltinBinaryExpr 只看宿主类型，
//   两者都成立且宿主不是内建类型 → 零诊断。
// 1.0.5 / 1.1.3 一致：零 error。
// FILE: oprLegalArity.cj
struct OPRLegalUnary {
}

extend OPRLegalUnary {
    operator func !(): Bool {
        return true
    }
}

struct OPRLegalBinary {
}

extend OPRLegalBinary {
    operator func +(a: OPRLegalBinary): OPRLegalBinary {
        return a
    }
}

// 形态 12：`[]` setter 合法形态——位置下标 + 恰好一个名为 `value` 的具名形参 + 返回 Unit。
//   HandIndexOperatorOverload 三条都不触发；extend 宿主与 class 宿主同理。官方**不校验下标形参的类型**
//   （只数形参、只查具名名字与返回类型），`index: Int32` 的 setter 同样合法（p27 / r5 实测零诊断）。
// 1.0.5 / 1.1.3 一致：零 error。
// FILE: oprSubscriptSetterLegal.cj
struct OPRSetterLegal {
    public var data: Array<Int64> = [0]
}

extend OPRSetterLegal {
    operator func [](index: Int64, value!: Int64): Unit {
        this.data[index] = value
    }
}

class OPRSetterLegalClass {
    private var data: Array<Int64> = [0]

    operator func [](index: Int64, value!: Int64): Unit {
        this.data[index] = value
    }
}

struct OPRSetterIndexTypeLegal {
    public var data: Array<Int64> = [0]
}

extend OPRSetterIndexTypeLegal {
    operator func [](index: Int32, value!: Int64): Unit {
        this.data[Int64(index)] = value
    }
}

// 形态 13：`[]` setter 显式返回类型不是 Unit → INVALID_SUBSCRIPT_ASSIGN_RETURN，锚返回类型整段
//   （TypeCheckDecl.cpp:227-234 `MakeRange(retType->begin, retType->end)`）。class / extend 宿主同理。
// 1.0.5 / 1.1.3 一致：(4,52)..(4,57) 与 (15,52)..(15,57)。
// FILE: oprSubscriptSetterReturnType.cj
class OPRSetterReturnClass {
    private var data: Array<Int64> = [0]

    operator func [](index: Int64, value!: Int64): <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>Int64<!> {
        this.data[index] = value
        return this.data[index]
    }
}

struct OPRSetterReturnExtend {
    public var data: Array<Int64> = [0]
}

extend OPRSetterReturnExtend {
    operator func [](index: Int64, value!: Int64): <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>Int64<!> {
        this.data[index] = value
        return value
    }
}

// 形态 14：`[]` setter 无显式返回类型但函数体推导为非 Unit（表达式体 `{ this.data[index] }` 类型 Int64）
//   → 锚 `[]`（`MakeRange(fd.identifier)`，TypeCheckDecl.cpp:227-231）。class / extend 宿主同理。
// 1.0.5 / 1.1.3 一致：(4,19)..(4,21) 与 (14,19)..(14,21)，均为 `[]` 2 字符。
// FILE: oprSubscriptSetterInferredReturn.cj
class OPRSetterInferredClass {
    private var data: Array<Int64> = [0]

    operator func <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>[]<!>(index: Int64, value!: Int64) {
        this.data[index]
    }
}

struct OPRSetterInferredExtend {
    public var data: Array<Int64> = [0]
}

extend OPRSetterInferredExtend {
    operator func <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>[]<!>(index: Int64, value!: Int64) {
        this.data[index]
    }
}

// 形态 15：`[]` setter 形参总数 ≤ 1（只有一个具名 `value!`，名字合法故无 PARAM）→ PARAM_NUM，锚 `[]`
//   （TypeCheckDecl.cpp:221-225）。class / extend 宿主同理。
// 1.0.5 / 1.1.3 一致：2 条 INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM，Range 均为 (行,19)..(行,21)（`[]` 2 字符）。
// FILE: oprSubscriptSetterNoPositional.cj
struct OPRSetterNoPositionalExtend {
    public var data: Array<Int64> = [0]
}

extend OPRSetterNoPositionalExtend {
    operator func <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM!>[]<!>(value!: Int64): Unit {
        this.data[0] = value
    }
}

class OPRSetterNoPositionalClass {
    private var data: Array<Int64> = [0]

    operator func <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM!>[]<!>(value!: Int64): Unit {
        this.data[0] = value
    }
}

// 形态 16：`[]` setter 唯一具名形参不叫 `value` → PARAM（锚该形参 identifier 1 字符）+ PARAM_NUM（锚 `[]`），
//   两条同一声明同时报（TypeCheckDecl.cpp:210-225 两段独立 if，PARAM 段不 return）。
//   **不报** RETURN：具名形参非空时 :226 的条件短路。
// 1.0.5 / 1.1.3 一致：(6,19) PARAM_NUM、(6,22) PARAM、(14,19) PARAM_NUM、(14,22) PARAM。
// FILE: oprSubscriptSetterBadNamedOnly.cj
struct OPRSetterBadNamedOnlyExtend {
    public var data: Array<Int64> = [0]
}

extend OPRSetterBadNamedOnlyExtend {
    operator func <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM!>[]<!>(<!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>v<!>!: Int64): Unit {
        this.data[0] = v
    }
}

class OPRSetterBadNamedOnlyClass {
    private var data: Array<Int64> = [0]

    operator func <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM!>[]<!>(<!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>v<!>!: Int64): Unit {
        this.data[0] = v
    }
}

// 形态 17：`[]` setter 两个非法具名形参 → 只报一条 PARAM，锚**第一个**非法具名形参（`w` 之前的 `v`），
//   第二个作为 AddHint（JSON 只输出 MainHint，hint 范围不进 MainHint.Range）。形参数 > 1，不报 PARAM_NUM。
// 1.0.5 / 1.1.3 一致：(6,36)..(6,37) 与 (15,36)..(15,37)。
// FILE: oprSubscriptSetterTwoBadNamed.cj
struct OPRSetterTwoBadNamedExtend {
    public var data: Array<Int64> = [0]
}

extend OPRSetterTwoBadNamedExtend {
    operator func [](index: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>v<!>!: Int64, w!: Int64): Unit {
        this.data[index] = v
        this.data[index] = w
    }
}

class OPRSetterTwoBadNamedClass {
    private var data: Array<Int64> = [0]

    operator func [](index: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>v<!>!: Int64, w!: Int64): Unit {
        this.data[index] = v
        this.data[index] = w
    }
}

// 形态 18：`[]` setter `value!` 合法但后面还有另一个具名形参 `w!`（**不是**最后一个形参）→ PARAM 锚 `w`；
//   **不报** RETURN（:226 的 invalidNamedParams 非空）；不报 PARAM_NUM（总数 > 1）。
// 1.0.5 / 1.1.3 一致：(6,51)..(6,52) 与 (15,51)..(15,52)。
// FILE: oprSubscriptSetterValuePlusBad.cj
struct OPRSetterValuePlusBadExtend {
    public var data: Array<Int64> = [0]
}

extend OPRSetterValuePlusBadExtend {
    operator func [](index: Int64, value!: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>w<!>!: Int64): Unit {
        this.data[index] = value
        this.data[index] = w
    }
}

class OPRSetterValuePlusBadClass {
    private var data: Array<Int64> = [0]

    operator func [](index: Int64, value!: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>w<!>!: Int64): Unit {
        this.data[index] = value
        this.data[index] = w
    }
}

// ── 已删除的形态（探到但不可写 / 被 parser 拦，官方 kind 与 Range 记录在此，不放标记）──
// 1. operator 形参带默认值：`operator func -(a: OPRX = OPRX(), b: OPRX)` → parser 报
//    parse_expected_dot_lparen "expected ',' or ')', found '='"，锚 `=` 1 字符（g1 实测，两版一致）。
//    官方 sema_operator_overload_can_not_has_default_param（DiagnosticSema.def:59）在 v1.0.0 全仓无引用点，
//    **不可达**；本项目 CANNOT_HAVE_DEFAULT_PARAM（CfirFunctionSemanticsChecker.kt:426）对 operator 形参
//    报 "optional parameter cannot be used in operator overloading function"，属 CFIR 侧可达的补充规则。
// 2. `operator func []()`（无形参）：可写，已在形态 8。
// 3. `operator func [](index: Int64, value!: Int64, w: Int64)` / `[](value!: Int64, index: Int64)`：parser 报
//    parse_named_parameter_after_unnamed "unnamed parameters must come before named parameters"
//    （锚整个无名形参，p20 / p21 实测），故“最后一个形参是位置形参”的 getter 门控在 parser 之后不可达；
//    HandIndexOperatorOverload :206-209 的 `!params[lastIndex]->isNamedParam` 分支只有“全位置形参”一条路径。
// 4. `operator func ++()` / `--()` / `&&` / `||`：parser 报 parse_invalid_overloaded_operator
//    "cannot overload operator '++' "，锚运算符 2 字符（b4 / b5 / c3 / c12 实测），故 UNARY_EXPR_TYPE_MAP 的
//    INCR/DECR 与 BINARY_EXPR_TYPE_MAP 的 AND/OR 在重载语境不可达。
// 5. `operator func ()(a: T)` / `()`：parser 报 parse_invalid_overloaded_operator "cannot overload operator '(' "
//    （p29 / p30 实测），官方 TypeCheckDecl.cpp:157 对 LPAREN 的提前返回是死路径。
// 6. `operator func [](index: Int64, set b: Int64)`：parse 报 parse_expected_right_delimiter / parse_expected_character
//    （g2 实测）；仓颉具名形参语法是 `name!: T`。
// 7. `static operator func ...`：parse 报 parse_conflict_modifier "'operator' and 'static' modifiers conflict"
//    （g3 实测，锚 `static` 整 token 8 字符）。
// 8. 官方对 `extend` primitive 声明 `!=` / `<` / `<=` / `>=` / `>` / `==` 内建重载时会**伴随**报
//    sema_extend_member_cannot_shadow（锚运算符 token）或 sema_weak_visibility（c8 / c10 / p05-p09 实测）；
//    本文件为避免伴随诊断选 `-` / `/` / `%` / `*` / 位运算 / 位移 / `**` 作内建二元命中形态。
// 9. `extend String { operator func ==(a: String): Bool }`：伴随 sema_inherit_member_kind_inconsistent
//    （String 已有 const `==`，c6 实测），故 String 不作合法对照宿主。
// 10. `extend String { operator func !(): Bool }`（p14）与 `extend Bool { operator func -(): Bool }`（b7）
//     均零诊断但带 chir_dce_unused_function warning；本文件用 Float64 承载 `!` 的表外形态（形态 2）。
