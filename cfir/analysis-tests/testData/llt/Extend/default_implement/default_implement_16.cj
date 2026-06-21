

interface I<T> {
    func foo(a: T): Unit {
        println("foo")
    }
}

interface I1<T> <: I<T> {}

class A<T> {}

extend<T> A<T> <: I<T> {}

extend<T> A<T> <: I1<T> {}

main() {
    A<Int64>().foo(1)
}
