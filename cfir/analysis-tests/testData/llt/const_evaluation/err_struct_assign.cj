

struct Foo {
    let a = 0
    var b = 0
    static const c = 0
    const init() {}
}

func f() {
    let v = Foo()
    v.<!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 1
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>v.b++<!>
    Foo.<!CANNOT_ASSIGN_TO_IMMUTABLE!>c<!> = 1
}

main() {}
