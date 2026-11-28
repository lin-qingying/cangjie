// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
// The call-site range is CFIR's source mapping for the generated declaration, not a language-level cjc anchor guarantee.

// FILE: macros.cj
macro package generated_declaration_conflict

import std.ast.*

public macro GenerateDuplicate(input: Tokens): Tokens {
    return quote(public func ideTarget(): Int64 { return 7 })
}

// FILE: main.cj
import generated_declaration_conflict.GenerateDuplicate

func ideTarget(): Int64 { return 42 }

<!CONFLICTING_OVERLOADS!>@GenerateDuplicate
func generatedCarrier(): Unit {}<!>

main(): Unit {}
