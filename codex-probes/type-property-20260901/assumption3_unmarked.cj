class C<X> where X <: C<C<C<C<Int32>>>> {
    func foo() {}
    public init(input: X) {
        let a: Unit = input.foo()
    }
}

interface I<X> where X <: I<I<I<I<Int32>>>> {}

main(): Int64 {
    return 0
}
