


struct A {}

@C
struct S {
    var a = VArray<VArray<VArray<VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>, $1>, $1>, $1>(repeat: [[[A()]]]) // error
    var b = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>(repeat: A()) // error
}

main() {}
