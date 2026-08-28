

struct S {
    mut func f() {}
}

class A {
    let s = S()
}

main() {
    let a = A()
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>((((a)))).s<!>.f()
}
