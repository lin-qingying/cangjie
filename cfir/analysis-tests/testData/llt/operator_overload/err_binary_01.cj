


enum E {
    A
}

main() {
    let _: E = <!NO_MATCHING_OPERATOR_INVOKE!>A<!>(A) + A
}
