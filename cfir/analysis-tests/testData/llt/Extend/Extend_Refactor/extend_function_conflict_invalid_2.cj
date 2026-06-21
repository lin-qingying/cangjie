


interface I1 {
    func foo(): Int64 {
        1
    }
}

interface I2 {
    func foo(): Int64 {
        2
    }
}

class C1 {}

extend C1 <: I1 {
    func <!WEAK_VISIBILITY!>foo<!>(): Int64 {
        3
    }
}

extend <!WEAK_VISIBILITY!>C1<!> <: I2 {}

main() {
    return -1
}
