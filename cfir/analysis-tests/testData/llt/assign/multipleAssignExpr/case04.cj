

func test() {
    var a: String
    var b: Int64
    var c: String
    var d: Int64
    var f = {=> ("abce", (2, 3.0))}
    (a, (b, c)) = <!TYPE_MISMATCH!>"TEST"<!>
    (a, (b, c)) = <!TYPE_MISMATCH!>f()<!>
    (a, (b, c), d) = <!TYPE_MISMATCH!>f()<!>
    (a, (b, c, d)) = <!TYPE_MISMATCH!>f()<!>
    (a, b) = <!TYPE_MISMATCH!>f()<!>
}

main() {
}
