


class C1 {
    public func foo(): This {
        return <!TYPE_MISMATCH!>C1()<!>
    }
}

main() {
    return 1
}
