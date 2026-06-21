


interface I {}

class A <: I {}

struct S1 {
    var a: A = A()
}

struct S2<T> {
    var a: T
    init(a1: T) {
        a = a1
    }
}

struct S2_1<T> {
    var a: Int64 = 0
}

struct S3 {
    prop a: A {
        get() {
            A()
        }
    }
}

struct S4 {
    var a: A
    init(a1: A) {
        a = a1
    }
}

struct S5 {
    static var a: A = A()
}

enum E {
    | C
    | D
}

main() {
    var a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> = [S1()] // error
    var b: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S2<A><!>, $1> = [S2(A())] // error
    var c: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S2<I><!>, $1> = [S2(A())] // error
    var c0 = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>I<!>, $1>(repeat: A()) // error
    var c1 = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>I<!>, $1>({i => A()}) // error
    var c2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>I<!>, $1> = [A()] // error
    var c3: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S2<A><!>, $1> = [S2(A())] // error
    var d: VArray<S3, $1> = [S3()] // pass
    var e: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S4<!>, $1> // error
    var f: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>String<!>, $1> = ["123"] // error
    var g = VArray<S5, $1>(repeat: S5()) // pass
    var h: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>E<!>, $3> = [E.C, E.D, E.D] // error
    0
}
