// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR

// FILE: macros.cj
macro package ide_target_macro

import std.ast.*

public macro GenerateReference(input: Tokens): Tokens {
    return quote(public func generatedReferenceValue(): Int64 { return ideTarget() })
}

// FILE: main.cj
import ide_target_macro.GenerateReference

public func ideTarget(): Int64 { return 42 }

@GenerateReference
public func macroCarrier(): Unit {}

public func useGeneratedReference(): Int64 {
    return generatedReferenceValue()
}
