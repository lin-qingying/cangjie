


class A {
    func a() {
        this
    }
}

class B {
    func b() {
        A().a()
    }
}

main(): Unit {
    let b: B = <!TYPE_MISMATCH!>B().b()<!>
    b.b()
}
