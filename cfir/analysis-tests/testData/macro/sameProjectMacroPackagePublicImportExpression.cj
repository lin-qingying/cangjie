// RUN_PIPELINE_TILL: FRONTEND

// FILE: other.cj
macro package a

public import std.deriving.Derive

// FILE: main.cj
import a.Derive

@Derive[Equatable]
class A {
    var a = 1

    init(temp: Int64) {
        a = temp
    }
}
