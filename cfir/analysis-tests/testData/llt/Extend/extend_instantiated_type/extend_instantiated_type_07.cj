

class B<T> {}

class A<T> {}

extend<T> A<B<T>> {
    static func test(a: B<T>) {
        11
    }
}

main() {
    let v = A.test(B<Int64>())
    let v2 = A<B<Int64>>.test(B<Int64>())
    if (v != 11 || v2 != v) {
        return 1
    }
    return 0
}
