

interface I {
    mut static prop a: Int64
    static func fset(): Unit {
        a = 1
    }
    static func fget(): Int64 {
        a
    }
}

interface I2 <: I {}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.fset()<!>
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.fget()<!>
    0
}
