// RUN_PIPELINE_TILL: FRONTEND
// 语义族：常量模式（const pattern）里的字符串插值 —— 官方 sema_interpolation_in_const_pattern。
// 取证方式：官方 cjc 1.0.5（准）与 1.1.3 两版实测，静态库模式
//   `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；
//   每个 `// FILE:` 段单独编译（官方整包编译时一处 parse 错误会吞掉其余段的语义诊断，
//   且 HasPrevDiag 只吞同起点同文案的重复诊断，逐段编译才不会互相屏蔽）。
//   1.1.3 本机在 CHIR/codegen 阶段对本文件涉及的枚举 / 元组 / 多行字符串形态可能 LLVM ERROR 崩溃
//   （崩在语义检查之后），语义诊断 JSON 仍完整产出，本文件按语义诊断写期望，两版逐段比对一致。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），
//   本文件标记逐字符对齐官方范围（含跨行标记）。
// 官方实现（`external/cangjie_compiler`，工作树 checkout 在 tag v1.0.0）：
//   - src/Sema/TypeCheckPattern.cpp:243-279 ChkConstPattern，四步顺序固定，**前一步失败即 return**：
//     :247-249 Rune 选择器 + 单码点字符串字面量 → 直接取 target 类型；
//     :250-253 UInt8 选择器 + 单字节字符串字面量 → 取值并做 ChkLitConstExprRange；
//     :254-257 其余形态 Check(ctx, &target, literal) 失败 → 直接 return；
//     :258-264 字面量类型与选择器类型必须 IsTyEqual（不自动装箱）→ DiagMismatchedTypes，锚整个 literal，返回；
//     :265-270 `DynamicCast<LitConstExpr*>(literal) && siExpr` 非空 → sema_interpolation_in_const_pattern，
//       `diag.DiagnoseRefactor(kind, p)`，p 是**整个 ConstPattern 节点**；
//     :271-278 之后才是 == 运算符重载检查（ChkOpOverloadForConstPattern → sema_not_overload_in_match）。
//   - 锚点范围：include/cangjie/Basic/DiagnosticEngine.h:908-917 `DiagnoseRefactor(kind, const AST::Node&)`
//     → `MakeRange(node.GetBegin(), node.GetEnd())`；而 src/Parse/ParsePattern.cpp:100-107 ParseConstPattern
//     把 `ConstPattern.begin = lookahead.Begin()`、`ConstPattern.end = literal->end`，
//     GetLitConstExprFromStr（src/Parse/ParseAtom.cpp:167-190）把字面量的 begin/end 取成整个字符串 token，
//     故官方锚点 = **字符串字面量文本本身（首尾引号在内）**，不含 `case` 关键字、不含 `=>`。两版实测确认。
//   - 插值判定：src/Parse/ParseAtom.cpp:250-284 ProcessStringInterpolation 在字符串 token 含插值时给 LitConstExpr
//     挂上 siExpr；src/Lex/Lexer.cpp:981-1020 ScanStringOrJString 与 :1164-1215 ScanMultiLineString
//     只在 `${` 处切分 stringParts（`currentChar == '$' && GetNextChar(0) == '{'`），
//     **不含 `$ident` 形态**：`$` + 标识符在字符串里被 IsLegalEscape 放行成普通字符（Lexer.cpp:939），
//     所以 `"a$y"` / `"a\${y}"` / `"a${y}"` 都是普通字符串字面量、不是插值（两版实测：`case "a$y"` 与 `case "a\${y}"` 零诊断，`case "a${y}"` 报插值 error）。
//   - 文案：include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:81
//     `ERROR(sema_interpolation_in_const_pattern, "cannot use string interpolation in constant pattern")`；
//     同文件 :104 `ERROR(sema_pattern_literal_expected, "only const literal is allowed in const pattern")`
//     是**死条目**——src 与 include 全仓无任何引用点，常量模式的字面量限制由 parser 层
//     （ParsePattern.cpp:20-29 的 token 集合）保证，语义层无法触发，本文件不写该形态。
//   - 入口：src/Parse/ParsePattern.cpp:20-29 ParsePattern 见 STRING_LITERAL / MULTILINE_STRING /
//     MULTILINE_RAW_STRING / RUNE_LITERAL / INT / FLOAT / `-` / `(` 时走 ParseConstPattern；
//     `case` 后的单个常量模式不是元组模式，因此**一个 case 永远只报一条**插值诊断。
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/.../diagnostics/CfirDiagnosticsList.kt:1391）：
//   `INTERPOLATION_IN_CONST_PATTERN by error<PsiElement>()`，定位策略为 SourceElementPositioningStrategies.DEFAULT
//   （OffsetsOnly，整节点 source 的 offset 区间）；默认文案 CfirErrorsDefaultMessages.kt:945 与官方一致。
//   **生产者被注释掉、无注册**：cfir/checkers/src/.../checkers/expression/CfirPatternExpressionChecker.kt:352-385
//   整个 `CfirConstPatternInterpolationChecker` 对象（:374 是 `factory = CfirErrors.INTERPOLATION_IN_CONST_PATTERN` 一行）
//   处于注释状态，因此本文件各段预期红，属 CFIR 缺口；按官方写期望，不因 CFIR 输出调整输入或期望。
//   按源码可预判的锚点差异（只写源码能直接看出的，未跑 CFIR）：
//     官方锚点 = ConstPattern 节点（= 字面量文本）；被注释的 CFIR 报告点用 `pattern.source`，
//     PSI 侧 CfirConstPattern 的 source 取 `CjConstantPattern` 节点（PsiRawCfirBuilder.kt:3673）、
//     LightTree 侧取 CONSTANT_PATTERN 节点（LightTreeRawCfirExpressionBuilder.kt:1711），
//     按节点定义应与官方同为字面量文本，**不是**含 `case` 的整条分支。
//   另一处模型差异（源码可读出）：官方 `siExpr` 挂在 `LitConstExpr` 上，判定条件是"字面量是字符串 token 且 token 含插值"；
//   CFIR 侧 psi2cfir / light-tree2cfir 的 convertStringTemplate（PsiRawCfirBuilder.kt:2258-2284、
//   LightTreeRawCfirExpressionBuilder.kt:268-299）在**有插值时构造 CfirStringInterpolation**（不是 CfirLiteralExpression），
//   被注释的检查器却写成 `expr is CfirLiteralExpression && kind == CfirLiteralKind.StringInterpolation`，
//   两个条件都与实际模型对不上；CFIR 若按该形态接线还须确认常量模式下走的是 convertStringTemplate 而非常量字面量分支。
//   已有覆盖不重复：llt/PatternMatching/ConstPattern/err_string_interpolation_00.cj（`case "${x.size}"`，无内联标记、
//   只在 LLT 覆盖，不计入本矩阵）；const/constFunctionRules.cj 头部 :52 已注明"未纳入"本族；
//   pattern/patternLegality.cj 覆盖 TYPE_MISMATCH / CANNOT_CONVERT_LITERAL / ENUM_PATTERN_PARAM_SIZE_ERROR；
//   pattern/patternDeclarationRules.cj 覆盖 let / for-in / irrefutable 侧规则；match/notOverloadInMatch.cj 覆盖第 4 步；
//   条件编译 when 条件的"字符串字面量不得插值"在 conditional-compilation/whenConditionErrors.cj（不同规则，不重复）。
// 各段 Range 均按**逐段单独编译**的干净文本行号（`// FILE:` 行被剥离），故正文里的形如 (3,14) 是段内行号。

// ── 形态 1：`${x.size}` 成员访问插值（官方唯一官方样例 llT/PatternMatching/ConstPattern/err_string_interpolation_00.cj 的形态）
// kind：sema_interpolation_in_const_pattern "cannot use string interpolation in constant pattern"
// 锚点：整个字面量 `"${x.size}"`，11 个字符（`case ` 之后到 `=>` 之前）；1.0.5 / 1.1.3 一致 Range (3,14)..(3,25)。
// FILE: constPatInterpolationMemberAccess.cj
func constPatInterpMemberAccess(x: String): Int64 {
    return match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"${x.size}"<!> => 0
        case _ => 1
    }
}

// ── 形态 2：`${}` 插值（插值体是普通标识符，锚点最直观的形态）
// 锚点：整个字面量 `"a${y}b"`，8 个字符；1.0.5 / 1.1.3 一致 Range (3,14)..(3,22)。
// FILE: constPatInterpolationBrace.cj
func constPatInterpBrace(x: String, y: Int64): Int64 {
    return match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"a${y}b"<!> => 0
        case _ => 1
    }
}

// ── 形态 3：多段插值（三个插值段 + 两段普通文本，一次报一条）
// 锚点：整个字面量 `"${y}-${z}-${y}"`，16 个字符；1.0.5 / 1.1.3 一致 Range (3,14)..(3,30)。
// FILE: constPatInterpolationMultiSegment.cj
func constPatInterpMultiSegment(x: String, y: Int64, z: Int64): Int64 {
    return match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"${y}-${z}-${y}"<!> => 0
        case _ => 1
    }
}

// ── 形态 4：嵌套插值（内层字符串里再插值；ProcessQuotaMarks 按括号计数找到配对 `}`，仍是一个 siExpr）
// 锚点：整个字面量 `"a${"b${y}c"}d"`，15 个字符；1.0.5 / 1.1.3 一致 Range (3,14)..(3,29)。
// FILE: constPatInterpolationNested.cj
func constPatInterpNested(x: String, y: String): Int64 {
    return match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"a${"b${y}c"}d"<!> => 0
        case _ => 1
    }
}

// ── 形态 5：元组模式里的常量模式分量（ChkPattern 递归，锚点只含该分量的字面量，不含括号与逗号）
// 锚点：`"a${y}"`，7 个字符（行首 `        case (` 之后）；1.0.5 / 1.1.3 一致 Range (3,15)..(3,22)。
// FILE: constPatInterpolationInTuple.cj
func constPatInterpInTuple(x: (String, Int64), y: Int64): Int64 {
    return match (x) {
        case (<!INTERPOLATION_IN_CONST_PATTERN!>"a${y}"<!>, 1) => 0
        case _ => 1
    }
}

// ── 形态 6：or 模式的一个分支是带插值的常量模式（只报带插值那一支，锚点不含 `|`）
// 锚点：`"a${y}"`，7 个字符；1.0.5 / 1.1.3 一致 Range (3,14)..(3,21)。
// FILE: constPatInterpolationInOrPattern.cj
func constPatInterpInOrPattern(x: String, y: Int64): Int64 {
    return match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"a${y}"<!> | "b" => 0
        case _ => 1
    }
}

// ── 形态 7：多行字符串常量模式里的插值（官方多行字符串必须以换行开头，锚点跨 2 行，从 `"""` 到收尾 `"""`）
// 锚点：`"""\n            a${y}b"""`，跨行 Range (3,14)..(4,22)（结束列开区间）。
// 1.0.5 / 1.1.3 一致。CFIR 侧此形态还叠加缺口：多行常量模式与多行字符串插值的 PSI/LightTree 建模未取证。
// FILE: constPatInterpolationMultiline.cj
func constPatInterpMultiline(x: String, y: Int64): Int64 {
    return match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"""
            a${y}b"""<!> => 0
        case _ => 1
    }
}

// ── 形态 8：同一个 match 里两个分支都是带插值的常量模式（官方逐条报，Range 不因前一条失败而变化）
// 两条诊断分别锚各自字面量；1.0.5 / 1.1.3 一致 Range (3,14)..(3,21) 与 (4,14)..(4,21)。
// FILE: constPatInterpolationTwoBranches.cj
func constPatInterpTwoBranches(x: String, y: Int64, z: Int64): Int64 {
    let r = match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"a${y}"<!> => 0
        case <!INTERPOLATION_IN_CONST_PATTERN!>"b${z}"<!> => 1
        case _ => 2
    }
    return r
}

// ── 形态 9：语句位置的 match（match 作为语句而非 return 的值）诊断相同
// 锚点：`"a${y}"`，7 个字符；1.0.5 / 1.1.3 一致 Range (3,14)..(3,21)。
// FILE: constPatInterpolationStatementPosition.cj
func constPatInterpStatementPosition(x: String, y: Int64): Int64 {
    match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"a${y}"<!> => 0
        case _ => 1
    }
    return 0
}

/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_pattern_literal_expected）
SUGGESTED_MESSAGE: only const literal is allowed in const pattern
SOURCE: include/cangjie/Basic/DiagnosticSema.def:104（v1.0.0 死条目：src/ 与 include/ 全仓无引用点；
        常量模式的"只允许常量字面量"由 parser 层 src/Parse/ParsePattern.cpp:20-29 的 token 集合保证，
        语义层 ChkConstPattern 对 literal 的每种非字符串形态都接受）
*/

// 合法对照段（error 为零即合法；各段的 warning 情况见各段注释；除注明外两版 error 与 warning 均为空）：
// L1：普通字符串常量模式（无插值）+ 通配模式；变量模式单列（L3），这里只有常量与通配两种，
//     `case _` 不触发 UNREACHABLE_PATTERN。官方对已穷尽的 match 不发 sema_nonexhuastive_patterns。
// FILE: legalConstPatternPlainString.cj
func constPatPlainString(x: String): Int64 {
    return match (x) {
        case "abc" => 0
        case _ => 1
    }
}

// L2：`$ident` 形态在字符串里不是插值（Lexer.cpp:939 IsLegalEscape 放行 `$`）。`y` 在同一作用域有 let 绑定，
//     所以官方把字面量里的 `y` 当普通文本，**两版零 error**（探针 p03 / p11），插值诊断缺席即本段结论；
//     唯一的 warning 是两版都有的 chir_dce_unused_variable（锚 `let y` 的 y，Range (2,9)..(2,10)），
//     说明同作用域的同名 let 绑定本身不触发未声明标识符诊断。对照：同一作用域、同样写 `"a${y}"` 时
//     官方报插值 error（见形态 2，探针 p02），说明 `$ident` 与 `${...}` 在官方词法里确实不同。
// FILE: legalConstPatternDollarIdent.cj
func constPatDollarIdent(x: String): Int64 {
    let y = "k"
    return match (x) {
        case "a$y" => 0
        case _ => 1
    }
}

// L2b：`\$` 转义形态单独一段（`\$` 之后的 `{}` 与标识符都变成普通文本，两版零 error；
//     唯一的 warning 同 L2，是两版都有的 chir_dce_unused_variable，锚 `let y` 的 y，Range (2,9)..(2,10)）。
//     同作用域有 let `y` 以排除未声明标识符噪声。探针 p11 / p24。
// FILE: legalConstPatternEscapedDollar.cj
func constPatEscapedDollar(x: String): Int64 {
    let y = "k"
    return match (x) {
        case "a\${y}" => 0
        case _ => 1
    }
}
// L3：变量模式 + 兜底通配。`case y => 1` 之后再 `case _ => 2`，官方两版都只给 warning、无 error
//     （sema_unreachable_pattern 锚 `_`，Range (5,14)..(5,15)；形参 y 未使用，
//     chir_dce_unused_variable 锚 1:40，两版一致）。与 L1 对照：变量模式不是常量模式，不受插值规则约束。
// FILE: legalConstPatternWithVarPattern.cj
func constPatWithVarPattern(x: String, y: String): Int64 {
    return match (x) {
        case "abc" => 0
        case y => 1
        case _ => 2
    }
}

// L4：插值出现在 match **分支体**里合法（与模式位置相反）：通配分支体里的 `"v${x}${y}".size`
//     两版零 error、零 warning（探针 c5）。注意 `.size` 在仓颉里是 **prop 而非方法**：
//     `s.size()` 官方报 sema_no_match_operator_function_call "no matching function for operator '()'
//     function call"（锚接收者表达式 1 字符，两版一致，探针 c7 / c8），与常量模式 / 插值无关，
//     故本段取 prop 形态；`.length` 官方报 sema_invalid_access_control（探针 L4f），也不属本族。
// FILE: legalInterpolationInBranchBody.cj
func constPatInterpInBody(x: String, y: Int64): Int64 {
    return match (x) {
        case "abc" => 0
        case _ => "v${x}${y}".size
    }
}

// L4b：常量模式分支体里也放插值（走官方为常量模式合成的 `==` 检查路径）：两版零 error、零 warning
//      （探针 c4）。与 L4 一起说明插值规则只约束**模式位置**，不约束分支体。
// FILE: legalInterpolationInConstBranchBody.cj
func constPatInterpInConstBranch(x: String, y: Int64): String {
    return match (x) {
        case "abc" => "k${y}"
        case _ => "v"
    }
}

// L5：多行字符串常量模式（无插值），与形态 7 对照；官方多行字符串必须以换行开头。
//     1.1.3 本机在该单元的 codegen 阶段 LLVM ERROR（崩在语义检查后），语义诊断两版均为空。
// FILE: legalConstPatternMultilinePlain.cj
func constPatMultilinePlain(x: String): Int64 {
    return match (x) {
        case """
            abc""" => 0
        case _ => 1
    }
}

// L6：枚举构造器模式（不是常量模式，不受插值规则约束）+ Rune / Bool 常量模式，官方全部零诊断。
// FILE: legalConstPatternOtherLiteralKinds.cj
enum ConstPatLegalEnum {
    A | B
}

func constPatOtherLiteralKinds(x: Int64, r: Rune, b: Bool, e: ConstPatLegalEnum): Int64 {
    return match (x) {
        case 1 => 0
        case _ => match (r) {
            case 'a' => 1
            case _ => match (b) {
                case true => 2
                case _ => match (e) {
                    case ConstPatLegalEnum.A => 3
                    case _ => 4
                }
            }
        }
    }
}

// 已删除的形态（取证 kind 与 Range 见下；1.0.5 / 1.1.3 两版一致，除注明外）：
//   1. `case "$y" =>`（`$ident` 形态）→ 两版**零语义诊断**：官方词法只把字符串里的 `${` 当插值
//      （Lexer.cpp:1006、:1201 均判 `'$' && GetNextChar(0) == '{'`），`$` + 标识符被 IsLegalEscape
//      （Lexer.cpp:939）放行为普通字符，`$ident` 只是 DOLLAR_IDENTIFIER token（Lexer.cpp:1614-1629），
//      在字符串字面量里不成立（`$ident` 只是 DOLLAR_IDENTIFIER token，见上）；官方不解析字面量里的 `y`，
//      同作用域有 let `y` 时两版零诊断、零 warning（合法对照 L2，探针 p03 / p11），因此本文件不把 `$ident` 写成插值形态。
//      `\$` 转义同理不是插值（见 L2b，探针 p11 / p24）。
//   2. `case "${y}" + "c" =>` → parse_expected_double_arrow_in_case "expected '=>' in case, found '+'"
//      （锚 `+`，Range (4,22)..(4,23)）。常量模式只能是一个字面量，表达式形态在 parser 层被拒。属 parser 族。探针 p07。
//   3. `case ("${y}") =>` → parse_tuple_pattern_expected_more_field "1-element tuple pattern is not allowed"
//      （锚整个 `("${y}")`，Range (4,14)..(4,22)）。元组模式至少两个分量。属 parser 族。探针 p17。
//   4. `case """a${y}b""" =>`（同行写多行字符串）→ lex_multiline_string_start_from_newline
//      "multi-line string must start with newline character"（锚 `a`，Range (4,17)..(4,18)）。
//      官方要求多行字符串 `"""` 后必须换行（Lexer.cpp:1175-1186）；`'''a${y}b'''` 同样如此。属 lexer 族。探针 p09 / p23。
//   5. 选择器类型不是 String（`x: Int64` / `x: Any`）时 `case "a${y}"` → 只有 sema_mismatched_types
//      "mismatched types"（锚整个字面量，Range (4,14)..(4,21)），**没有** sema_interpolation_in_const_pattern：
//      ChkConstPattern 第 2 步类型不相等即 return，第 3 步的插值检查不可达。本文件的诊断面因此限定在
//      String 选择器；锚点形态与 patternLegality.cj 的 CANNOT_CONVERT_LITERAL 已有覆盖，不重复。探针 p20 / p26。
//   6. `case "a${y}" =>` 里插值体引用未声明标识符（`${y.size}`，y: Int64）→ sema_undeclared_identifier
//      "undeclared identifier 'size'"（锚 `size`，Range (5,20)..(5,24)），插值诊断因类型检查先失败而不报；
//      属 UNDECLARED_IDENTIFIER 族，与 patternLegality.cj 的形态 1 同一形态，仅插值体成员访问不同。探针 p21。
//   3b. `case "abc" => 0` 后跟 `case y => 1`（变量模式排在常量模式之后）→ 两版**不报**
//      sema_unreachable_pattern；只有末尾的 `case _` 兜底才报（warning，锚 `_`），见合法对照 L3。探针 p03。
//   7. `case let a => a` → parse_expected_pattern "expected pattern, found keyword 'let'"（锚 `let` 3 个字符）。
//      `let` 不能写在 case 后面（let 只用于变量声明模式，官方 ParsePattern 不接受 `let`）。属 parser 族。探针 p12。
//   8. `case A =>`（枚举构造器裸名）两版零诊断：ParseIrrefutablePattern 直接产出 VarPattern，不查构造器；
//      与 `case ConstPatEnum.A` 一样合法。合法对照 L6 已含限定名形态，不另立形态。探针 p13 / p14。
//   9. 字符串插值短形态 `${y}` / `$y` 作为独立表达式（`let a = ${y}` / `let a = $y`）→ 两版都只有
//      parse_expected_expression "expected expression after '=', found '$'" / "found ''"（锚 `$` 1–2 个字符）。
//      短插值形态在本仓 SDK 的 cjc 上不是可用的表达式语法（`$y` 词法是 DOLLAR_IDENTIFIER，parser 在赋值号后
//      见到它报 parse 错误），因此"常量模式里的 `$ident` 插值"在官方不可达。属 parser 族。探针 p27 / p28 / p29 / p30。