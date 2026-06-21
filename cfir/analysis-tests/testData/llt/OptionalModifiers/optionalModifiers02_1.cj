


interface I1 {
    // no default implementation
    func f(a: Int64): Int32
    static func sf(a: Int64): Int32
}

interface I2 <: I1 {
    override func f(a: Int64) {
        Int32(a + 1)
    }
    static redef func sf(a: Int64): Int32
}

interface I3 <: I1 {
    <!NOTHING_TO_OVERRIDE!>override<!> func f(a: Int32) {
        a + 1
    }
    static <!NOTHING_TO_OVERRIDE!>redef<!> func sf(a: Int32): Int32
}

interface I4 <: I1 {
    <!NOTHING_TO_OVERRIDE!>override<!> func f(a: Int32): Int32
}

main() {}
