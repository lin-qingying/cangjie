

class A<T> {
    static func test(a: Array<T>, b!: Bool = false) {
        a
    }
}

class B<T> where T <: Comparable<T> & ToString {
    static func test(a: Array<T>, b!: Bool = false) {
        a
    }
}

main() {
    var a = [1, 2]
    a = A.test()
    a = B.test()
}
