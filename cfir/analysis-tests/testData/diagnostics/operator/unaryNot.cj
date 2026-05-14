class B {}

class C {
    public operator func !(): Bool {
        return true
    }
}

func name1(): Unit {
    let a = C()

    !a
}

func name(): Unit {
    let a = B()

    <!INVALID_UNARY_EXPR!>!a<!>
}
