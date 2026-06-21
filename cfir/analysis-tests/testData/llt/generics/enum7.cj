

enum E<T0> {
    | S(T0)
    | N
}

interface I<T1> {
    func foo(): Unit
}

extend<T2> E<T2> <: I<E<T2>> {
    public func foo(): Unit {
        let x: E<E<T2>> = E<E<T2>>.N
    }
}

main() {}
