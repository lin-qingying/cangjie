
class B {}

class C {
    public operator func [](index: Int64): Int64 {
        return index
    }
}

func name1(): Unit {
    let a = C()

    a[0]
}

func name(): Unit {
    let a = B()

    <!UNRESOLVED_REFERENCE!>a[0]<!>
}
