

interface T1 {
    func f() {}
}

interface T2 <: T1 {
    func f() {}
}

open class D <: T2 {
    public open override func f() {}
}

class E <: D {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}
