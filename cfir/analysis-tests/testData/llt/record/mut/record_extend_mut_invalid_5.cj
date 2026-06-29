

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo() {
        i = i + 1
    }
}

main() {
    let obj = R1()
    obj.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    return 0
}
