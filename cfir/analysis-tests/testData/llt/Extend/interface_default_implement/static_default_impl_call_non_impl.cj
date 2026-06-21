

public interface I1 {
    static func foo1(): Unit
}

public interface I2 <: I1 {
    static func foo2() {
        foo1()
    }
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.foo2()<!>
    return 1
}
