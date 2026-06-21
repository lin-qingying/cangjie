


struct A {
    public mut prop a: Int64 {
        <!CANNOT_HAVE_PARAMETER!>g<!>et(b) {
            1
        }
        set(v) {}
    }
}

main(): Int64 {
    0
}
