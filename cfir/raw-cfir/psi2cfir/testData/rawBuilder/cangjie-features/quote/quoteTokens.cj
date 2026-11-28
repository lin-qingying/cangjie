// RUN_PIPELINE_TILL: FRONTEND
import std.ast.*

func useQuote(input: Tokens): Tokens {
    return quote(hello)
}
