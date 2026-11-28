// WITH_STDLIB
macro package sample.macros.signature.defaultreturn
import std.ast.*
public macro Identity(input: Tokens) {
    return input
}
