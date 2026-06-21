

interface I {
    static func f1(): Unit
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I.f1()<!> // error, cannot directly call
}
