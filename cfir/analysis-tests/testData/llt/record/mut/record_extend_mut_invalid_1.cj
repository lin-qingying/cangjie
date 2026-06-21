


class C1 {
    public var i: Int64 = 0
}

extend C1 {
    public <!MUT_ONLY_ON_FUNCTION!>mut<!> func foo() {
        this.i = 1
    }
}

main() {
    return 1
}
