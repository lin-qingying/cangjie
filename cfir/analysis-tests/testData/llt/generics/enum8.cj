

interface I<T> {
    func foo(): Option<T>
}

extend<T> Option<T> <: I<Option<T>> {
    public func foo(): Option<Option<T>> {
        return Option<Option<T>>.None
    }
}

main() {}
