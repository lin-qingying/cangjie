

struct R {
    private func a() {
        var c: Int32 = 10
    }
}

main(): Int64 {
    var c: Int32 = 0
    <!TYPE_MISMATCH!>let b = R().<!INVISIBLE_MEMBER!>a<!><!>
}
