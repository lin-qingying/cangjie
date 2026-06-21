

class A<T> {
    func test<R>(a: T, b: R) {
        return 123
    }
}

extend<T> A<T> {
    func foo(a: T, b: T) {
        return test<T>(a, b)
    }
}

main() {
    let a = A<Int64>()
    if (a.foo(1, 2) != 123) {
        return 1
    }
    return 0
}
