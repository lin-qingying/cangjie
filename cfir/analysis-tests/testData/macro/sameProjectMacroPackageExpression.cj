// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR

// FILE: macros.cj
macro package macros.local

import std.ast.*

public macro MakeInt64(input: Tokens): Tokens {
    return quote(41)
}

public macro EchoInput(input: Tokens): Tokens {
    return input
}

// FILE: main.cj
import macros.local.EchoInput
import macros.local.MakeInt64

func useSameProjectMacroPackage(): Int64 {
    let value: Int64 = @MakeInt64()
    return value
}

func useSameProjectMacroPackageInput(): Int64 {
    let value: Int64 = @EchoInput(42)
    return value
}
