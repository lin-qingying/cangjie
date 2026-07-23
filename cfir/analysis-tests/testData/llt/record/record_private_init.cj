

struct A {
    private init() {}
    public init(b: Int64, c: Int64) {}
    public func m() {
        var a = A() // ok
        var c = A(1, 1) // ok
    }
    public static func f() {
        var a = A() // ok
        var c = A(1, 1) // ok
    }
}

struct B {
    public func m() {
        var a = A<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
        var c = A(1, 1) // OK
    }
    public static func f() {
        var a = A<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
        var c = A(1, 1) // OK
    }
}

main() {
    var a = A<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
    var c = A(1, 1) // OK
    return 0
}
