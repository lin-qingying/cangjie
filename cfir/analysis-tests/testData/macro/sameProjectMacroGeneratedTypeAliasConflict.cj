// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
// cjc 1.0.5 treats a type alias and its expanded type as the same function parameter signature.
// The call-site diagnostic is CFIR's source mapping and does not assert cjc's primary declaration anchor.

// FILE: macros.cj
macro package generated_type_alias_conflict

import std.ast.*

public macro GenerateAliasConflict(input: Tokens): Tokens {
    return quote(
        type NumberAlias = Int64;
        public func aliasTarget(value: NumberAlias): Int64 { return value };
        public func aliasTarget(value: Int64): Int64 { return value }
    )
}

// FILE: main.cj
import generated_type_alias_conflict.GenerateAliasConflict

<!CONFLICTING_OVERLOADS!>@GenerateAliasConflict
public func generatedCarrier(): Unit {}<!>

main(): Unit {}
