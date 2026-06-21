

interface I {
    func coo() {
        1
    }
}

open class A <: I {
    protected func foo() {
        2
    }
}

extend A {
    func goo() {
        3
    }
}

class B <: A {
    var a = 0
    public init() {
        { // lambda
            =>
            let v1 = foo() + goo() + coo()
            let f1 = this.coo
            let f2 = this.foo
            let v2 = f1() + f2()
            a = v1 + v2
        }()
    }
}

main(): Int64 {
    let b = B()
    if (b.a != 9) {
        return 1
    }
    return 0
}
