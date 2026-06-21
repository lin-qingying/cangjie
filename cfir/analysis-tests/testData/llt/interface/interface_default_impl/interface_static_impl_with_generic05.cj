

interface I<T> {
    static func f1(a: T): Unit

    static func f3(a: T): Unit {
        f1(a)
    }
}

interface I1 <: I<Int64> & I<String> {
    static func f1(a: Int64): Unit {
        f1("1")
    }
    static func f1(a: String): Unit {
        f2()
    }
    static func f2(): Unit
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I1.f3(1)<!>
}
