

main() {
    var a: Float64 = 0.0
    <!TYPE_MISMATCH!>a++<!>
    <!TYPE_MISMATCH!>a--<!>
}
