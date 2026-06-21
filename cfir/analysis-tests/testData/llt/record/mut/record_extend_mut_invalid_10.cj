

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo(): Unit {
        i += 1
    }

    public func goo1() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>
    }

    public func goo2() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    }
}

interface I1 {
    mut func foo(): Unit {
    }

    func goo1() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>
    }

    func goo2() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    }
}

main() {
    return 1
}
