


package pkg

private func foo() {
    1
}

// main will not be source exported.
main() {
    foo()
}
