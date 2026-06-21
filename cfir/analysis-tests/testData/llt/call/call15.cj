

open class A<T> {
    public func test(a: T, b!: Int64 = 1) {}
}

class B<T> <: A<T> {
    public func test(a: T) {}
    public func i(input: T) {
        var I = <!AMBIGUOUS_FUNCTION_CALL!>test<!>(input)
        return I
    }
}

main() {
    return 0
}
