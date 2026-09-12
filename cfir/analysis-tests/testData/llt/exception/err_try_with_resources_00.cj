

main() {
    try (<!TYPE_MISMATCH!>x = return<!>) {}
}
