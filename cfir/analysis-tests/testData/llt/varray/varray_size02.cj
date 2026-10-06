


foreign func foo1(p: CPointer<Int64>): Unit

struct S {
    public var arr: VArray<Int32, $3> = [1, 2, 3]
}

unsafe main() {
    var a = VArray<Int64, $3>(repeat: 0)
    foo1(inout <!INOUT_MUST_BE_VAR_VARIABLE!>a.size<!>)
    return 0
}
