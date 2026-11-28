

macro package default.define

import std.ast.*

public macro echo(input: Tokens) {
    return input
}
