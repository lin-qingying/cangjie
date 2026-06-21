


open class C1<T> {
    public open func foo(): This {
        return this
    }
}

class C2<T> <: C1<T> {
    public override func <!INHERIT_NOT_RETURN_THIS!>foo<!>(): C2<T> {
        return this
    }
}

main() {
    return 1
}
