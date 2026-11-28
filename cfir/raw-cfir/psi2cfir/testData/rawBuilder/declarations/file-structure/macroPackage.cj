// RUN_PIPELINE_TILL: FRONTEND
macro package macrotest

import std.ast.*

public macro myMacro(input: Tokens): Tokens {
    return input
}
