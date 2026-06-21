

class A<T> {
    func test(a: T) {
        1
    }
}

extend<T> A<T> {
    func test(a: ?T) {
        if (let Some(t) <- a) {
            return test(t)
        }
        return 2
    }
}

main() {
    let a: A<Int64> = A<Int64>()
    return a.test(Some(1)) - 1
}
