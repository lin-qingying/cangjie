

var str = ""

interface V {}

interface S {
    static func name(): String {
        return "S"
    }
}

interface I<T> where T <: S {
    func foo(a: T): (T) -> Unit {
        str = T.name()
        return {v: T => ()}
    }
}

open class A <: S & V {
    public static func name(): String {
        return "A"
    }
}

class B <: A {
    public static redef func name(): String {
        return "B"
    }
}

class C <: I<A> & I<B> {}

main() {
    let c = C()
    let v1 = c.foo(A())
    if (str != "A") {
        return 1
    }
    let v2 = c.foo(B())
    if (str != "B") {
        return 2
    }
    return 0
}
