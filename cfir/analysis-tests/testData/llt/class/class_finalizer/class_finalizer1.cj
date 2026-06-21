


class A {
    ~init() {}
}

class B {
    ~init() {}
}

class C {
    ~init<!CANNOT_CURRYING!>(<!>)() {}
}

class D {
    ~init() {}
    <!CONFLICTING_OVERLOADS!>~init<!>() {}
}

open class E {
    <!FINALIZER_FORBIDDEN_IN_CLASS!>~<!>init() {}
}

open class F {
    <!FINALIZER_FORBIDDEN_IN_CLASS!>~<!>init() {}
    public open func f() {
        0
    }
}

open class Parent {
    public open func f() {
        0
    }
}

class Children {
    ~init() {}
}

main() {
    0
}
