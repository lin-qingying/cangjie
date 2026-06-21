

interface T {
    func f() {}
}

interface T1 <: T {
    func f() {}
}

abstract class B <: T {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}

open class C <: T {
    public open override func f() {}
}

public class D <: C {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {}
}
