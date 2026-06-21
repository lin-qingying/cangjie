

main() {
    let a = Some(15)
    while (<!VAR_IN_OR_CONDITION!>false || let Some(b) <- a || let Some(c) <- a<!>) {
        return b - 1
    }

    return 2
}

enum A {
    B | C(Int32) | D(Int64)
}

func foo(a: A) {
    while (<!VAR_IN_OR_CONDITION!>let B | C(v) | D(d) <- a || false<!>) {
        1
    }
}
