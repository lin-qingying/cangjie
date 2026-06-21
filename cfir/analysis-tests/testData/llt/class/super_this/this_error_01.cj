


open class C1 {
    public func foo1() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this<!> = C1()
    }
}

main() {
    return 1
}
