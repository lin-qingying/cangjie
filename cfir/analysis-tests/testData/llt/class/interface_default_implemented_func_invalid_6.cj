

interface I<T> {
    func foo(x: T) {
        return x
    }
}

open class A<X, Y> <: I<X> & I<Y> {}

class B<X, Y> <: A<X, Y> {
    init(a: X, b: Y) {}
}

func test<X>(a: X) {
    let x = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<X, X><!>(a, a)
    let y = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<!>(a, a)
}

main(): Int64 {
    let x: Int32 = 1
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>test<!>(x)
    var b = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>test<Int64><!>(1)
    return 0
}
