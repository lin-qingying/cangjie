

interface I<T> {
    func f(): I<T>
}

enum E <: I<E> {
    E1 | E2

    public func f(): I<E> {
        return this
    }
}

main(): Int64 {
    0
}
