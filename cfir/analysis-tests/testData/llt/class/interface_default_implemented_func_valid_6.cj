


interface I {
    static func foo(x: Int32): Int32 {
        x
    }
    func coo() {
        foo(2)
    }
}

open class A <: I {
    public static redef func foo(x: Int32): Int32 {
        return 111
    }
}

open class B <: A {
    public static redef func foo(x: Int32): Int32 {
        return 123
    }
}

class C <: B & I {
    public static redef func foo(x: Int32): Int32 {
        return 234
    }
}

main() {
    var a = A()
    var b = B()
    var c = C()
    if (a.coo() != 111 || b.coo() != 123 || c.coo() != 234) {
        return 1
    }
    return 0
}
