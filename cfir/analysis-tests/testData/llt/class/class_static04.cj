


class A {
    private static func f() {
        0
    }
    static func f(_: Int64) {
        1
    }
}

main() {
    let f: () -> Int64 = A.<!INVISIBLE_MEMBER!>f<!>
    f()
}
