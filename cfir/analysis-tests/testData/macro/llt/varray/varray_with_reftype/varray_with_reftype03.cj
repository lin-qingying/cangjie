


struct S1 {
    var a: (() -> Unit, Int64) = ({=>}, 123)
}

struct S2 {
    func foo(): Unit {}
}

struct S3 {
    var a: () -> Unit = {=>}
}

struct S4 {
    var a: CFunc<() -> Unit> = {=>}
}

func foo(): Unit {}

@C
func cfoo(): Unit {}

main(): Int64 {
    // Tuple
    var a1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> = [S1()] // error
    var a2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(Array<Int64>, String)<!>, $1> = [([1, 2, 3], "123")] // error
    var a3: VArray<(Int32, Int64), $1> = [(2, 1)] // pass
    var a4: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(CFunc<() -> Unit>, () -> Unit)<!>, $1> = [({=>}, {=>})] // error

    // Lambda
    var b1: VArray<S2, $1> = [S2()] // pass
    var b2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S3<!>, $1> = [S3()] // error
    var b3: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>() -> Unit<!>, $1> = [foo] // error
    var b4: VArray<CFunc<() -> Unit>, $1> = [cfoo] // pass
    var b5: VArray<S4, $1> = [S4()] // pass
    0
}
