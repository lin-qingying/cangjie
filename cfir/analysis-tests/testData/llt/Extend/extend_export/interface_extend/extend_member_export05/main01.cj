// FILE: testa.cj


package a

private interface I0 {
    public func f0(): Unit
}

public class Foo<T> {}

// The extension is exported.
extend<T> Foo<T> <: I0 {
    public func f0(): Unit {}
}

// FILE: main01.cj


package b.c

import a.*

main() {
    Foo<Int64>().f0() // ok
}
