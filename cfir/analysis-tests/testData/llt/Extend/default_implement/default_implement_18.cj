

interface I1 <: I<Int64> {
    func foo(a: Int64): Unit {
        println(2)
    }
}

interface I<T> {
    func foo(a: T): Unit {
        println(1)
    }
}

class A {}

extend A <: I<Int64> {}

extend A <: I1 {}

main() {
    A().foo(1)
}
