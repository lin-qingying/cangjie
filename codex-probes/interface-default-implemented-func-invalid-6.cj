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
    let x = B<X, X>(a, a)
    let y = B(a, a)
}

main(): Int64 {
    let x: Int32 = 1
    var a = test(x)
    var b = test<Int64>(1)
    return 0
}
