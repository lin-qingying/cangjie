


interface I1 {
    func a() {}
}

interface I2 <: I1 {
    func b() {
        <!INTERFACE_SUPER_NOT_ALLOWED!>super<!>.a()
    }
}

main(): Int64 {
    return 0
}
