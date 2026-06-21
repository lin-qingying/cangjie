

class A<T> {}

interface I<T> {
    func test(a: T): T
}

extend<T> A<T> <: I<T> {
    public func test(a: T): T {
        print("done")
        return a
    }
}

main() {
    let a: A<Int32> = A<Int32>()
    a.test(1)
    0
}
