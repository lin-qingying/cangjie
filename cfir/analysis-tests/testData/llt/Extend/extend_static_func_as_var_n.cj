

extend Int64 {
    static func test() {}
}

extend Int64 {
    static func test(a: Int64) {}
}

main() {
    var x = <!AMBIGUOUS_USE!>Int64.test<!>
    return 0
}
