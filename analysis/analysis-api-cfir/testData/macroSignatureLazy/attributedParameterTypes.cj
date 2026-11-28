// WITH_STDLIB
macro package sample.macros.signature.attributed
import std.ast.*
public macro Identity(attributes: Tokens, input: Tokens): Tokens {
    return input
}
