// RUN_PIPELINE_TILL: FRONTEND
// 主题：match 的 case 体（`case ... =>` 之后）的多种形态。
// 形态清单：
//   1.  单表达式体：`case _ => 0`
//   2.  分号分隔的表达式序列（同一行内多节点）：`case 1 => let a = 2; a + 1`
//   3.  换行分隔的多语句体（语句逐行书写、无分号）：`=>` 之后换行再写若干语句
//   4.  体内声明：`let` / `var` / `const` 声明、赋值与使用
//   5.  体内局部函数：`func` 声明并在同一体内调用
//   6.  体内嵌套 `match` 表达式
//   7.  `where` 守卫（pattern guard）配多语句体
//   8.  绑定模式 + 体内使用：类型模式 `case v: T`、tuple 模式、enum 构造器模式
//   9.  体为 `return`
//  10.  体内 `if` / `for` / `while` 表达式
//  11.  体内 lambda 字面量并立即调用
//  12.  match 作为语句（各分支为 Unit）+ 多语句体
//  13.  无匹配值 match 的多语句体
//  14.  整个 match 压在一行；多个 case 同行以 `;` 分隔
// 官方依据：
//   src/Parse/ParseAtom.cpp:397 ParserImpl::ParseExprOrDeclsInMatchCase —— case 体是「多节点块」，
//     循环解析直到 `case` 或 `}`，每轮由 SeeingMacroCallDecl / SeeingDecl / SeeingExpr 分派（:412）；
//     :405 DiagExpectSemiOrNewline —— 同一行的相邻节点之间必须由分号分隔，换行同样可以。
//   src/Parse/Parser.cpp:452 ParserImpl::ParseExprOrDecl —— :468 SeeingDecl() 时转 ParseDecl，
//     所以 case 体内可以写 `func` 局部函数与变量声明。
//   src/Parse/ParserImpl.h:447 SeeingDecl —— case 体按 ScopeKind::FUNC_BODY 分派，可识别 FUNC / LET / VAR 等声明起始。
//   src/Parse/ParseAtom.cpp:539 ParserImpl::ParseMatchCases —— :550 解析 `where` 守卫（pattern guard），
//     :570 取 case 体，:573 体为空时报 parse_case_body_cannot_be_empty。
//   src/Parse/ParseAtom.cpp:581 ParserImpl::ParseMatchNoSelector —— 无匹配值 match 的 case 体走同一入口（:613）。
//   文档《match 表达式》「match 表达式的定义」：`=>` 之后「可以是一系列表达式、变量和函数定义
//   （新定义的变量或函数的作用域从其定义处开始到下一个 case 之前结束）」。
// cjc 1.0.5 / 1.1.3 零诊断。

enum MatchBodyKind {
    | Round(Int64)
    | Flat
}

// 形态 1 / 14：单表达式体；整个 match 压在一行、多个 case 同行以 `;` 分隔。
func matchBodySingleExpr(n: Int64): Int64 {
    return match (n) {
        case 1 => 0
        case _ => 0
    }
}

func matchBodyOneLine(n: Int64): Int64 {
    return match (n) { case 1 => 1; case _ => 0 }
}

// 形态 2：同一行内以分号分隔的表达式序列。
func matchBodySemiSequence(n: Int64): Int64 {
    return match (n) {
        case 1 => let a = 2; a + 1
        case _ => 0
    }
}

// 形态 3：`=>` 之后换行，逐行书写多条语句、无分号。
func matchBodyMultiLine(n: Int64): Int64 {
    return match (n) {
        case 1 =>
            let a = 2
            let b = a + 1
            b
        case _ => 0
    }
}

// 形态 4：体内 `let` / `var` / `const` 声明、赋值与使用。
func matchBodyDecls(n: Int64): Int64 {
    return match (n) {
        case 1 =>
            let a = 2
            var b = 3
            const c = 4
            b = b + a
            b + c
        case _ => 0
    }
}

// 形态 5：体内局部函数 `func` 声明并在同一体内调用。
func matchBodyLocalFunc(n: Int64): Int64 {
    return match (n) {
        case 1 =>
            func matchBodyInner(x: Int64): Int64 {
                return x * 2
            }
            matchBodyInner(3)
        case _ => 0
    }
}

// 形态 6：体内嵌套 match 表达式。
func matchBodyNestedMatch(n: Int64, s: String): Int64 {
    return match (n) {
        case 1 => match (s) {
            case "a" => 1
            case _ => 2
        }
        case _ => 0
    }
}

// 形态 7：`where` 守卫配多语句体。
func matchBodyGuarded(x: Any): Int64 {
    return match (x) {
        case v: Int64 where v > 0 =>
            let doubled = v + v
            doubled
        case _ => 0
    }
}

// 形态 8：绑定模式在体内可见并可使用（类型模式 / tuple 模式 / enum 构造器模式）。
func matchBodyTypeBinding(x: Any): Int64 {
    return match (x) {
        case v: Int64 => v + 1
        case _ => 0
    }
}

func matchBodyTupleBinding(t: (Int64, Int64)): Int64 {
    return match (t) {
        case (a, b) => a + b
    }
}

func matchBodyEnumBinding(k: MatchBodyKind): Int64 {
    return match (k) {
        case Round(r) => r
        case Flat => 0
    }
}

// 形态 9：体为 `return`。
func matchBodyReturn(n: Int64): Int64 {
    return match (n) {
        case 1 => return 10
        case _ => return 0
    }
}

// 形态 10：体内 `if` / `for` / `while` 表达式。
func matchBodyControlFlow(n: Int64): Int64 {
    return match (n) {
        case 1 =>
            if (n > 0) {
                1
            } else {
                2
            }
        case 2 =>
            var sum = 0
            for (i in 0..3) {
                sum = sum + i
            }
            sum
        case 3 =>
            var acc = 0
            var k = 0
            while (k < 3) {
                acc = acc + k
                k = k + 1
            }
            acc
        case _ => 0
    }
}

// 形态 11：体内 lambda 字面量并立即调用。
func matchBodyLambda(n: Int64): Int64 {
    return match (n) {
        case 1 => { f: Int64 => f + 1 }(2)
        case _ => 0
    }
}

// 形态 12：match 作为语句（各分支为 Unit）+ 多语句体。
func matchBodyAsStatement(n: Int64): Unit {
    match (n) {
        case 1 =>
            let a = 1;
            println(a)
        case _ => println("other")
    }
}

// 形态 13：无匹配值 match 的多语句体。
func matchBodyNoSelector(n: Int64): Int64 {
    return match {
        case n > 10 =>
            let a = 1
            a + 1
        case _ => 0
    }
}
