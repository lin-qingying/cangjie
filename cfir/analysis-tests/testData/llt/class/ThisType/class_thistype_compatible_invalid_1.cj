


open class C1 {
    public open func foo(): This {
        return this
    }
}

class C2 <: C1 {
    public override func <!INHERIT_NOT_RETURN_THIS!>foo<!>(): C2 {
        return this
    }
}

main() {
    return 1
}
