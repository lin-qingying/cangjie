


class A {
    public func toString() {
        "1"
    }
}

struct B {
    public func toString() {
        "1"
    }
}

class C <: ToString {
    public func toString() {
        "1"
    }
}

struct D <: ToString {
    public func toString() {
        "1"
    }
}

main() {
    print("$<!INVALID_STRING_IMPLEMENTATION!>{<!>A()}")
    print("$<!INVALID_STRING_IMPLEMENTATION!>{<!>B()}")
    print("${C()}")
    print("${D()}")
    return 0
}
