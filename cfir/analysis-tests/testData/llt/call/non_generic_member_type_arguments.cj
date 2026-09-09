func plain(value: Int64): Unit {}

class Host<T> {
    public func member(value: Int64): Unit {}
    public func member(): Unit {}
    public func single(value: Int64): Unit {}
    public func generic<S>(value: S): S { value }
}

extend<T> Host<T> {
    public func extended(): Unit {}
}

class Plain {
    public func member(value: Int64): Unit {}
    public static func staticMember(): Unit {}
    public func callbackFactory(): (Int64) -> Unit { { value: Int64 => () } }
    public func inside(): Unit {
        <!GENERIC_ARGUMENT_NO_MATCH!>member<Int64>(1)<!>
        this.member<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>(1)
    }
}

extend Plain {
    public func extended(): Unit {}
}

struct Record {
    public func member(value: Int64): Unit {}
}

func makeHost<T>(): Host<T> { Host<T>() }

main() {
    <!GENERIC_ARGUMENT_NO_MATCH!>plain<Int64>(1)<!>
    <!GENERIC_ARGUMENT_NO_MATCH!>Plain.staticMember<Int64>()<!>
    let plainValue = Plain()
    plainValue.member<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>(1)
    plainValue.member<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>("text")
    plainValue.member<<!UNDECLARED_TYPE_NAME!>Missing<!>>(1)
    plainValue.member<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>(<!UNRESOLVED_REFERENCE!>missingValue<!>)
    plainValue.member<Int64><!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
    plainValue.extended<Int64>()
    plainValue.extended<Int64><!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    plainValue.callbackFactory<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>()(1)
    Record().member<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>(1)

    let host = Host<Int64>()
    host.member<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>(1)
    host.member<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Rune><!>()
    host.single<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64, Rune><!>(1)
    host.extended<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Rune><!>()
    host.generic<Int64>(1)
    host.generic(1)
    makeHost<Int64>().single<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Rune><!>(1)
    (host).single<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Rune><!>(1)
}
