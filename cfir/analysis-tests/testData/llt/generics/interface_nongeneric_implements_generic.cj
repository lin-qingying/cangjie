


interface I<T> {
    func i(a: T): Bool
}

interface I2<T> {
    func j(a: T): Bool
}

interface I3 <: I<Bool> & I2<Rune> {
    static func k(a: I<Bool>, b: I2<Rune>) {
        return a.i(true) && b.j(r'a')
    }
}

class Test <: I3 {
    public func i(a: Bool) {
        return false
    }
    public func j(a: Rune) {
        return true
    }
}

class C1 <: I<Bool> & I2<Rune> {
    public func i(a: Bool) {
        return false
    }
    public func j(a: Rune) {
        return true
    }
}

main() {
    if (Test.k(C1(), C1()) == false) {
        return 0
    }
    return 1
}
