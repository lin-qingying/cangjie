


main() {
    var b = R<!WRONG_NUMBER_OF_ARGUMENTS!>(a: 6)<!>
    b.c(7)
    return Int64(b.a - 8)
}

interface I {
    func c(b: Int32): Int32
    func cc(): Int32
}

struct R {
    public var a: Int32 = 8
    public mut func c(b: Int32): Int32 {
        let c = <!UNRESOLVED_REFERENCE!>c<!>c
        a = c() + b
        return a
    }
}

<!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>e<!>xtend R <: I {
    public func cc(): Int32 {
        return 1
    }
}
