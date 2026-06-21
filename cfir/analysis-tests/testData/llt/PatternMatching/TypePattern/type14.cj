

// Option box shouldn't work at pattern matching
main() {
    if (Some(1) is Int) {
        return 1
    }
    return 0
}
