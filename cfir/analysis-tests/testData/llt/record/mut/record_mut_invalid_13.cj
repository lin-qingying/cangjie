

struct R1 {
    public var i = 0

    public mut func foo(): Unit {
        i += 1
    }

    public var fn = {=> <!USED_BEFORE_INITIALIZATION!>foo<!>()}
}

main() {
    return 1
}
