


class A {}

func foo(a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>): VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1> { // error x2
    return [A()]
}

main() {
    var a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1> = [A()] // error
    var b = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>(repeat: A()) // error
    var c = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1> {i => A()} // error
    0
}
