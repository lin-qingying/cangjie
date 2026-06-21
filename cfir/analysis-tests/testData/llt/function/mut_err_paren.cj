

struct S {
    mut func f() {}
}

class A {
    let s = S()
}

main() {
    let a = A()
    ((((a)))).s.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
}
