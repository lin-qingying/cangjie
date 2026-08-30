

struct R1 {
    public var i = 0

    public mut func foo(): Unit {
        i += 1
    }

    public var fn = {=> <!ILLEGAL_USAGE_OF_MEMBER!>foo<!>()}
}

main() {
    return 1
}
