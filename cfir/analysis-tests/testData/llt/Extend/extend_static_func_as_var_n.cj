

extend Int64 {
    static func test() {}
}

extend Int64 {
    static func test(a: Int64) {}
}

main() {
    var x = Int64.<!AMBIGUOUS_USE!>test<!>
    return 0
}
