

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
        let v1 = test({ // lambda
            => this
        }())
        let v = this
        a = test(this) + test(v) + v1
    }
}

func test(v: B) {
    return v.foo() + v.goo() + v.coo()
}

main(): Int64 {
    let b = B()
    if (b.a != 18) {
        return 1
    }
    return 0
}
