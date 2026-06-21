


extend Int64 {
    static func test() {
        1
    }
}

extend Int64 {
    <!UNUSED_FUNCTION!>static func test(<!UNUSED_VARIABLE!>a<!>: Int64) {
        2
    }<!>
}

main() {
    var x: () -> Int64 = Int64.test
    return x() - 1
}
