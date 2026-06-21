


<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>c<!>lass C<X> where X <: C<C<C<C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>I<!>nt32>>>> {
    func foo() {}
    public init(input: X) {
        let a: Unit = input.foo()
    }
}

<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>i<!>nterface I<X> where X <: I<I<I<I<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>I<!>nt32>>>> {}

main(): Int64 {
    return 0
}
