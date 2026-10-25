// quote 与 splice：quote 表达式与 $(...) 插值。
// 官方依据：Cangjie 文档《宏》《quote 与 splice》；cjc 1.0.5 / 1.1.3 零诊断。
import std.ast.*

func useQuote(x: Int64): Tokens {
    return quote($(x))
}

func wrapQuote(input: Tokens): Tokens {
    return quote(hello)
}

func combineQuote(a: Int64, b: Int64): Tokens {
    return quote($(a) + $(b))
}

func nestedQuote(inner: Tokens): Tokens {
    return quote($(inner) + 1)
}
