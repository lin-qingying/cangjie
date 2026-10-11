// RUN_PIPELINE_TILL: FRONTEND
// 本文件覆盖"闭包 / lambda 捕获可变变量（var）"这一族诊断。行尾必须是 LF：CRLF 下官方把 lambda 的 Range
//   终点算到行尾的 `\r` 处（testData/llt/lambda 下的旧用例正是 CRLF，标记因此落在行尾），diagnostics2 统一用 LF。
// 取证方式（两版逐段单独编译，JSON 走 stderr）：
//   C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe 段.cj -o out.a --output-type=staticlib --diagnostic-format json
//   C:\Users\lin17\.cangjie\sdks\cangjie-sdk-windows-x64-1.1.3\cangjie\bin\cjc.exe 同上
//   1.1.3 在本机 codegen 阶段可能 LLVM 崩（"Reflection format: modifier type is invalid"），只取它的解析 / 语义诊断。
//   除特别注明外，两版 kind、消息、Range 完全一致，本文件不需要 LANGUAGE_VERSION 门禁。
// 官方实现：
//   - src/Sema/LegalityOfUsage/LegalityOfUsage.cpp:170-193 CheckClosures（按包遍历：三步——标记捕获 → SetFuncBodyCaptureKind → 诊断）
//     src/Sema/LegalityOfUsage/LegalityOfUsage.cpp:22-69 SetFuncBodyCaptureKind（唯一的 TRANSITIVE_CAPTURE 来源：
//       函数体里以 baseFunc 形式调用了 captureKind != NO_CAPTURE 的 lambda / 函数，且被调用者捕获的 var 不在本函数内声明）
//     src/Sema/LegalityOfUsage/LegalityOfUsage.cpp:95-116 MarkAndCheckRefExprVarCaptureStatus → src/Sema/TypeCheckExpr.cpp:98-135 SetCaptureKind
//     src/Sema/TypeChecker.cpp:1763-1784 CheckLegalUseOfClosure(Expr&, kind)（CAPTURE_VAR → DiagUseClosureCaptureVarAlone；
//       TRANSITIVE_CAPTURE → diag.Diagnose(e, kind, "lambda" 或函数名, "transitively", 同名)）
//     src/Sema/TypeChecker.cpp:1786-1818 CheckLegalUseOfClosure(Node&)（按节点位置分派 _cannot_assign / _return / _param / _expr）
//     src/Sema/Diags.cpp:571-589 DiagUseClosureCaptureVarAlone（DiagnoseRefactor(sema_use_func_capture_var_alone, expr, "lambda"/"function")，
//       为每个被捕获变量加 note "'x' is mutable"）
//     src/Parse/ParseExpr.cpp:924 `baseExpr->isBaseFunc = true`（解析期就给调用的基表达式打标；这解释了为什么
//       `while (h())` 里作为被调用的 `h` 不再被 _cannot_expr 检查、而 `let x = h` / `{a: Int64 => g}` 里的 `g` 会被检查）
//     src/Sema/TypeCheckExpr/LambdaExpr.cpp:124-133 DiagInferParamTyFail、:270-277 SynLamExpr（sema_lambdaExpr_must_have_type_annotation
//       的两个报告点，都在 SynLamExpr，锚第一个仍无类型的形参节点）
//   - 文案：include/cangjie/Basic/DiagnosticSema.def:47-50（sema_func_capture_var_cannot_assign / _cannot_return / _cannot_param / _cannot_expr）、
//     :172（sema_func_capture_var_not_ctype）、:173（sema_cfunc_cannot_capture_var）；
//     include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:74（sema_lambdaExpr_must_have_type_annotation）、
//     :75（sema_use_func_capture_var_alone）。
// CFIR 侧对应名（CfirErrors.kt 行号）：LAMBDA_MUST_HAVE_TYPE_ANNOTATION :327、USE_FUNC_CAPTURE_VAR_ALONE :328、
//   FUNC_CAPTURE_VAR_CANNOT_ASSIGN :329、_RETURN :330、_PARAM :331、_EXPR :332、CFUNC_CANNOT_CAPTURE_VAR :185、FUNC_CAPTURE_VAR_NOT_CTYPE :187。
//   生产者：CfirClosureCaptureUsageChecker.kt:113-142（USE_FUNC_CAPTURE_VAR_ALONE 与四个 CANNOT_*）、:79（FUNC_CAPTURE_VAR_NOT_CTYPE）、
//   CfirFunctionLambdaChecker.kt:171（LAMBDA_MUST_HAVE_TYPE_ANNOTATION，锚整个形参节点）、CfirCFuncCaptureCheckers.kt:83/111（CFUNC_CANNOT_CAPTURE_VAR）。
// 范围规则（1.0.5 / 1.1.3 实测一致）——这一族的两种锚点形状，差别来自官方用的诊断入口：
//   - sema_use_func_capture_var_alone 走 `DiagnoseRefactor(kind, expr)`（Diags.cpp:572-573）→ MakeRange(begin, end)，
//     锚**整个**被使用的闭包表达式：lambda 是整个 `{...}` 字面量（多行 lambda 从 `{` 起、到 `}` 止），具名局部函数是函数名标识符。
//   - sema_func_capture_var_cannot_assign / _return / _param / _expr 走 `diag.Diagnose(e, kind, ...)`（TypeChecker.cpp:1773/1779），
//     没有 MainHint.Range，官方在 JSON 里退化成 GetBegin() 一个字符：lambda 只锚 `{`、函数引用只锚标识符首字符。
//     **内联标记不跟这个窄锚**：本项目 range policy 要求锚点覆盖完整最小单位（单个 token 或以上，不切 token），
//     所以这里的标记按"整个被使用的闭包表达式"写——lambda 是整个 `{...}` 字面量、函数引用是整个标识符；
//     各段注释里的官方 Range 数字只是取证记录，二者对多字符锚点宽度不同是预期。
//   - sema_lambdaExpr_must_have_type_annotation 走 `DiagnoseRefactor(kind, *node)`，node 是第一个仍无类型的形参节点，实测锚形参名标识符。
//   位置决定 kind：变量声明的初始化器 → _cannot_assign；return 的表达式 → _cannot_return；调用实参 → _cannot_param；
//   其余位置（裸 lambda 表达式语句、lambda 函数体里的裸函数名）→ _cannot_expr。
//   cjc 的 MainHint.Range 结束列是开区间；官方 Range 与 CFIR 标记按上一条的两种口径分别书写。
// 取证时必须避开的干扰：
//   - `let {…} = …`（lambda 字面量紧跟 `let`）是解析错误 "expected identifier or pattern after 'let', found '{'"，两版都报，
//     而且因为 CanBeEmitted 只发第一个错误类别，同段的语义诊断被吞掉。`let name = {…}`、`var name = {…}`、`let name: T = {…}`
//     语法都合法（探针 kA/kB/kC），但右值推不出闭包类型时官方还会多报 sema_mismatched_types，本文件的声明形态一律写显式闭包类型。
//   - `w = {=> v}`（赋给 Int64 变量）除了 use_func_capture_var_alone 还会多报 sema_mismatched_types（闭包类型 vs Int64），
//     且因为 HandleDiagnose 的 HasPrevDiag 只按"同起点同文案"去重，同一段里的第二个 lambda 会被吞掉——同一段里
//     不放两处相同的 lambda。本文件的声明形态用 `var name: () -> Int64 = {…}` / `let name: () -> Int64 = {…}`。
//   - 实参位置的 lambda 类型不匹配会多报一条 sema_mismatched_types（锚同一段 lambda），本文件给实参形参 `() -> Int64` 精确类型排除。
//   - `let f = {=> v}`（把捕获 var 的 lambda 存进变量再调用）官方照样报 use_func_capture_var_alone——捕获 var 的闭包只能
//     字面量直接调用（`{=> v}()`）；合法对照段用 `let x = {=> v}()` 这种立即调用形态。
//   - 段内多余的 `func` 会在 CHIR 阶段多出 chir_dce_unused_function 警告（Level::WARNING，不参与框架比较），尽量避免。
// 已知 CFIR 诊断名 / 文案差异（诊断名与锚点按上一条口径书写，文案差异不参与内联比较）：
//   - CfirClosureCaptureUsageChecker 的四个 CANNOT_* 用文案 "cannot be returned" / "cannot be passed as an argument" /
//     "cannot be used as an expression"（CfirErrorsDefaultMessages.kt:911-913），官方是 "cannot be used as a return value" /
//     "... as a param" / "... as a expression"（DiagnosticSema.def:48-50）。
//   - FUNC_CAPTURE_VAR_CANNOT_PARAM / _RETURN 的官方报告点（函数引用 + 参数 / 返回值位置）在本机两个版本上不可达，
//     只对 lambda 报；见文末"已删除的形态"1-3。
// 已有覆盖不重复（testData/llt 旧套件，CRLF）：lambda/lambda_capture/（capture3、capture4、capture5、capture7、capture_transitive、
//   capture_var_as_decl、capture_var_as_param、capture_var_as_return、lambda_closures_301、capture_not_init_01）、
//   lambda/lambda04.cj、lambda/err_capture_var.cj、type_infer/lambda_param_10.cj、type_infer/lambda_param_11.cj、
//   type_infer/lambda_identity_requires_annotation.cj、record/function_cannot_capture_var1.cj、function/function_capture2.cj、
//   let/let001.cj、generics/generic_subst_perf.cj、ffi/semantic_contract/cstructFieldsNegative.cj、ffi/semantic_contract/cfuncSignaturesNegative.cj。
//   全 testData 里此前没有 FUNC_CAPTURE_VAR_CANNOT_PARAM / FUNC_CAPTURE_VAR_CANNOT_RETURN 的标记，故本文件重点写这两条与
//   if / while / do-while 逻辑块（官方把逻辑块当隐式 lambda）作为实参 / 返回值 / 赋值的形态。
// 各 `// FILE:` 段在测试框架里是同包多文件，段内顶层名（函数 / 类 / 结构 / 变量）全文件唯一，否则官方报 sema_redefinition、
//   CFIR 报 CLASSIFIER_REDECLARATION 并级联出 AMBIGUOUS_USE 噪声。官方整包编译不能用作取证，一次编译里只要有一个
//   错误类别，其余诊断会被 DiagnosticEngine 吞掉，所以下面的"两版实测"都是逐段单独编译的结果。

// ── 1. 直接捕获 var 的闭包被当作值使用 → sema_use_func_capture_var_alone（锚整个闭包表达式）

// FILE: lambdaAloneAsLetInitializer.cj
// 形态：函数里的 lambda 直接引用同层的 var 局部变量，再作 var / let 声明的初始化器（右值是闭包类型）。
// kind：sema_use_func_capture_var_alone "lambda capturing mutable variables needs to be called directly"，
//       note "'v' is mutable"；锚整个 lambda 字面量 `{=> v}`。1.0.5 / 1.1.3 一致：
//       Range (7,30)..(7,36) 与 (8,31)..(8,37)。
func lambdaAloneAsLetInitializer(): Unit {
    var v: Int64 = 1
    var first: () -> Int64 = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> v}<!>
    let second: () -> Int64 = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> v}<!>
    first()
    second()
}

// FILE: nestedFuncAloneAsLetInitializer.cj
// 形态：局部函数（`func g() { ... }` 声明）直接引用同层的 var 局部变量，再把函数名当值赋给 let 变量。
// kind：sema_use_func_capture_var_alone "function capturing mutable variables needs to be called directly"，锚函数名 `g`。
// 1.0.5 / 1.1.3 一致，Range (9,13)..(9,14)。
func nestedFuncAloneAsLetInitializer(): Unit {
    var v: Int64 = 1
    func g(): Int64 {
        return v
    }
    let h = <!USE_FUNC_CAPTURE_VAR_ALONE!>g<!>
    let _ = h
    return
}

// FILE: capturedLambdaAloneInsideLoopBlock.cj
// 形态：while 逻辑块（隐式 lambda）里给捕获 var 的 lambda 赋值。
// kind：sema_use_func_capture_var_alone，锚块内整个 lambda 字面量。1.0.5 / 1.1.3 一致，Range (6,17)..(6,23)。
func capturedLambdaAloneInsideLoopBlock(): Unit {
    var v: Int64 = 1
    while (true) {
        let x = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> v}<!>
        x()
        return
    }
}

// FILE: outerLambdaBindsCapturedInnerLambda.cj
// 形态：外层 lambda 的函数体里绑定了一个直接捕获 var 的内层 lambda。内层 lambda 是 CAPTURE_VAR 闭包，绑定到 let 变量
//       时官方锚**内层** lambda；外层 lambda 只调用内层闭包 ⇒ NO_CAPTURE，官方不报（SetFuncBodyCaptureKind 的"绑定"遍历
//       只看 VarDecl，闭包变量不是 var）。
// kind：sema_use_func_capture_var_alone，锚内层 lambda `{=> v}`。1.0.5 / 1.1.3 一致，Range (8,21)..(8,27)。
func outerLambdaBindsCapturedInnerLambda(): Unit {
    var v: Int64 = 1
    {=>
        let inner = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> v}<!>
        inner()}
    return
}

// ── 2. 传递捕获（TRANSITIVE_CAPTURE）：被使用的闭包本身不直接引用 var，而是调用了捕获 var 的闭包。
//    官方唯一把 captureKind 置成 TRANSITIVE_CAPTURE 的地方是 SetFuncBodyCaptureKind（LegalityOfUsage.cpp:22-69）。
//    与直接捕获的区别：SetFuncBodyCaptureKind 的调用遍历只把"被调用的闭包"声明在**自己函数体内**的 var 记为捕获，
//    外层函数的 var 会走"绑定"路径（CAPTURE_VAR）。所以 `{=> v}` 是直接捕获，`{=> f()}`（f 捕获 v）是传递捕获。

// FILE: transitiveCaptureFuncCannotAssign.cj
// 形态：`let x = g`（g 调用了直接捕获 var 的 f）走 VarDecl 分支。
// kind：sema_func_capture_var_cannot_assign "g captured a mutable variable transitively, g cannot be assigned to a variable"，
//       锚 g 的标识符（`diag.Diagnose(e, kind, target->identifier.Val(), "transitively", 同名)`）。
// 1.0.5 / 1.1.3 一致，Range (13,13)..(13,14)。
func transitiveCaptureFuncCannotAssign(): Unit {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    func g(): Int64 {
        return f()
    }
    let x = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>g<!>
    let _ = x
    return
}

// FILE: transitiveCaptureLambdaCannotAssign.cj
// 形态：lambda 的函数体里调用了直接捕获 var 的局部函数；该 lambda 成为传递捕获闭包，再赋给 let 变量。
// kind：sema_func_capture_var_cannot_assign "lambda captured a mutable variable transitively, lambda cannot be assigned to a variable"，
//       锚 lambda 的首字符 `{`。1.0.5 / 1.1.3 一致，Range (6,13)..(6,14)。
func transitiveCaptureLambdaCannotAssign(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    let x = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>{=> f()}<!>
    return x()
}

// FILE: transitiveCaptureLambdaCannotAssignInLoopBlock.cj
// 形态：do-while 逻辑块（隐式 lambda）里给传递捕获的 lambda 赋值。
// kind：sema_func_capture_var_cannot_assign，锚块内 lambda 的首字符 `{`。1.0.5 / 1.1.3 一致，Range (11,17)..(11,18)。
func transitiveCaptureLambdaCannotAssignInLoopBlock(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    var i: Int64 = 0
    do {
        i = i + 1
        let x = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>{=> f()}<!>
        i = i + x()
    } while (i < 3)
    return i
}

// FILE: transitiveCaptureLambdaCannotReturn.cj
// 形态：lambda 的函数体里调用了直接捕获 var 的局部函数；`return {=> f()}` 走 ReturnExpr 分支。
// kind：sema_func_capture_var_cannot_return "lambda captured a mutable variable transitively, lambda cannot be used as a return value"，
//       锚 lambda 的首字符 `{`。1.0.5 / 1.1.3 一致，Range (6,12)..(6,13)。
func transitiveCaptureLambdaCannotReturn(): () -> Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    return <!FUNC_CAPTURE_VAR_CANNOT_RETURN!>{=> f()}<!>
}

// FILE: transitiveCaptureLambdaCannotParam.cj
// 形态：把"函数体里调用了捕获 var 的局部函数"的 lambda 直接作为实参传给函数类型形参 `() -> Int64`。
// kind：sema_func_capture_var_cannot_param "lambda captured a mutable variable transitively, lambda cannot be used as a param"，
//       锚 lambda 的首字符 `{`（`arg->expr` 就是该字面量）。1.0.5 / 1.1.3 一致，Range (13,33)..(13,34)。
func transitiveTakeLambda(i: () -> Int64): Int64 {
    return i()
}

func transitiveCaptureLambdaCannotParam(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    return transitiveTakeLambda(<!FUNC_CAPTURE_VAR_CANNOT_PARAM!>{=> f()}<!>)
}

// FILE: transitiveCaptureLambdaCannotParamAsMemberCallArg.cj
// 形态：同上，但调用是成员访问 `obj.take(...)`；官方对每个实参都走同一段循环，与 baseFunc 无关。
// kind：sema_func_capture_var_cannot_param，锚 lambda 的首字符 `{`。1.0.5 / 1.1.3 一致，Range (15,21)..(15,22)。
class TransitiveArgTaker {
    func take(i: () -> Int64): Int64 {
        return i()
    }
}

func transitiveCaptureLambdaCannotParamAsMemberCallArg(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    let obj = TransitiveArgTaker()
    return obj.take(<!FUNC_CAPTURE_VAR_CANNOT_PARAM!>{=> f()}<!>)
}

// FILE: ifBlockInnerLambdaCannotAssign.cj
// 形态：if 逻辑块（官方 AST 里是隐式 lambda）里给传递捕获的 lambda 赋值；块 lambda 本身 NO_CAPTURE，官方不报它。
// kind：sema_func_capture_var_cannot_assign，锚两个 lambda 各自的首字符 `{`。1.0.5 / 1.1.3 一致，Range (10,21)..(10,22) 与 (11,17)..(11,18)。
// 合法对照见 whileBlockInnerLambdaCannotAssign（同名诊断、外层是 while 块）。
func ifBlockInnerLambdaCannotAssign(b: Bool): Unit {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    if (b) {
        let inner = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>{=> f()}<!>
        let x = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>{=> f()}<!>
        inner()
        x()
    }
    return
}

// FILE: whileBlockInnerLambdaCannotAssign.cj
// 形态：同上，外层逻辑块是 while。
// kind：sema_func_capture_var_cannot_assign，锚 lambda 首字符 `{`。1.0.5 / 1.1.3 一致，Range (11,17)..(11,18)。
func whileBlockInnerLambdaCannotAssign(): Unit {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    var i: Int64 = 0
    while (i < 3) {
        i = i + 1
        let x = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>{=> f()}<!>
        x()
    }
    return
}

// FILE: transitiveCaptureLambdaCannotExpr.cj
// 形态：传递捕获的 lambda 作为裸表达式语句出现，既不是声明初始化器、也不是 return / 实参。
// kind：sema_func_capture_var_cannot_expr "lambda captured a mutable variable transitively, lambda cannot be used as a expression"，
//       锚 lambda 的首字符 `{`。1.0.5 / 1.1.3 一致，Range (6,5)..(6,6)。
func transitiveCaptureLambdaCannotExpr(): Unit {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    <!FUNC_CAPTURE_VAR_CANNOT_EXPR!>{=> f()}<!>
    return
}

// FILE: transitiveCaptureFuncRefInTypedLambdaBodyCannotExpr.cj
// 形态：带类型 lambda（`{a: Int64 => g}`）的函数体里，传递捕获的函数名 g 作为尾表达式（RefExpr，非调用基，解析期没有 `g()`）。
// kind：sema_func_capture_var_cannot_expr "g captured a mutable variable transitively, g cannot be used as a expression"，
//       锚 g 一个字符。1.0.5 / 1.1.3 一致，Range (13,18)..(13,19)。
// 官方对 g 的"赋值"入口报的是 _cannot_assign（见 transitiveCaptureFuncRefInLambdaBodyCannotAssign），两个入口的 kind 由节点类型决定。
func transitiveCaptureFuncRefInTypedLambdaBodyCannotExpr(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    func g(): Int64 {
        return f()
    }
    {a: Int64 => <!FUNC_CAPTURE_VAR_CANNOT_EXPR!>g<!>}(0)
    return 0
}

// FILE: transitiveCaptureFuncRefInLambdaBodyCannotAssign.cj
// 形态：无类型 lambda 的函数体里，把传递捕获的函数名 h 绑定到 let 变量（VarDecl 分支）。此时 h 走到调用遍历时尚未是捕获闭包，
//       官方先在绑定点按 NO_CAPTURE 放过；随后 h 的函数体被这个 lambda 调用而触发传递捕获标记，但遍历已经过了 lambda，
//       所以 h 最终是 TRANSITIVE，诊断报在绑定点。
// kind：sema_func_capture_var_cannot_assign "h captured a mutable variable transitively, h cannot be assigned to a variable"，
//       锚 h 一个字符。1.0.5 / 1.1.3 一致，Range (18,21)..(18,22)。
func transitiveCaptureFuncRefInLambdaBodyCannotAssign(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    func g(): Int64 {
        return f()
    }
    func h(): Int64 {
        return g()
    }
    let s = {=>
        let alias = <!FUNC_CAPTURE_VAR_CANNOT_ASSIGN!>h<!>
        alias()}()
    return s
}

// ── 3. lambda 形参省略类型 → sema_lambdaExpr_must_have_type_annotation（锚形参名标识符）
//    官方只在"没有任何类型信息可用来解出这个形参"时报（SynLamExpr 里 `ctx.funcArgReachable` 或已有可用的目标类型），
//    只要期望函数类型能从别处得到（如 `let f: (Int64) -> Int64 = {i => i}`、字段显式类型、实参形参类型）就合法。

// FILE: lambdaParamWithoutTypeAnnotation.cj
// 形态：`let f = {i => i}`——lambda 的形参没有类型注解，且没有任何期望类型可用。
// kind：sema_lambdaExpr_must_have_type_annotation "parameters of this lambda expression must have type annotations"，
//       锚形参节点（官方 DiagnoseRefactor(kind, *node)，node 是 FuncParam），实测恰好是形参名 `i` 一个标识符。
// 1.0.5 / 1.1.3 一致，Range (6,14)..(6,15)。
func lambdaParamWithoutTypeAnnotation(): Unit {
    let f = {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>i<!> => i}
    return
}

// FILE: multiLambdaParamsWithoutTypeAnnotation.cj
// 形态：多个形参都省略类型；官方只报第一个（`for (auto& node : params) { if (!node->type) { 报; return true } }`）。
// kind：sema_lambdaExpr_must_have_type_annotation，锚第一个形参 `i`。1.0.5 / 1.1.3 一致，Range (4,6)..(4,7)。
func multiLambdaParamsWithoutTypeAnnotation(): Unit {
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>i<!>, j => i + j}
    return
}

// FILE: lambdaParamWithoutTypeInsideCapturedLambda.cj
// 形态：嵌套——外层 lambda 形参有类型、其函数体里再写一个省略类型的内层 lambda。
// kind：sema_lambdaExpr_must_have_type_annotation，锚内层第一个形参 `j`。1.0.5 / 1.1.3 一致，Range (5,18)..(5,19)。
func lambdaParamWithoutTypeInsideCapturedLambda(): Unit {
    let f = {i: Int64 =>
        let g = {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>j<!> => j}
        g(i)}
    f(1)
    return
}

// FILE: lambdaParamWithoutTypeInLoopBlock.cj
// 形态：`while` 逻辑块（隐式 lambda）的函数体里出现省略类型的 lambda；与在普通函数体里写等价。
// kind：sema_lambdaExpr_must_have_type_annotation，锚形参 `i`。1.0.5 / 1.1.3 一致，Range (7,18)..(7,19)。
func lambdaParamWithoutTypeInLoopBlock(): Unit {
    var i: Int64 = 0
    while (i < 3) {
        i = i + 1
        let f = {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>i<!> => i}
        f(1)
    }
    return
}

// ── 4. 合法对照（两版零诊断）

// FILE: calledDirectlyCaptureVarLambdaOk.cj
// 合法对照 1：捕获 var 的 lambda 只被直接调用（字面量立即调用 `{=> v}()`，或结果赋给变量 `{=> v}()`），官方不报；
// 注意 `let f = {=> v}` 再 `f()` 是**不**合法的（见 lambdaAloneAsLetInitializer）。
func calledDirectlyCaptureVarLambdaOk(): Int64 {
    var v: Int64 = 1
    let direct = {=> v}()
    let seq = {=>
        let w = v + 1
        w}()
    return direct + seq
}

// FILE: capturedImmutableVarOk.cj
// 合法对照 2：lambda 只捕获 let 变量，或只捕获自己函数体里声明的 var（官方 `SetCaptureKind` 对 targetFB == curFuncBody 直接返回），
// captureKind 保持 NO_CAPTURE，作为值使用合法。本段的前置声明只能放在本段文件开头：
//   顶层变量只能被它之后声明的顶层初始化器使用，提前声明会报 sema_undefined_variable。
func capturedImmutableVarOk(): Unit {
    let l: Int64 = 1
    let a = {=> l}
    let b = {=> var own: Int64 = 2
        own}
    let _ = a
    let _ = b
    return
}

// FILE: capturedGlobalVarOk.cj
// 合法对照 2b：lambda 捕获顶层 `var`（标记 GLOBAL，官方 MarkAndCheckRefExprVarCaptureStatus 直接跳过）与 class 的
// static var，都不算闭包捕获，作为值使用合法（两版零诊断）。
var capturedGlobalVarOkTarget: Int64 = 0

class CapturedStaticVarOkHolder {
    static var capturedStaticVarOkTarget: Int64 = 0
}

func capturedGlobalVarOk(): Unit {
    let fromGlobal = {=> capturedGlobalVarOkTarget}
    let fromStatic = {=> CapturedStaticVarOkHolder.capturedStaticVarOkTarget}
    let _ = fromGlobal
    let _ = fromStatic
    return
}

// FILE: classMemberFieldLambdaCaptureOk.cj
// 合法对照 3：lambda 捕获 class / struct 的成员 var（`this.a` 或不带 `this` 的 `a`）不算捕获闭包（成员不在任何 FuncBody 的
// scope 里，也不是局部变量），作为值使用合法；class 字段初始化器里捕获成员反而是 sema_illegal_usage_of_member，
// struct 字段初始化器里写 `this.a` 是 sema_illegal_this_outside_struct_constructor，两者都不属本族。
class ClassMemberFieldLambdaCaptureOk {
    var a: Int64 = 1

    func withThis(): Unit {
        let f = {=> this.a}
        f()
    }

    func withoutThis(): Unit {
        let g = {=> a}
        g()
    }
}

struct StructMemberFieldLambdaCaptureOk {
    var a: Int64 = 1

    func withThis(): Unit {
        let f = {=> this.a}
        f()
    }
}

// FILE: transitiveCaptureCalledDirectlyOk.cj
// 合法对照 4：传递捕获只影响"作为值使用"，直接调用（`f()`、`{=> f()}()`）合法；被调用者自己声明的 var 不构成捕获；
// 传递捕获的闭包赋给变量后再调用时，官方只在赋值点报（见 transitiveCaptureLambdaCannotAssignInLoopBlock）。
func transitiveCaptureCalledDirectlyOk(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    func selfDeclared(): Int64 {
        var own: Int64 = 2
        return own
    }
    let direct = f()
    let nested = {=> f()}()
    let selfCall = selfDeclared()
    return direct + nested + selfCall
}

// FILE: lambdaParamWithExpectedTypeOk.cj
// 合法对照 5：省略类型的 lambda 形参只要期望类型可从上下文得到就合法（变量声明的显式类型 / 形参的函数类型），
// 官方 SynLamExpr 先走 `ctx.lastTargetTypeMap` 推导，不报 must_have_type_annotation。
func lambdaParamWithExpectedTypeApply(f: (Int64) -> Int64, v: Int64): Int64 {
    return f(v)
}

struct LambdaParamWithExpectedTypeStruct {
    var a: Int64 = 1
    var f: (Int64) -> Int64 = {i => i}
}

func lambdaParamWithExpectedTypeOk(): Int64 {
    let local: (Int64) -> Int64 = {i => i}
    let applied = lambdaParamWithExpectedTypeApply({i => i}, 1)
    let field = LambdaParamWithExpectedTypeStruct()
    return local(2) + applied + field.f(3)
}

// 已删除的形态（探到但在本机 1.0.5 / 1.1.3 上不可达、或被前置诊断抢先的形态；官方调用点与抢先的 kind/Range 见下。
//   探针源文件在 .scratch/lambda/probeA.cj ~ probeJ.cj 与 cjcprobe.py / cjcprobe_helpers.py）：
//   1. 函数引用 + 变量声明初始化器：`var v = 1; func f(): Int64 { return v }; let x = f`（或 `var x = f`）
//      → TypeChecker.cpp:1790 CheckLegalUseOfClosure(*vd->initializer, sema_func_capture_var_cannot_assign)
//        进入 Expr 版（:1763-1784）的 RefExpr 分支后先判 CAPTURE_VAR → DiagUseClosureCaptureVarAlone（Diags.cpp:573），
//        报 sema_use_func_capture_var_alone "function capturing mutable variables needs to be called directly"，
//        锚整个函数名表达式（探针 aNestedFuncRefAlone / dNestedFuncVarAlone，两版 Range 均为 `f` 一个字符）。
//        sema_func_capture_var_cannot_assign 只在 TRANSITIVE_CAPTURE 时才会被选中（见 transitiveCaptureFuncCannotAssign）。
//   2. 函数引用 + return：`return f`（f 直接捕获 var）→ TypeChecker.cpp:1794 的 _cannot_return 分支同样被 Expr 版的
//        RefExpr 分支抢先，报 sema_use_func_capture_var_alone @ `f`；sema_func_capture_var_cannot_return 不可达
//        （llt/function/function_capture2.cj 的 `return g` 也是 use_func_capture_var_alone）。
//   3. 函数引用 + 实参：`apply(f)`（f 直接捕获 var，且实参形参类型匹配）→ TypeChecker.cpp:1802 的 _cannot_param 分支
//        被抢先，报 sema_use_func_capture_var_alone @ `f`；sema_func_capture_var_cannot_param 不可达。
//   4. 调用位置上的函数名不报：`while (h())`、`-f()`、`for (i in 0..f())` 里的 `f` 是被调用的基，解析期 ParseExpr.cpp:924
//        已打 isBaseFunc，`CheckLegalUseOfClosure(Node&)` 的 `!ref->isBaseFunc` / `!lambda->isBaseFunc` 条件不成立（探针
//        dTransitiveExprInWhile、dTransitiveExprUnaryMinus、hTransitiveExprInForExpr，两版零诊断）。
//        把函数值本身（而不是调用结果）当条件 `while (h)` 会报 _cannot_expr，但同时额外报 sema_mismatched_types（`while`
//        条件要求 Bool），两条诊断锚点都落在 `h` 上、互相重叠，无法区分，故本文件不用该形态。
//        裸语句 `g`（传递捕获函数）只在 1.0.5 报 _cannot_expr、1.1.3 不报（探针 bTransitiveExprRefAlone：1.0.5 报 @ `g`，
//        1.1.3 零诊断），属版本差异，不取形态。
//   5. 实参位置上的无类型 lambda：`i({i => i})`（i 的形参类型是 `(Int64) -> Int64`）→ TypeChecker.cpp:1802 会调用
//        CheckLegalUseOfClosure，但 lambda 没有任何捕获（形参类型由目标类型推出）⇒ CaptureKind::NO_CAPTURE ⇒
//        两个分支都不进，零诊断（两版实测，探针段 iParamLambdaTypedParam / gLambdaNoTypeAsCallArg）。
//   6. 返回类型已知位置的 lambda：`func f(): (Int64) -> Int64 { return {i => i} }` → TypeChecker.cpp:1794 同理零诊断（两版实测）。
//   7. 直接捕获 var 的 lambda 作为实参：`i({=> v})` → use_func_capture_var_alone @ 整个 lambda（探针 dInnerLambdaInCallArg：
//        两版 Range 均为 `{=> v}` 整段）。
//   8. `spawn { => flag = true }`（llt/lambda/err_capture_var.cj 形态）→ 官方 CheckClosures 只遍历 AST，`spawn` 的块不是
//        LambdaExpr，两版实测零诊断（`spawn` 的诊断由 CfirSpawnSemanticsChecker.kt:173 处理，属另一族）。
//   9. 外层 lambda 直接引用 var、内层 lambda 只调用捕获 var 的函数（`{=> let g = {=> f()}; g()}`）：内层 lambda 报
//        _cannot_assign（TRANSITIVE），外层不报（外层只调用闭包 ⇒ NO_CAPTURE；探针 dOuterCapturesInnerDirect）。
//   10. sema_func_capture_var_not_ctype（TypeChecker.cpp:1807-1814，锚捕获变量的引用）与 sema_cfunc_cannot_capture_var
//        （:1797-1799 / :1813，锚变量名）要求 `@C` struct 变量或 CFunc lambda，属 FFI 族，已由
//        llt/ffi/semantic_contract/cstructFieldsNegative.cj 与 cfuncSignaturesNegative.cj 覆盖，本文件不重复。
//   11. 整包编译不可用作取证：DiagnosticEngine.cpp:764-770 的 CanBeEmitted 只发第一个错误类别，
//        HandleDiagnose 的 HasPrevDiag(range.begin, message) 会丢掉与已发诊断同起点同文案的重复项；一个段有 parse 错误时，
//        其余段的语义诊断全被吞掉。
