


main() {
    let a = Some(15)
    if (false || let Some(<!VAR_IN_OR_CONDITION!>b<!>) <- a || let Some(c) <- a) {
        return b - 1
    }

    return 2
}

enum A {
    B | C(Int32) | D(Int64)
}

func foo(a: A) {
    if (let B | C(<!VAR_IN_OR_CONDITION!>v<!>) | D(d) <- a || false) {
        1
    }
}
