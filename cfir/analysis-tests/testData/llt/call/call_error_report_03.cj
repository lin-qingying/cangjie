

class A <: ToString {
    public func toString() {
        "a"
    }
}

main(): Int64 {
    var a = A()
    <!RETURN_TYPE_MISMATCH!>print(a)<!>
}
