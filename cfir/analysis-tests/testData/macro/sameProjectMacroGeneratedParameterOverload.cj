// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR

// FILE: macros.cj
macro package generated_parameter_overload

import std.ast.*

public macro GenerateOverload(input: Tokens): Tokens {
    return quote(
        public func ideTarget(value: Int64): Int64 { return value };
        public func ideTarget(value: String): String { return value }
    )
}

// FILE: main.cj
import generated_parameter_overload.GenerateOverload

func ideTarget(): Int64 { return 42 }

@GenerateOverload
func generatedCarrier(): Unit {}

main(): Int64 {
    let intValue = ideTarget(7)
    let stringValue = ideTarget("generated")
    println(intValue)
    println(stringValue)
    return ideTarget()
}
