

struct R1<T> {
    public var i: T

    public init(x: T) {
        i = x
    }

    public mut func foo(x: T) {
        i = x
    }
}

main() {
    let obj = R1<Int64>(0)
    obj.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>(1)
    return 1
}
