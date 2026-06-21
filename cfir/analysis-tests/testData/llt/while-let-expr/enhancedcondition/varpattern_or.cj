

main() {
    let a = Some(15)
    while (<!VAR_IN_OR_CONDITION!>let Some(b) <- a || true<!>) {
        return 0
    }

    while (<!VAR_IN_OR_CONDITION!>false || let Some(b) <- a<!>) {
        return b - 1
    }

    return 2
}
