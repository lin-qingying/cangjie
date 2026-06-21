

struct A {
    var x = 0
}

struct B {
    var a: A
    init() {
        <!USED_BEFORE_INITIALIZATION!>a<!>.x = 1
        a = A()
    }
}

main() {
    println(B().a.x)
}
