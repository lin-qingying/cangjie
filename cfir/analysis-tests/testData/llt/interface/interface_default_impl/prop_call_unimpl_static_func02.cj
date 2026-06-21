

interface I {
    static func foo(): Int64
    static func foo(a: Int64): Int64
    mut static prop a: Int64 {
        get() {
            foo()
        }
        set(x) {
            foo(x)
        }
    }
}

interface I2 <: I {}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.a = 10<!>
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.a<!>
    0
}
