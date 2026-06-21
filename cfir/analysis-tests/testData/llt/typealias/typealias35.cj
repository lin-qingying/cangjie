


class GennericClassA<A, B> {}

type TypeClass<T> = GennericClassA<T, T>

type C<X> = TypeClass<Array<X>>

interface InterfaceI<X> {}

extend<Y> C<Y> <: InterfaceI<Y> {}

main() {
    var i: InterfaceI<Int64> = <!TYPE_MISMATCH!>C<Int64>()<!>
    0
}
