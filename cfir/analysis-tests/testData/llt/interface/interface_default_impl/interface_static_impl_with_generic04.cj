

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
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.f3(1)<!>
}
