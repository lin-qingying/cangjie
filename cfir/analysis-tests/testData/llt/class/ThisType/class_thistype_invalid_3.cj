


class C1 {
    public func foo(): This {
        var a = this
        return <!TYPE_MISMATCH!>a<!>
    }
}

main() {
    return 1
}
