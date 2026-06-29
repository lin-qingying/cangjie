

struct R1 {
    public var i = 0

    public mut func foo() {
        i += 1
    }
}

main() {
    let obj = R1()
    obj.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    return 0
}
