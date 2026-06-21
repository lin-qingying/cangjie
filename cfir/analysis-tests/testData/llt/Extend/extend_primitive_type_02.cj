

extend Int64 {
    public func add() {
        return this + 1
    }
}

main() {
    1.add
    1.<!UNRESOLVED_REFERENCE!>addd<!>
    0
}
