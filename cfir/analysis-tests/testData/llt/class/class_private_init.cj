


class A {
    private init() {}
    protected init(a: Int64) {}
    public init(b: Int64, c: Int64) {}
    public func m() {
        var a = A() // ok
        var b = A(1) // ok
        var c = A(1, 1) // ok
    }
    public static func p() {
        var a = A() // ok
        var b = A(1) // ok
        var c = A(1, 1) // ok
    }
}

class B {
    public func m() {
        var a = A<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
    }
}

class C <: <!NON_INHERITABLE_SUPER_CLASS!>A<!> {
    public init() {
        super<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
    }
    public init(a: Int64) {
        super(a)
    }
    public func k() {
        var a = A<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
        var b = A(1) // OK
        var c = A(1, 1) // OK
    }
    public static func j() {
        var a = A<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
        var b = A(1) // OK
        var c = A(1, 1) // OK
    }
}

main() {
    var a = A(1)
    var b = A<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
    var c = A(1, 1) // OK
    return 0
}
