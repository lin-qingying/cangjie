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

// FILE: testb3.cj


package b.c

import a.Foo

private interface I1 {
    public func f1(): Unit
}

internal interface I2 {
    public func f2(): Unit
}

protected interface I3 {
    public func f3(): Unit
}

public interface I4 {
    public func f4(): Unit
}

extend<T> Foo<T> <: I3 {
    public func f1() {}
    public func f2() {}
    public func f3() {}
    public func f4() {}
}

// FILE: main05.cj



package b.a

import a.*
import b.c.*

main() {
    Foo<Int64>().f0() // ok
    Foo<Int64>().<!NOT_MEMBER_OF!>f1<!>() // error
    Foo<Int64>().<!NOT_MEMBER_OF!>f2<!>() // error
    Foo<Int64>().f3() // ok
}
