// RUN_PIPELINE_TILL: FRONTEND
import std.ast.*

func useQuote(x: Int64): Tokens {
    return quote($(x))
}
