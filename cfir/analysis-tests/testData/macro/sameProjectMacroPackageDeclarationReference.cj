// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR

// FILE: macros.cj
macro package macros.local

import std.ast.*

public macro DeclareGenerated(input: Tokens): Tokens {
    return quote(func generatedValue(): Int64 { return 43 })
}

// FILE: main.cj
import macros.local.DeclareGenerated

@DeclareGenerated
func macroCarrier(): Unit {}

func useGeneratedDeclaration(): Int64 {
    return generatedValue()
}
