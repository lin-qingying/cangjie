


open class A<T, K> {
    static func test(a: T, b: K) {
        return b
    }
}

class B<K> <: A<Int8, Int32> {}

type C<T> = A<T, Int16>

main() {
    let x = C.test(1, 2)
    if (x != 2) {
        return 1
    }
    return 0
}
