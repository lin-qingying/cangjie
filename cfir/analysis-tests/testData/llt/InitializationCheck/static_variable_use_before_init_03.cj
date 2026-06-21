

class A {
    static var a: Int64
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>b<!>: Int64
    static init() {
        a = 1
    }
}

main() {
}
