

class A {
    var a: Int64 = 0
    init(n: Int64) {
        a = n
    }
    public operator func *(i: Int64): A {
        return A(a * i)
    }
}

extend Int64 {
    public operator func *(a: A): A {
        return a * this
    }
}

func s(a: String, b: Int32) {
    1
}

func s(a: String, b: A) {
    2
}

main() {
    s("a", 8 * A(1))
}
