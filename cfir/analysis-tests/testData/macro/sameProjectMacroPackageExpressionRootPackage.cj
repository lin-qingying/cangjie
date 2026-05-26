// RUN_PIPELINE_TILL: FRONTEND

// FILE: macro.cj
macro package macro_definition

import std.ast.*

public macro MakeInt64(input: Tokens): Tokens {
    return quote(41)
}

// FILE: main.cj
import macro_definition.MakeInt64

func useSameProjectRootMacroPackage(): Int64 {
    let value: Int64 = @MakeInt64()
    return value
}
