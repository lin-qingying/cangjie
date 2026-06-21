// FILE: define.cj


macro package FFF

internal import std.ast.*

public macro QA2(input: Tokens): Tokens {
    input
}

// FILE: typeaslias.cj



import FFF as TT

@TT.QA2
func fk() {}

var vt1 = fk()

main() {}
