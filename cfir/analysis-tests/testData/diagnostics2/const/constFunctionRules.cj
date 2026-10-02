// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5（准）与 1.1.3 两版实测，静态库模式 `cjc <段>.cj -o out.a --output-type=staticlib
// --diagnostic-format json`；每个 `// FILE:` 段单独编译（官方整包编译时一处 parse 错误会吞掉其余段的语义诊断）。
// 1.1.3 本机在 CHIR/codegen 阶段对含 const 构造器 / const 函数的单元会 LLVM ERROR 崩溃
// （"Reflection format: modifier type is invalid" / "EnumReflection format: operand numbers error"），
// 崩在语义检查之后，语义诊断 JSON 仍完整产出，本文件按语义诊断写期望。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围；
// 官方对不同表达式形态的锚点宽度不同（见各段注释：调用锚被调函数名、下标锚基表达式、赋值锚整条赋值、for 锚整条 for 语句）。
// 两条官方事实决定了本文件各段的写法（都是实测，不是推测）：
//   (1) `static var` 成员同样算 var 字段：ChkClassDeclHasConstInitNoVarMember 只排除 prop，不排除 static，
//       所以"static var + const init"就会报 CLASS_CONST_INIT_WITH_VAR（见形态 15）。
//   (2) 子类的 `const init()` 若隐式调用父类构造器（父类有显式 `init()`），官方把**整条 const init 声明**
//       当作 expect_const 的锚点（Range 从 `init` 标识符到函数体右花括号）；显式写 `super(...)` 时只锚 `super`。
//       父类的 `init() {}`（空体）就是这类"有显式构造器"的父类，探针 h10 / e03 分别对应隐式 / 显式两种形态。
// 官方实现（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0；入口 src/Sema/TypeChecker.cpp:2107
//   CheckConstEvaluation → src/Sema/ConstEvaluationChecker.cpp 的 ConstEvaluationChecker::Check → ChkAllDeclsByWalk :180）：
//   - :18-22   DiagExpectConstFunc → sema_expect_const "expected 'const' function"，锚 MakeRange(fd.identifier)
//   - :24-44   DiagExpectConstExpr → sema_expect_const "expected 'const' expression"（isWeak）
//              / "expected 'const' expression guaranteed to be evaluated at compile time"（!isWeak），锚 MakeRange(expr)
//   - :46-50   DiagDefineVarInConstFunction → sema_cannot_define_var_in_const_funciton，锚 MakeRange(vda.identifier)
//   - :52-71   DiagNoConstInit → sema_no_const_init，锚 MakeRange(fd.identifier)，附注锚宿主类型名
//   - :73-93   DiagCannotDefineConstInit → sema_class_const_init_with_var，锚 MakeRange(第一个 const init 的 identifier)
//   - :243-258 ChkConstDecl（const VarDecl / VarWithPatternDecl / FuncDecl）；:260-284 ChkVarDeclAbstract：
//              isVar → DiagDefineVarInConstFunction；let 初值 ChkExpr(weak)；const 初值 ChkExpr(strong)
//   - :306-343 ChkFuncBody：具名形参默认值 ChkExpr(weak)；函数体 ChkBlock(weak)；ConstWalker 收集 inConstFunc /
//              constThisOrSuper（const 函数体里的 this/super/实例成员引用在 ChkRefExpr 中被放行）
//   - :345-357 ChkFuncDecl：!fd.isConst → DiagExpectConstFunc（const 函数体里的非 const 嵌套函数走这里）；构造器置 inInit
//   - :363-374 ChkDeclHasConstAllConstInitializers（仅 1.0.x，见"已删除的形态 6"）
//   - :376-389 ChkDeclNoConstInitNoConstFunc：无 const init 时，非 static 的 const 实例成员函数 → DiagNoConstInit
//   - :391-408 ChkStructDecl；:410-437 ChkClassDeclHasConstInitNoVarMember（**只作用于 class**）；:439-457 ChkClassDecl
//   - :459-485 ChkExtendDecl（extend 成员看宿主类型有没有 const init）
//   - :487-501 ChkExpr（不在 dispatchTable :860 里的表达式按整节点报）；:617-650 ChkRefExpr；:652-665 ChkCallExpr；
//     :760-784 ChkMemberAccess；:786-806 ChkSubscriptExpr；:808-826 ChkBlock；:828-837 ChkAssignExpr；:839-845 ChkIncOrDecExpr
//   文案：include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:165-168（:171 sema_annotation_no_const_init 属注解族，本文件不覆盖）
// 本项目 CFIR 名（cfir/checkers/.../diagnostics/CfirDiagnosticsList.kt 的 CONST_DECLARATION 组，:1784-1798）：
//   EXPECT_CONST(kind)、CANNOT_DEFINE_VAR_IN_CONST_FUNCTION、NO_CONST_INIT、CLASS_CONST_INIT_WITH_VAR；
//   生产者 cfir/checkers/src/.../declaration/CfirConstDeclarationChecker.kt:139 / :197 / :833 / :937 / :950；
//   锚点工具 CfirDeclarationDiagnosticSources.kt:362 constructorNameDiagnosticSource(includeConstKeyword) / :267
//   functionNameDiagnosticSource / :392 fieldVariableNameDiagnosticSource；
//   文案表 CfirErrorsDefaultMessages.kt:1095-1098。按源码读取（未跑 CFIR）可预判的锚点差异：
//   CLASS_CONST_INIT_WITH_VAR 用 constructorNameDiagnosticSource(includeConstKeyword = true)（含 `const init` 整段，
//   官方只锚 `init`）；CANNOT_DEFINE_VAR_IN_CONST_FUNCTION 用 variable.source（整条 var 声明，官方只锚变量名）；
//   NO_CONST_INIT 用 functionNameDiagnosticSource()（与官方一致）；EXPECT_CONST 的表达式锚点为 receiver..callee 或整条表达式，
//   与官方按表达式形态（调用/下标/赋值/整条声明）分别取被调名/基表达式/整条赋值的做法不完全一致。以上只是源码预判，
//   实际差异以 CFIR 实跑为准。
// 已有覆盖不重复：EXPECT_CONST 在 testData/llt/const_evaluation/err_*.cj（err_unsafe / err_tuple_lit /
//   err_try_with_resources / err_this_00..03 / err_sync / err_return / err_lit_const / err_let / err_interface /
//   err_interface_call_non_const_func / err_global_func_use_let / err_func_var）与 testData/macro、
//   testData/llt/function/defaultParameter_pkg_02/test_mutation.cj 已有用例；本文件把 const 规则按官方
//   ConstEvaluationChecker 的四条报告点重排为诊断矩阵，NO_CONST_INIT / CLASS_CONST_INIT_WITH_VAR /
//   CANNOT_DEFINE_VAR_IN_CONST_FUNCTION 在全 testData 无内联标记。
//   未纳入：INTERPOLATION_IN_CONST_PATTERN（官方 TypeCheckPattern.cpp:267，属模式族）、SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR（CJMP 专有）。

// ── 形态 1：class 只有 const init 且含 var 字段
// kind：sema_class_const_init_with_var "cannot define 'const' constructor with 'var' members in class"
// 锚点：const init 的 `init` 标识符（MakeRange(第一个 const init 的 identifier)），1 个字符；附注锚 var 字段名。
// 1.0.5 / 1.1.3 一致 Range (4,11)..(4,15)。探针 c02 / e09。
// FILE: constInitWithVarMember.cj
class ConstInitWithVarMemberClass {
    var x: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
        x = 1
    }
}

// ── 形态 2：子类只有 const init，父类有显式 `init() {}`（编译器插入隐式 super()）
// 两条诊断：隐式 super() → weak expect_const，锚**整条 const init 声明**（8,11)..(10,6)（嵌套标记，外层 EXPECT_CONST）；
//   var 字段 → class_const_init_with_var 锚 `init`（8,11)..(8,15)（内层标记）。
// 1.0.5 / 1.1.3 一致。探针 h10（同 c47 / d05 / d06）。
open class ConstImplicitSuperBase {
    init() {}
}

class ConstImplicitSuperSub <: ConstImplicitSuperBase {
    var x: Int64 = 0

    const <!EXPECT_CONST!><!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
        this.x = 1
    }<!>
}

// ── 形态 3：class 无 const init 但有非 static 的 const 实例成员函数
// kind：sema_no_const_init "cannot define 'const' member function without 'const' constructor"
// 锚点：成员函数名 `value`（MakeRange(fd.identifier)），附注锚类名。1.0.5 / 1.1.3 一致 Range (8,16)..(8,21)。探针 c03。
// FILE: constFuncWithoutConstInit.cj
class ConstFuncWithoutConstInitClass {
    var x: Int64 = 0

    init() {
        x = 1
    }

    const func <!NO_CONST_INIT!>value<!>(): Int64 {
        return x
    }
}

// ── 形态 4：extend 块里的 const 成员函数，宿主类型只有普通 init（ChkExtendDecl :459 → ChkDeclNoConstInitNoConstFunc）
// kind：sema_no_const_init，锚成员函数名 `doubled`，附注锚宿主类型名 "…to this class"。1.0.5 / 1.1.3 一致 Range (10,16)..(10,23)。
// 探针 d11。
// FILE: constFuncInExtendWithoutConstInit.cj
class ConstExtendPlainTarget {
    var x: Int64 = 0

    init() {
        x = 1
    }
}

extend ConstExtendPlainTarget {
    const func <!NO_CONST_INIT!>doubled<!>(): Int64 {
        return this.x * 2
    }
}

// ── 形态 5：接口成员的非 static const 实现函数，实现类无 const init（接口继承路径同一条诊断）
// kind：sema_no_const_init，锚实现成员名 `f`。1.0.5 / 1.1.3 一致 Range (12,23)..(12,24)。探针 c15。
// FILE: constFuncImplementingInterface.cj
interface ConstFuncTargetInterface {
    func f(): Int64
}

class ConstFuncInterfaceImpl <: ConstFuncTargetInterface {
    var x: Int64 = 0

    init() {
        x = 1
    }

    public const func <!NO_CONST_INIT!>f<!>(): Int64 {
        return x
    }
}

// ── 形态 6：const 函数体里定义 var 局部（ChkVarDeclAbstract :260 → DiagDefineVarInConstFunction :46）
// kind：sema_cannot_define_var_in_const_funciton "cannot define 'var' variable in 'const' function"（官方 kind 拼写 funciton）
// 锚点：var 变量名 `a`，1 个字符。1.0.5 / 1.1.3 一致 Range (5,13)..(5,14)。探针 c04。
// FILE: varInConstFunc.cj
class VarInConstFuncClass {
    const init() {
    }

    const func f(): Int64 {
        var <!CANNOT_DEFINE_VAR_IN_CONST_FUNCTION!>a<!> = 1
        return a
    }
}

// ── 形态 7：const 构造器体里定义 var 局部
// kind：sema_cannot_define_var_in_const_funciton，锚变量名 `a`。1.0.5 / 1.1.3 一致 Range (5,13)..(5,14)。探针 d13。
// FILE: varInConstInit.cj
class VarInConstInitClass {
    let x: Int64

    const init() {
        var <!CANNOT_DEFINE_VAR_IN_CONST_FUNCTION!>a<!> = 1
        this.x = a
    }
}

// ── 形态 8：const 函数里的 lambda 体内定义 var（lambda 的 FuncBody 同样按 const 上下文检查）
// kind：sema_cannot_define_var_in_const_funciton，锚变量名 `a`。1.0.5 / 1.1.3 一致 Range (5,26)..(5,27)。探针 d18。
// FILE: varInLambdaInConstFunc.cj
class VarInLambdaConstClass {
    const init() {
    }

    const func f(): Int64 {
        let g = { => var <!CANNOT_DEFINE_VAR_IN_CONST_FUNCTION!>a<!> = 1
            a }
        return g()
    }
}

// ── 形态 9：const 函数体内声明非 const 嵌套函数（ChkFuncDecl :345 → DiagExpectConstFunc :18）
// kind：sema_expect_const "expected 'const' function"，锚嵌套函数名 `g`。1.0.5 / 1.1.3 一致 Range (5,14)..(5,15)。探针 d15。
// FILE: nestedPlainFuncInConstFunc.cj
class NestedPlainFuncClass {
    const init() {
    }

    const func f(): Int64 {
        func <!EXPECT_CONST!>g<!>(): Int64 {
            return 1
        }
        return g()
    }
}

// ── 形态 10：const 构造器体内声明非 const 嵌套函数，同一 kind
// kind：sema_expect_const "expected 'const' function"，锚 `g`。1.0.5 / 1.1.3 一致 Range (5,14)..(5,15)。探针 d17。
// FILE: nestedPlainFuncInConstInit.cj
class NestedPlainFuncInitClass {
    let x: Int64

    const init() {
        func <!EXPECT_CONST!>g<!>(): Int64 {
            return 1
        }
        this.x = g()
    }
}

// ── 形态 11：static const 变量初值是数组字面量（ChkVarDeclAbstract const → ChkExpr(strong) → ChkArrayLit :605）
// kind：sema_expect_const "expected 'const' expression guaranteed to be evaluated at compile time"，
//       hint "expressions of type 'Array' are not constant"，锚整个数组字面量 `[1, 2]`。
// 1.0.5 / 1.1.3 一致 Range (5,38)..(5,44)。探针 c06。
// FILE: staticConstArrayLiteral.cj
class ConstArrayLiteralClass {
    const init() {
    }

    static const v: Int64 = 1
    static const arr: Array<Int64> = <!EXPECT_CONST!>[1, 2]<!>
}

// ── 形态 12：函数内 const 局部变量初值引用 var 局部（strong：!vd.isConst 为假）
// kind：sema_expect_const（strong 文案），锚引用 `t`。1.0.5 / 1.1.3 一致 Range (3,22)..(3,23)。探针 c39。
// FILE: localConstVarNonConstInit.cj
func localConstVarHolder(): Int64 {
    var t = 1
    const a: Int64 = <!EXPECT_CONST!>t<!>
    return a
}

// ── 形态 13：顶层 const 变量初值是函数调用（strong）；官方对调用只锚被调函数名，不含括号与实参
// kind：sema_expect_const（strong），锚 `toplevelConstHelper`。1.0.5 / 1.1.3 一致 Range (5,20)..(5,39)。探针 c27。
// FILE: toplevelConstFuncCall.cj
func toplevelConstHelper(): Int64 {
    return 1
}

const TOP: Int64 = <!EXPECT_CONST!>toplevelConstHelper<!>()

// ── 形态 14：const 函数调用非 const 实例成员函数（weak："expected 'const' expression"，锚被调函数名）
// 1.0.5 / 1.1.3 一致 Range (10,16)..(10,23)。探针 f01（同 c05，但本段类里没有 var 字段，只有这一条诊断）。
// FILE: constFuncCallsNonConstMember.cj
class ConstCallsNonConstMemberClass {
    const init() {
    }

    func mutable(): Int64 {
        return 1
    }

    const func f(): Int64 {
        return <!EXPECT_CONST!>mutable<!>()
    }
}

// 以下形态 15–20 的类都只有 `static var t`（没有实例字段）：static var 同样触发 CLASS_CONST_INIT_WITH_VAR（官方 :410-437
//   只排除 prop），所以每段都是两条诊断：weak expect_const 锚表达式 + class_const_init_with_var 锚 const init 的 `init`（4,11)..(4,15)。
//   const init 的空体（`const init() {\n}`）不算"主构造器"，仍算 HasConstInit（探针 h09 / f02–f07 与 1.0.5 / 1.1.3 一致）。

// ── 形态 15：const 函数引用 static var 成员（ChkRefExpr :617：static var 不是 const，也不在 weak 局部白名单）
// 两条诊断：expect_const 锚 `t`（8,16)..(8,17)；class_const_init_with_var 锚 `init`（4,11)..(4,15)。探针 f02 / h09。
// FILE: constFuncRefStaticVar.cj
class ConstRefStaticVarClass {
    static var t: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
    }

    const func f(): Int64 {
        return <!EXPECT_CONST!>t<!>
    }
}

// ── 形态 16：const 函数里 let 局部初值引用 var 局部
// 两条诊断：expect_const 锚 `t`（8,17)..(8,18)；class_const_init_with_var 锚 `init`（4,11)..(4,15)。探针 f03。
// FILE: constFuncLocalLetNonConstInit.cj
class ConstLocalLetClass {
    static var t: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
    }

    const func f(): Int64 {
        let a = <!EXPECT_CONST!>t<!>
        return a
    }
}

// ── 形态 17：const 函数里给 static var 成员赋值（ChkAssignExpr :828：不在 const init 内且左值不是实例字段 → 整条赋值不是常量）
// 两条诊断：expect_const 锚整条赋值 `t = 1`（8,9)..(8,14)；class_const_init_with_var 锚 `init`（4,11)..(4,15)。探针 f04。
// FILE: constFuncAssignStaticVar.cj
class ConstAssignStaticVarClass {
    static var t: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
    }

    const func f(): Unit {
        <!EXPECT_CONST!>t = 1<!>
    }
}

// ── 形态 18：const 函数里自增 static var 成员（ChkIncOrDecExpr :839 固定 weak）
// 两条诊断：expect_const 锚整条 `t++`（8,9)..(8,12)；class_const_init_with_var 锚 `init`（4,11)..(4,15)。探针 f05。
// FILE: constFuncIncrementStaticVar.cj
class ConstIncrStaticVarClass {
    static var t: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
    }

    const func f(): Unit {
        <!EXPECT_CONST!>t++<!>
    }
}

// ── 形态 19：const 函数里的 lambda 捕获 static var 成员
// 两条诊断：expect_const 锚 `t`（8,22)..(8,23)；class_const_init_with_var 锚 `init`（4,11)..(4,15)。探针 f06。
// FILE: constFuncLambdaCaptureStaticVar.cj
class ConstLambdaCaptureClass {
    static var t: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
    }

    const func f(): Int64 {
        let g = { => <!EXPECT_CONST!>t<!> }
        return g()
    }
}

// ── 形态 20：const 函数的具名形参默认值引用 var 局部（ChkFuncBody :306：形参 assignment 按 weak 检查）
// 两条诊断：expect_const 锚默认值 `t`（7,30)..(7,31)；class_const_init_with_var 锚 `init`（4,11)..(4,15)。探针 f07。
// FILE: constFuncDefaultParamNonConst.cj
class ConstDefaultParamClass {
    static var t: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
    }

    const func f(a!: Int64 = <!EXPECT_CONST!>t<!>): Int64 {
        return a
    }
}

// ── 形态 21：子类 const init 里显式 super(...)（ChkRefExpr :617：super 引用不在 constThisOrSuper 白名单）
// 两条诊断：expect_const 锚 `super` 关键字（13,9)..(13,14)；class_const_init_with_var 锚 `init`（12,11)..(12,15)。
// 1.0.5 / 1.1.3 一致。探针 e03。
// FILE: constInitExplicitSuperCall.cj
open class ConstExplicitSuperBase {
    let b: Int64

    init(b: Int64) {
        this.b = b
    }
}

class ConstExplicitSuperSub <: ConstExplicitSuperBase {
    var x: Int64 = 0

    const <!CLASS_CONST_INIT_WITH_VAR!>init<!>() {
        <!EXPECT_CONST!>super<!>(1)
        this.x = 1
    }
}

// ── 形态 22：const 函数里对顶层 var 数组下标（ChkSubscriptExpr :786：基表达式类型非元组/VArray → 锚基表达式）
// kind：weak expect_const，锚 `constSubscriptArr`（不含 `[0]`）。1.0.5 / 1.1.3 一致 Range (8,16)..(8,33)。探针 h08。
// FILE: constFuncSubscriptStaticVar.cj
var constSubscriptArr: Array<Int64> = [1, 2]

class ConstSubscriptClass {
    const init() {
    }

    const func f(): Int64 {
        return <!EXPECT_CONST!>constSubscriptArr<!>[0]
    }
}

// ── 形态 23：const 函数里的 for 语句（for 表达式不在 dispatchTable :860 里 → ChkExpr :487 整节点报）
// kind：weak expect_const，锚整条 for 语句（6,9)..(8,10)（跨 3 行）。1.0.5 / 1.1.3 一致。探针 h02。
// FILE: constFuncForLoop.cj
class ForConstClass {
    const init() {
    }

    const func f(): Int64 {
        <!EXPECT_CONST!>for (i in [1, 2]) {
            let d = i
        }<!>
        return 0
    }
}

// 合法对照段（1.0.5 零语义诊断；含 const 构造器 / const 函数的段在 1.1.3 codegen 阶段 LLVM 崩，语义 JSON 同样为空）：

// L1：class 有 const init，const 成员函数调用同类 const 成员函数。探针 h05。
// FILE: legalClassConstFuncs.cj
class LegalConstFuncsClass {
    const init() {
    }

    const func value(): Int64 {
        return 1
    }

    const func compute(a: Int64): Int64 {
        return a + value()
    }
}

// L2：struct 有 const init、const 函数与 var 字段（ChkClassDeclHasConstInitNoVarMember 只作用于 class）。探针 g06。
// FILE: legalStructConstInitWithVar.cj
struct LegalConstStruct {
    var x: Int64

    const init(x: Int64) {
        this.x = x
    }

    const func doubled(): Int64 {
        return this.x * 2
    }
}

// L3：顶层 const 函数与 const 变量，函数体/初值都是字面量。探针 h06。
// FILE: legalToplevelConst.cj
const legalTopConst: Int64 = 2

const func legalTopConstFunc(): Int64 {
    return 1
}

func callLegalTopConst(): Int64 {
    return legalTopConstFunc() + legalTopConst
}

// L4：static const 函数不需要 const init（ChkDeclNoConstInitNoConstFunc 跳过 static），可在含 var 字段的类里；
//     同类的普通成员函数也不受 const 约束。探针 g03。
// FILE: legalStaticConstFuncInClassWithVar.cj
class LegalStaticConstFuncClass {
    var x: Int64 = 0

    static const func f(): Int64 {
        return 1
    }

    func g(): Int64 {
        return x
    }
}

// L5：const 函数里只用 let 局部、if、元组下标、lambda、嵌套 const 函数、形参、实例 let 字段、枚举构造器、match 表达式。
//     探针 h01。枚举构造器之间必须写 `|`（c24 / f10 用换行分隔时官方报 parse_expected_decl）。
// FILE: legalConstFuncBodies.cj
enum LegalConstEnum {
    A | B
}

struct LegalConstBodyStruct {
    let k: Int64

    const init(k: Int64) {
        this.k = k
    }
}

class LegalConstBodyClass {
    let k: Int64 = 1

    const init() {
    }

    const func f(a: Int64, flag: Bool): Int64 {
        let b = a + 1
        let t = (b, k)
        let g = { => b }
        if (flag) {
            let c = t[0]
            return c + g()
        }
        const func inner(): Int64 {
            return k
        }
        let e = LegalConstEnum.A
        let z = inner()
        match (e) {
            case LegalConstEnum.A => z
            case LegalConstEnum.B => 1
        }
    }
}

// L6：extend 块里的 const 成员函数，宿主 struct 有 const init（ChkExtendDecl :459 看宿主类型）。探针 g05。
// FILE: legalExtendConstFunc.cj
struct LegalExtendConstStruct {
    let x: Int64

    const init(x: Int64) {
        this.x = x
    }
}

extend LegalExtendConstStruct {
    const func tripled(): Int64 {
        return this.x * 3
    }
}

// L7：const 构造器用 this(1) 委托给另一个带参 const init（this 必须是构造器首语句）；const 函数调用 const 构造器。探针 h03。
// FILE: legalConstInitDelegation.cj
class LegalConstDelegationClass {
    let v: Int64

    const init() {
        this(1)
    }

    const init(v: Int64) {
        this.v = v
    }

    const func make(): LegalConstDelegationClass {
        return LegalConstDelegationClass()
    }
}

// L8：非 const 函数的默认值不受 const 检查；普通类（var 字段 + 普通 init + 非 const 成员函数）合法。探针 h07。
// FILE: legalPlainFuncAndClass.cj
class LegalPlainClass {
    var x: Int64 = 0

    init() {
        x = 1
    }

    func f(a!: Int64 = x): Int64 {
        return a
    }
}

// L9：const 函数的具名形参默认值是常量；const init 体里只有 let 与字段赋值；const 函数读实例 let 字段。探针 g07。
// FILE: legalConstFuncDefaultParam.cj
class LegalConstDefaultParamClass {
    let v: Int64

    const init() {
        let a = 1
        this.v = a
    }

    const func f(a!: Int64 = 1): Int64 {
        return a + v
    }
}

// L10：const 函数调用别的类的 const 构造器。探针 h04。
// FILE: legalConstCallsConstCtor.cj
class LegalConstCtorOther {
    let x: Int64

    const init() {
        this.x = 1
    }
}

class LegalConstCtorCaller {
    const init() {
    }

    const func f(): LegalConstCtorOther {
        return LegalConstCtorOther()
    }
}

// L11：接口里的 const 成员函数声明（接口没有构造器，ChkStructDecl / ChkClassDecl 的 const init 规则不适用）。探针 c43。
// FILE: legalInterfaceConstFunc.cj
interface LegalConstIfaceDecl {
    const func f(): Int64
}

// 已删除的形态（取证 kind 与 Range 见下；1.0.5 与 1.1.3 两版一致，除注明外）：
//   1. `static const prop v: Int64 { get() { … } }`（const prop）
//      → parse_illegal_modifier_in_scope "unexpected modifier 'const' on property declaration in class body"
//        （锚 `const` 5 个字符，Range (n,12)..(n,17)）。const 属性不是合法语法，PROPERTY 族无对应形态。探针 c09。
//   2. `open const func f()` / `const open func f()` / `open class` 里的 `open const func`
//      → parse_conflict_modifier "'const' and 'open' modifiers conflict on function declaration"（锚 `const`）。
//        const 函数不可 open（ToOverride 族无对应形态）。探针 c57。
//   3. 成员 `const v: Int64 = 1` 漏写 static
//      → parse_expected_static_for_const_member_var "expected static before const member variable"（锚 `const` 5 个字符）。
//        属 parser 族，CfirDiagnosticsList 无同名诊断。探针 c30。
//   4. `static const v: Int64` 不写初始化器
//      → parse_const_expected_initializer "const variable declaration must be initialized"（锚类型后的 `;`/换行处 1 个字符）。
//        属 parser 族，CfirDiagnosticsList 无同名诊断。探针 c31。
//   5. `static const v: Int64 = 1` 加 `@Frozen`
//      → sema_illegal_use_of_annotation "variable cannot be modified with '@Frozen'"（锚 `static` 1 个字符）。
//        属注解适用性族（ILLEGAL_USE_OF_ANNOTATION），与 const 求值无关，不在本文件。探针 c21 / d21。
//   6. class 有 const init 时，普通（非 const）成员变量的初始化器不是常量：
//      `class C { static var t = 0; const init() {}; static let u: Int64 = t }`
//      → 1.0.5：sema_expect_const（strong）锚 `t`（ChkDeclHasConstAllConstInitializers :363 对所有非 prop 字段求值）；
//        1.1.3 不报（该版本只检查 const 字段）。版本差异，不写期望。探针 c60。
//      同理 `static var arr: Array<Int64> = [1, 2]` + const init：1.0.5 锚 `[1, 2]` 报 strong expect_const，1.1.3 不报。探针 c56 / d25。
//   7. const 函数里 `for` 循环含 `var s = 0`
//      → 1.0.5：sema_cannot_define_var_in_const_funciton 锚 `s` + sema_expect_const（weak）锚整个语句块
//        （Range 跨多行，ChkBlock 对失败语句报块级锚点）；1.1.3 只报 var 那一条。版本差异，不写期望。探针 c56。
//      纯 let 的 for 循环两版一致（形态 23），所以 for 形态写的是无 var 版本。
//   8. 同一 class 里同时声明同签名的 `init()` 与 `const init()`（"结构化 init + const init"）
//      → sema_overload_conflicts "constructor 'init' has overload conflicts"（锚 `init`），构造器不可重载
//        （INVALID-DECLARATION 族）；因此"既有结构化 init 又有 const init"这一形态在官方不可表达。
//        不同形参列表的两个 const init 合法（见 L7）。探针 e01 / e02 / e06 / e07 / e08。
//   9. const init 里 `let a = 1` 之后再 `this(a)` 委托
//      → sema_illegal_place_of_calling_this_or_super "call to 'this' must be first expression in constructor"（锚 `this`）
//        + sema_class_uninitialized_field（锚字段名）。探针 g04。
//  10. `final class C { const init() … }`
//      → 1.0.5 / 1.1.3：parse 错误 "'final' 不是顶层声明的修饰符"（parse_expected_decl）。与 const 无关。探针 c49。
//  11. 具名形参默认值必须写 `a!:`；写 `a: Int64 = t` → parse_expected_dot_lparen（锚 `=`）。探针 c19 / c20。
//  12. @Annotation 类缺 const init → sema_annotation_no_const_init "class with '@Annotation' should have 'const' constructor"
//      （锚 `@Annotation` 整段，外加 sema_annotation_non_public warning）。属注解族，CfirDiagnosticsList 无对应名。探针 c44 / c45。
//  13. const 函数调用同类非 const 构造器：`class C { init() {}; const init() {}; const func f(): C { return C() } }`
//      → 1.0.5 / 1.1.3：sema_overload_conflicts（同签名两个 init，见已删除形态 8）+ sema_ambiguous_constructor_match。探针 d23。
//  14. 枚举构造器之间必须写 `|`：`enum E { A\n B }` → parse_expected_decl "expected declaration, found B"（锚 `B`）。探针 c24 / f10。
//  15. struct 里的 `~init() { }`（析构函数）与 const 无关；class 的 mut 成员函数不能写 `mut`（parse_illegal_modifier_in_scope），
//      所以"class 里的 const 成员调用 mut 成员"形态不存在（见 mut/mutableFunctionReferenceRules.cj 已删除形态 2）。