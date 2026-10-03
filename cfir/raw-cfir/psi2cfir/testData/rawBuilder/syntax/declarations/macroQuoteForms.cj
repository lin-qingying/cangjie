// RUN_PIPELINE_TILL: FRONTEND
// 宏体的 quote 形态：字面量（Int / Float / Str）、标识符（ident / foo）、复合表达式（a + b、括号、数组、逗号）、空 quote、声明（class / func）、
// token 构造（Ident / Int64 / Semi / Newline / Paren / Str / Square / TypeNode / Dollar / At）、parseIdent 辅助、quote($(x)) 插值。
// 附注：1.1.3 起 `$(x)` 展开语义改为调用 `transformTokens`（本文件不写 `$(x)`，见 recovery/quoteDollarInterpolation.cj）。
// 官方依据：官方 Parse/ParseQuote.cpp（quote 内是 token 序列，`$(expr)` / `$ident` 是插值）、Parse/ParseAtom.cpp ParseQuoteExpr；cjc 1.0.5 零诊断，
// 1.1.3 在 codegen 阶段因本机 SDK 的 opt.exe 路径问题崩溃（见 README「环境限制」）。
macro package mp
import std.ast.*

public macro intLit(input: Tokens): Tokens {
    return quote(1)
}

public macro floatLit(input: Tokens): Tokens {
    return quote(1.5)
}

public macro strLit(input: Tokens): Tokens {
    return quote("s")
}

public macro identKw(input: Tokens): Tokens {
    return quote(ident)
}

public macro identName(input: Tokens): Tokens {
    return quote(foo)
}

public macro binaryExpr(input: Tokens): Tokens {
    return quote(a + b)
}

public macro parenExpr(input: Tokens): Tokens {
    return quote((a))
}

public macro arrayExpr(input: Tokens): Tokens {
    return quote([a])
}

public macro commaExpr(input: Tokens): Tokens {
    return quote(a, b)
}

public macro emptyQuote(input: Tokens): Tokens {
    return quote()
}

public macro classDecl(input: Tokens): Tokens {
    return quote(class C {})
}

public macro funcDecl(input: Tokens): Tokens {
    return quote(func f() {})
}

public macro tokenIdent(input: Tokens): Tokens {
    return quote(Ident("a"))
}

public macro tokenInt(input: Tokens): Tokens {
    return quote(Int64(1))
}

public macro tokenSemi(input: Tokens): Tokens {
    return quote(Semi())
}

public macro tokenNewline(input: Tokens): Tokens {
    return quote(Newline())
}

public macro tokenParen(input: Tokens): Tokens {
    return quote(Paren())
}

public macro tokenStr(input: Tokens): Tokens {
    return quote(Str("s"))
}

public macro tokenSquare(input: Tokens): Tokens {
    return quote(Square([a]))
}

public macro tokenTypeNode(input: Tokens): Tokens {
    return quote(TypeNode(Int64))
}

public macro tokenDollar(input: Tokens): Tokens {
    return quote(Dollar())
}

public macro tokenAt(input: Tokens): Tokens {
    return quote(At())
}

public macro parseIdentCall(input: Tokens): Tokens {
    return quote(parseIdent("a"))
}
