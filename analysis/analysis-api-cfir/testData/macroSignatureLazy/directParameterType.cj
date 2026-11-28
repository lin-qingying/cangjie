// WITH_STDLIB
macro package sample.macros.signature.direct
import std.ast.*
public macro Identity(input: Tokens): Tokens {
    return input
}
