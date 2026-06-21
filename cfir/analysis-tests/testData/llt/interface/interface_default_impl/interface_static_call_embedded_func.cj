

interface I {
    static func f(): Unit
    static func f1(): Unit {
        func f2(): Unit {
            f()
        }
        f2()
    }
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I.f1()<!>
}
