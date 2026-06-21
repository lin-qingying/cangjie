

main(): Int64 {
    let a = B()
    return 0
}

open class A {
    public var p: Int64 = 8
}

class B <: A {
    public var a: Unit = ()
    public func foo() {
        this.p++
        p++
        super.p++
        <!TYPE_MISMATCH!>this.a++<!>
        <!TYPE_MISMATCH!>a++<!>
        a
    }
}
