


interface C1 {}

open class A<T> where T <: C1 {
    public func foo(a: T) {
        return a
    }
}

class D<X> <: A<X> where X <: A<X> {}

func bar<T>() where T <: A<T> {
    var a: A<T> = D<T>()
    return a
}

class E <: A<E> & C1 {}

main() {
    var obj = bar<E>()
    return 0
}
