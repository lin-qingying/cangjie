

open class A {
    public var s1: UInt32 = 1
}

class B <: A {
    public var b1: UInt32 = 2
    public var obj: A = <!RECURSIVE_CONSTRUCTOR_CALL!>B()<!>
}

main() {
    var c: B = B()
    0
}
