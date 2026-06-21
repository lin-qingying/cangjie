

interface I<T> {
    func i(a: T): Bool
}

open class Base<U> {
    public open func base(): Int64 {
        return 0
    }
}

class A <: Base<Bool> & I<Bool> {
    public func i(a: Bool): Bool {
        return false
    }
    public override func base(): Int64 {
        return 1
    }
}

main() {
    var obj: A = A()
    if (obj.i(true) == false && obj.base() == 1) {
        return 0
    }
    return 1
}
