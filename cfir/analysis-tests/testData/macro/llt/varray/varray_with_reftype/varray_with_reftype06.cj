


func foo1<T>(x: T): Unit {
    var s1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S<T><!>, $1> = [S(x)] // error
    var s2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>T<!>, $1> = [x] //error
}

struct S<T> {
    var a: T
    init(a1: T) {
        a = a1
    }
}

main(): Int64 {
    0
}
