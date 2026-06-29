
// WITH_EXTRA_CHECKERS

struct B {
    public var b = 3
}

class A {
    public mut prop a: B {
        get() {
            return B()
        }
        set(_) {
            return ()
        }
    }
    public func f() {
        return B()
    }
}

main() {
    var a = A()
    <!UNUSED_EXPRESSION!>a.a.b<!>
    if (a.a.b == 3) {
        print("${a.a.b}\n")
        return 0
    }
    a.a = B()
    a.f()
    return 1
}
