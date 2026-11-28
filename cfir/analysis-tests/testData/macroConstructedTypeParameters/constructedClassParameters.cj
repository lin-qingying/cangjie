// RUN_PIPELINE_TILL: FRONTEND

// FILE: macros.cj
macro package capture_macros

import std.ast.*

public macro M(input: Tokens): Tokens {
    return input
}

public macro EmitCtor(input: Tokens): Tokens {
    return quote(public init() {})
}

// FILE: main.cj
package capture_use

import capture_macros.*

class ViaInit<T> {
    @M public init(value: T) {}
}

struct ViaPrimary<T> {
    @M public ViaPrimary(value: T) {}
}

class Emitted<T> {
    @EmitCtor()
}

@M
class ViaNominal<T> {
    public init(value: T) {}
}

main(): Int64 {
    ViaInit<Int64>(1)
    ViaPrimary<Int64>(2)
    Emitted<Int64>()
    ViaNominal<Int64>(3)
    return 0
}
