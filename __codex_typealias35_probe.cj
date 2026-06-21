class GennericClassA<A, B> {}

type TypeClass<T> = GennericClassA<T, T>

type C<X> = TypeClass<Array<X>>

interface InterfaceI<X> {}

extend<Y> C<Y> <: InterfaceI<Y> {}

main() {
    var i: InterfaceI<Int64> = C<Int64>()
    0
}
