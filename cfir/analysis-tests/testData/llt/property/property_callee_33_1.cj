


struct Foo {
    public var ccc: Int32 = 2
    public prop ddd: Int32 {
        get() {
            <!CANNOT_MODIFY_VAR!>ccc<!> = 1
            return ccc
        }
    }
}

main(): Int32 {
    var f = Foo()
    let t = f.ddd
    return t
}
