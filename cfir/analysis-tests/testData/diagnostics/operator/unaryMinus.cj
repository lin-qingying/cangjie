class B {}

class C {
    public operator func -(): C {
        return C()
    }
}

func name1(): Unit {
    let a = C()

    -a
}

func name(): Unit {
    let a = B()

    <!UNRESOLVED_REFERENCE!>-<!>a
}
