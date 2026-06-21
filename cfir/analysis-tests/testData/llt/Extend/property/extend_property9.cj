

struct A {
    public mut prop b: Int64 {
        get() {
            1
        }
        set(v) {}
    }
}

extend A {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>b<!>() {}
}

main() {
    var x = A().b
    return 0
}
