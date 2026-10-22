// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
// Function type parameters are alpha-equivalent by declaration position; renaming T to U does not create an overload.

// FILE: macros.cj
macro package generated_alpha_equivalent_generic_conflict

import std.ast.*

public macro GenerateAlphaEquivalent(input: Tokens): Tokens {
    return quote(public func alphaTarget<U>(value: U): Int64 { return 2 })
}

// FILE: main.cj
import generated_alpha_equivalent_generic_conflict.GenerateAlphaEquivalent

public func alphaTarget<T>(value: T): Int64 { return 1 }

<!CONFLICTING_OVERLOADS!>@GenerateAlphaEquivalent
public func generatedCarrier(): Unit {}<!>

main(): Unit {}
