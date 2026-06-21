

// case 1
class A {}

extend A {
    private func h() {}
}

extend A {
    private func h() {} // OK
}
// case 2
class B {
    private func h() {}
}

extend B {
    private func h() {} // OK
}

// case 3
class C {
    private func f() {}
    private func <!CONFLICTING_OVERLOADS!>f<!>() {} // error
}

// case 4
class D {
    func f() {}
}

extend D {
    private func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>() {} // error
}
// case 5
class E {
    private func h() {}
}

extend E {
    func h() {} // ok
}

// case 6
class F {}

extend F {
    private func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>() {} //error
}

extend F {
    func f() {} // ok
}

// case 4
class I {}

extend I {
    func f() {} // ok
}

extend I {
    private func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>() {} // error
}

main() {
    0
}
