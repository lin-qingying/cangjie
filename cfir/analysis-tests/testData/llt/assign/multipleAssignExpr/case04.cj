

func test() {
    var a: String
    var b: Int64
    var c: String
    var d: Int64
    var f = {=> ("abce", (2, 3.0))}
    <!TYPE_MISMATCH!>(a, (b, c)) = "TEST"<!>
    <!TYPE_MISMATCH!>(a, (b, c)) = f()<!>
    <!TYPE_MISMATCH!>(a, (b, c), d) = f()<!>
    <!TYPE_MISMATCH!>(a, (b, c, d)) = f()<!>
    <!TYPE_MISMATCH!>(a, b) = f()<!>
}

main() {
}
