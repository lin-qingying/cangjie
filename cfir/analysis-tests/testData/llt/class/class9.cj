

class d {
    public func test() {}
    public var b = <!ILLEGAL_USAGE_OF_MEMBER!>test<!>
}

main() {
    var a = d()
    var b = a.test
    return 0
}
