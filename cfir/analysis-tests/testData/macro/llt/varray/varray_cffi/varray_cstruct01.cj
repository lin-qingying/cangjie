


struct A {}

@C
struct S {
    var <!ILLEGAL_MEMBER_OF_CSTRUCT!>a<!> = VArray<VArray<VArray<VArray<A, $1>, $1>, $1>, $1>(repeat: [[[A()]]]) // error
    var <!ILLEGAL_MEMBER_OF_CSTRUCT!>b<!> = VArray<A, $1>(repeat: A()) // error
}

main() {}
