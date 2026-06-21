

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

main() {}
