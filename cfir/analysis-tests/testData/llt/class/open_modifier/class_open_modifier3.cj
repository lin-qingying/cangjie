


class A {
    <!IGNORE_OPEN!>p<!>ublic open func f(): Int32 {
        return 0
    }
    public init() {
    }
}

class B <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>A<!> {
    public init() {
        f()
        var a = {=> super.f}
        func f3(c!: Int32 = f()) {
            f
        }
        a()
    }
    <!IGNORE_OPEN!>p<!>ublic open override func f(): Int32 {
        return 3
    }
}

class C <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>B<!> {
    public init() {
        super.f()
        var a = {=> f}
        func f3(c!: Int32 = <!TYPE_MISMATCH!>f<!>) {
            this.f
        }
        a()
    }
}

main(): Int64 {
    0
}
