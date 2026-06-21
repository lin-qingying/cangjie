

let <!UNABLE_TO_INFER_DECL!>(f, x)<!> = ({=> g}, 1)

func g() {
    <!USED_BEFORE_INITIALIZATION!>f<!>
}

main() {}
