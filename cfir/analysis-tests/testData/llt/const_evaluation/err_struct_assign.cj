

struct Foo {
    let a = 0
    var b = 0
    static const c = 0
    const init() {}
}

func f() {
    let v = Foo()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>v.a = 1<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>v.b++<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>Foo.c = 1<!>
}

main() {}
