

struct A {
    mut func f() {}
}

class B {
    var a = A()

    mut prop x: A {
        get() {
            a
        }
        set(_) {}
    }

    func f() {
        a
    }
}

main() {
    B().x.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
}
