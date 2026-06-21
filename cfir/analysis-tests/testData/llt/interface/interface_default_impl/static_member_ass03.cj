

interface I {
    static func f1(): Unit
    static func f2(): Unit {
        f1()
    }
    static func f3(): Unit {
        f2()
    }
}

interface I2 {
    static func f4(): Unit {
        <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I.f2()<!> // error
    }
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I.f3()<!> // error
}
