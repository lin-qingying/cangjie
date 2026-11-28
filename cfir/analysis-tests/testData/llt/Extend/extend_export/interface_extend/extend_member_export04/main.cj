// FILE: testa.cj


package a

public class Foo {}

// FILE: testb.cj


package b

import a.Foo

private interface I1 {
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> func f1(): Unit
}

internal interface I2 {
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> func f1(a: Int64): Unit
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> func f2(): Unit
}

extend Foo <: I1 & I2 {
    public func f1() {}
    public func f1(a: Int64) {}
    public func f2(): Unit {}
}

// FILE: main.cj


package b.c

import a.*
import b.*

main() {
    Foo().f1<!WRONG_NUMBER_OF_ARGUMENTS!>()<!> // error
    Foo().f1(1) // ok
    Foo().f2() // ok
}
