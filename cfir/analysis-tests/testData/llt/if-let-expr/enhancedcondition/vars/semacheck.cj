

main() {
    if (<!VAR_IN_OR_CONDITION!>let a1 | a2 | a3 <- 1 && let a4 | a5 | a6 <- 1 || let a7 | a8 | a9 <- 1<!>) {
        return 0
    }
    while (<!VAR_IN_OR_CONDITION!>let a1 | a2 | a3 <- 1 && let a4 | a5 | a6 <- 1 || let a7 | a8 | a9 <- 1<!>) {
        return 0
    }
    return 1
}
