

macro package define

internal import std.ast.*

public macro SumOfEachAbs(input: Tokens) {
    let x = parseExpr(input) | return quote($<!UNRESOLVED_REFERENCE!>x<!>)
    input
}

main() {}
