


struct S {
    var x = 0

    init() {
        func g() {
            <!ILLEGAL_CAPTURE_THIS!>x<!> += 1
        }
        g()
    }

    mut func f() {
        func g() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>x<!> += 1
        }
        g()
        x += 1
    }
}

main() {
    var x = S()
    x.f()
    x.x
}
