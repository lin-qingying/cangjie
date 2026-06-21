

interface D<T> {}

class C <: D<Int32> {}

class A<T> {
    public func test(a: A<T>): C {
        return C()
    }
}

main(): Int64 {
    var a: A<D<Int32>> = A<D<Int32>>()
    var b: A<C> = A<C>()
    var c: A<D<Int8>> = A<D<Int8>>()
    var d: A<Int8> = A<Int8>()
    return 0
}
