macro package patternmacros

import std.ast.*

public macro Keep(input: Tokens): Tokens {
    return input
}

public macro EmitGlobals(input: Tokens): Tokens {
    return quote(
        public var (generatedCount, (generatedLabel, generatedFlag), _) =
            (hostAnchor, ("macro", true), 99)
    )
}
