

func test() {
    var a: String
    var b: Int64
    var c: String
    var d: Int64
    var f = {=> ("abce", (2, 3.0))}
    (a, (b, c)) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>"TEST"<!>
    (a, (b, c)) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>f()<!>
    (a, (b, c), d) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>f()<!>
    (a, (b, c, d)) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>f()<!>
    (a, b) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>f()<!>
}

main() {
}
