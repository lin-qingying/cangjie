// RUN_PIPELINE_TILL: FRONTEND

// FILE: macros.cj
macro package untitled89.b

import std.ast.*

public macro a11(a1: Tokens): Tokens {
    return a1
}

// FILE: main.cj
import untitled89.b.a11

func testImportOnly() {}
