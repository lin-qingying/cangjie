


main() {
    let a = Some(15)
    if (let Some(<!VAR_IN_OR_CONDITION!>b<!>) <- a || true) {
        return 0
    }

    if (false || let Some(<!VAR_IN_OR_CONDITION!>b<!>) <- a) {
        return b - 1
    }

    return 2
}
