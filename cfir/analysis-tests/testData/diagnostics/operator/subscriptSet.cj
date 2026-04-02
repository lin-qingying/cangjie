
class B {}

class C {
    public operator func [](index: Int64, value!: Int64): Unit {
        return
    }
}

func name1(): Unit {
    let a = C()

    a[0] = 1
}

func name(): Unit {
    let a = B()

    <!UNRESOLVED_REFERENCE!>a[0]<!> = 1
}
