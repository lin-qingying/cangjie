

interface I {
    func f(): Int64
}

class C<R> {}

type c<T> = C<T>

extend <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>c<!> {
    public func f1() {
        2
    }
}

main() {
    return 0
}
