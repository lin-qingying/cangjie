

struct R1 {
    public var i = 0

    public mut func foo(): Unit {
        i += 1
        hoo()
    }

    public func goo(): Unit {
        <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    }

    public func hoo(): Unit {
    }
}

interface I1 {
    mut func foo(): Unit {
        hoo()
    }

    func goo(): Unit {
        <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    }

    func hoo(): Unit {
    }
}

main() {
    return 1
}
