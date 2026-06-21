

interface I<T> {
    static func f1(a: T): Unit
}

interface I1<T> <: I<String> {
    static func f3(a: T) {
        f1("123")
    }
}

interface I2 <: I1<Int64> & I1<String> {
    static func f1(a: Int64): Unit {
        println("Int64 f1 ${a}")
    }
    static func f1(a: String): Unit {
        println("String f1 ${a}")
    }
}

main() {
    I2.f3(1)
}
