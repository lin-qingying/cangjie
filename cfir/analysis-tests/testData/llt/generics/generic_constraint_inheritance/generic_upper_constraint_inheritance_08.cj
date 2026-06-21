


open class C {
    public open func foo(): Int {
        1
    }
}

interface I {
    func foo(): String
}

func bar<T>(x: T) where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T <: C & I<!> {
    return x.foo()
}

class D <: C & I {
    public func foo(): Nothing {
        throw Exception()
    }
}

main() {
    try {
        bar<D>(D())
    } catch (_) {
        return 0
    }
    return 1
}
