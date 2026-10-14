macro package semanticTop

import std.ast.*

public macro GenerateTopLevel(input: Tokens): Tokens {
    return quote(public func generatedTopLevel(value: Int64): Int64 { return value })
}
