


class A {
    private static func f() {
        0
    }
    static func f(_: Int64) {
        1
    }
}

class B {
    private func f() {
        0
    }
    func f(_: Int64) {
        1
    }
}

main() {
    let f: () -> Int64 = A.<!INVISIBLE_MEMBER!>f<!>
    let b = B()
    let memberF: () -> Int64 = b.<!INVISIBLE_MEMBER!>f<!>
    f()
    memberF()
}
