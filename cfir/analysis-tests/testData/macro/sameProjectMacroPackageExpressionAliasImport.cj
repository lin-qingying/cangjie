// RUN_PIPELINE_TILL: FRONTEND

// FILE: macros.cj
macro package macros.local

import std.ast.*

public macro MakeInt64(input: Tokens): Tokens {
    return quote(41)
}

// FILE: main.cj
import macros.local.MakeInt64 as Make

func useSameProjectMacroPackageAliasImport(): Int64 {
    let value: Int64 = @Make()
    return value
}
