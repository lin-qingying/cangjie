// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_EXPANSION_DEMAND_AUTO_COMPILE_MACRO_PACKAGES

// FILE: macros.cj
macro package macros.local

import std.ast.*

public macro MakeInt64(input: Tokens): Tokens {
    return quote(41)
}

// FILE: main.cj
import macros.local.MakeInt64

func useSameProjectMacroPackage(): Int64 {
    let value: Int64 = <!MACRO_DEPENDENCY_COMPILE_FAILED!>@MakeInt64()<!>
    return value
}
