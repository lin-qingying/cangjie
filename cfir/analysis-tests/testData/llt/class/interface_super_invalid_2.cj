


interface I1 {
    func a() {}
}

interface I2 <: I1 {
    func b() {
        var c = <!INTERFACE_SUPER_NOT_ALLOWED!>super<!>.a
        c()
    }
}

main(): Int64 {
    return 0
}
