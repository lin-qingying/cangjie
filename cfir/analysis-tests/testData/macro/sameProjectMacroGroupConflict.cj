// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
// cjc 1.0.5 reports one sema_overload_conflicts for this same-signature declaration group.
// The inline range is CFIR's deterministic diagnostic mapping, not a language-level primary-anchor guarantee.

// FILE: macros.cj
macro package generated_group_conflict

import std.ast.*

public macro GenerateDuplicate(input: Tokens): Tokens {
    return quote(public func ideTarget(): Int64 { return 7 })
}

// FILE: main.cj
import generated_group_conflict.GenerateDuplicate

@GenerateDuplicate
func firstCarrier(): Unit {}

<!CONFLICTING_OVERLOADS!>@GenerateDuplicate
func secondCarrier(): Unit {}<!>

main(): Unit {}
