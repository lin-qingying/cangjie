


<!VALUE_TYPE_RECURSIVE!>s<!>truct S1 {
    var a: S2
    init(a1: S2) {
        a = a1
    }
}

struct S2 {
    static var b: Array<Int64> = [1, 2, 3]
    var a: S3
    init(a1: S3) {
        a = a1
    }
}

struct S3 {
    var a: S1
    var b: String = "123"
    init(a1: S1) {
        a = a1
    }
}

main() {
    var a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1>
}
