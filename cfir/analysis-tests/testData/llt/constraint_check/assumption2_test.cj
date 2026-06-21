


class Z<T> {
    public func foo() {}
}

<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>c<!>lass C<X> where X <: Z<C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>I<!>nt32>> {
    init(input: X) {
        let a: Unit = input.foo()
    }
}

main(): Int64 {
    return 0
}
