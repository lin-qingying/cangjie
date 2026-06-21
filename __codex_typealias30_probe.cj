class GennericClassA<T, V> {}

type TypeClass1<T, V> = GennericClassA<T, V>

type TypeClass2<T> = GennericClassA<T, T>

type A = TypeClass1<Int64, Int8>

type B<X> = TypeClass2<X>

type C<X> = TypeClass2<Array<X>>

interface InterfaceI<X> {}

extend<Y> C<Y> <: InterfaceI<Y> {}

main() {
    var a: A = A()
    var b: B<Int64> = B<Int64>()
    var c: C<Int64> = GennericClassA<Array<Int64>, Array<Int64>>()
    var i: InterfaceI<Int64> = C<Int64>()
    0
}
