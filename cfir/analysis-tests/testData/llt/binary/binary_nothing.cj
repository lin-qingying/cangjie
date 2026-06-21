

// DIAGNOSTICS: -warnings

var a = 0

open class C1 {
    public open func f(): Bool {
        a = 3
        return true
    }
}

class C2 <: C1 {
    public override func f(): Bool {
        super.f()
    }
}

main(): Int64 {
    if (C2().f()) {
        return (a - 3)
    } else {
        return 2
    }
        % <!UNRESOLVED_REFERENCE!>n<!>
}
