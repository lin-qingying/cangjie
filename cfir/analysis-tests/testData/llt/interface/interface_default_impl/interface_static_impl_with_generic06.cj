

interface I<T> {
    static func f1(a: T): Unit

    static func f3(a: T): Unit {
        f1(a)
    }
}

interface I1 <: I<Int64> & I<String> {
    static func f1(a: Int64): Unit {
        print("1")
        f1("1")
    }
    static func f1(a: String): Unit {
        print("2")
        f2()
    }
    static func f2(): Unit {
        println("3")
    }
}

main() {
    I1.f3(1)
    0
}
