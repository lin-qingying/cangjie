// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
// cjc 1.0.5 reports one sema_overload_conflicts for the same-package declaration group.
// The marked physical declaration is the later CFIR canonical representative by file basename, not a language-level cjc anchor guarantee.

// FILE: macros.cj
macro package cross_file_generated_declaration_conflict

import std.ast.*

public macro GenerateDuplicate(input: Tokens): Tokens {
    return quote(public func ideTarget(): Int64 { return 7 })
}

// FILE: generated.cj
package shared_macro_group

import cross_file_generated_declaration_conflict.GenerateDuplicate

@GenerateDuplicate
func generatedCarrier(): Unit {}

// FILE: physical.cj
package shared_macro_group

func <!CONFLICTING_OVERLOADS!>ideTarget<!>(): Int64 { return 42 }

// FILE: main.cj
package shared_macro_group

main(): Unit {}
