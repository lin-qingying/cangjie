

class C {
    let <!UNABLE_TO_INFER_DECL!>v<!> = f
}

func f() {
    let c = C()
    c.v
}

main() {}
