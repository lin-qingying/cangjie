

package pkg

enum E<T> {
    A

    static func test(a: T) {
        return a
    }
}

main() {
    let v = E.test(123)
    let x = v + E.test(123)
    if (x != 246) {
        return 1
    }
    return 0
}
