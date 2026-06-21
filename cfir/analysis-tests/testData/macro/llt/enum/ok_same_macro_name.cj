

macro package define

internal import std.ast.*

enum E {
    e
}

public macro e(x: Tokens) {
    x
}

main() {
    let _ = e
}
