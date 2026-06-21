


interface C {
    func f() {
        0
    }
}

open class E {}

extend E <: C {} // inherit 'f' for class E

class F <: E {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>f<!>() {
        1
    } // conflic with E's extend function 'f'
}

main() {
    return 0
}
