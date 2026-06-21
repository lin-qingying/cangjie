

struct R {
    private func a() {
        var c: Int32 = 10
    }
}

main(): Int64 {
    var c: Int32 = 0
    let b = <!TYPE_MISMATCH!><!INVISIBLE_MEMBER!>R().a<!><!>
}
