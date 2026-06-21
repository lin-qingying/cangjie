

class A {
    static var a: Int64
    static var b: Int64
    static init() {
        b = 1
        a = b
    }
}

main() {
    if (A.a + A.b != 2) {
        return 1
    }
    return 0
}
