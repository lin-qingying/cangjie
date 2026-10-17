// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR

// FILE: macros.cj
macro package macros.expression

import std.ast.*

public macro MakeInt64(input: Tokens): Tokens {
    return quote(42)
}

public macro EchoTokens(input: Tokens): Tokens {
    return input
}

// FILE: main.cj
import macros.expression.EchoTokens
import macros.expression.MakeInt64

func increment(value: Int64): Int64 {
    value + 1
}

func expressionMacroBodySemantics(): Int64 {
    let literal: Int64 = @MakeInt64()
    let binary: Int64 = @EchoTokens(1 + 1)
    let parenthesized: Int64 = @EchoTokens((1 + 1))
    let called: Int64 = @EchoTokens(increment(1))
    let lambdaBlock: Int64 = @EchoTokens({ => let local: Int64 = 40; local + 2 }())
    return literal + binary + parenthesized + called + lambdaBlock
}
