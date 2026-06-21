


interface I<T> {}

class A <: I<Any> {}

func test<T>(a: I<T>) {}

main() {
    test(A())
    0
}
