class GennericClassA<T, V> {}

type TypeClass1<T, V> = GennericClassA<T, V>
type TypeClass2<T> = GennericClassA<T, T>
type A = TypeClass1<Int64, Int8>
type B<X> = TypeClass2<X>
type C<X> = TypeClass2<Array<X>>

interface InterfaceI<X> {}

extend<Y> C<Y> <: InterfaceI<Y> {}

main() {
    var a: InterfaceI<Int64> = C<Int64>()
    var b: InterfaceI<Int64> = GennericClassA<Array<Int64>, Array<Int64>>()
    var c: C<Int64> = GennericClassA<Array<Int64>, Array<Int64>>()
    var d: InterfaceI<Int64> = TypeClass2<Array<Int64>>()
    0
}
