

open class A {
    public open func abc() {}
}

class B <: A {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>abc<!>() {}
}

open class D {
    public open func f() {}
}

public open class E <: D {
    public open override func f() {}
}

class F <: E {
    private override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}
