

class A {
    <!CLASS_UNINITIALIZED_FIELD!>init<!>(a: Int64, c: Bool) {
        let b = 1
        if (c) {
            return
        }
        x = a
        println(x)
    }
    init() {
        x = 1
    }

    public var x: Int64
}

main() {
}
