

class d {
    public func test() {}
    public var b = <!USED_BEFORE_INITIALIZATION!>test<!>
}

main() {
    var a = d()
    var b = a.test
    return 0
}
