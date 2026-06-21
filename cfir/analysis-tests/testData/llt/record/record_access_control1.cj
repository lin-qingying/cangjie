

struct R {
    private var x: Int64 = 2
    public func a() {
        var c: Int32 = 10
        return A()
    }
    private func b() {
        return R().x
    }
}

struct A {
    private func a() {
        var c: Int32 = 10
    }
}

main(): Int64 {
    R().a().<!INVISIBLE_MEMBER!>a<!>
    R().<!INVISIBLE_MEMBER!>b<!>

    return 0
}
