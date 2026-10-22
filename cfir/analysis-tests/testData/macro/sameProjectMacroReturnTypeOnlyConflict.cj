// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
// cjc 1.0.5 reports one sema_overload_conflicts because return types alone do not distinguish overloads.
// The diagnostic is represented on the macro invocation; this test does not claim a language-level primary declaration anchor.

// FILE: macros.cj
macro package generated_return_type_conflict

import std.ast.*

public macro GenerateStringTarget(input: Tokens): Tokens {
    return quote(public func ideTarget(): String { return "generated" })
}

// FILE: main.cj
import generated_return_type_conflict.GenerateStringTarget

func ideTarget(): Int64 { return 42 }

<!CONFLICTING_OVERLOADS!>@GenerateStringTarget
func generatedCarrier(): Unit {}<!>

main(): Unit {}
