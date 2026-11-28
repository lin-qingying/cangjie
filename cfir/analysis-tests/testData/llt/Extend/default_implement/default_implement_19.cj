// FILE: default_implement_19.cj

interface I1 {
    func foo(): Unit {
        println(1)
    }
    static func goo(): Unit {
        println(11)
    }
}

interface I2 <: I1 {
    func foo(): Unit {
        println(2)
    }
    static func goo(): Unit {
        println(22)
    }
}

class A {}

extend A <: I1 {}

extend A <: I2 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(): Unit {
        println(3)
    } // error
    public static func <!EXTEND_MEMBER_CANNOT_SHADOW!>goo<!>(): Unit {
        println(33)
    } // error
}

class B {}

extend B <: I1 {}

extend B {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(): Unit {
        println(4)
    } // error
    public static func <!EXTEND_MEMBER_CANNOT_SHADOW!>goo<!>(): Unit {
        println(44)
    } // error
}

class C {}

extend C <: I1 {
    public func foo(): Unit { }
}

extend C <: I2 { }

interface GenericDefaultSurface<T> {
    func choose(value: T): Unit { }
}

interface GenericMarker { }

class GenericBox { }

extend GenericBox <: GenericDefaultSurface<Int64> { }

extend GenericBox <: GenericMarker {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>choose<!>(value: Int64): Unit { }
}

main() {}

// FILE: cross_base.cj
package cross.base

public class CrossTarget {}

// FILE: cross_library.cj
package cross.library

import cross.base.CrossTarget

public interface CrossParent {}

public interface CrossChild <: CrossParent {
    func crossDefault(): Unit {}
}

extend CrossTarget <: CrossChild {}

// FILE: cross_consumer.cj
package cross.consumer

import cross.base.CrossTarget
import cross.library.*

extend CrossTarget <: CrossParent {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>crossDefault<!>(): Unit {}
}
