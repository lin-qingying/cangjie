

class C {
    public func f() {
        1
    }
    private func f(a: Int64) {
        2
    }
}

extend C {
    private func f(a: Int64) {
        3
    }
    func g1() {
        f()
    }
    func g2() {
        f(1)
    }
}

main() {
    var c = C()
    if (c.g1() != 1) {
        return 1
    }
    if (c.g2() != 3) {
        return 2
    }
    return 0
}
