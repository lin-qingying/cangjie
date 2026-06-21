

public struct A<T> {
    A(public let b: T) {}
}

extend<R> A<R> {
    public func test() {
        let a = A(Array<R>())
        let b: Array<R> = a.b
    }
}

main() {}
