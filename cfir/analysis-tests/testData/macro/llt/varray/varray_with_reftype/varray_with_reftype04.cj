


struct S1 {
    var a: S2
    init(a1: S2) {
        a = a1
    }
}

struct S2 {
    var a: S3
    static var b: String = "123" // ok
    init(a1: S3) {
        a = a1
    }
}

struct S3 {
    var a: String = "123" // error
}

main() {
    var a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1>
}
