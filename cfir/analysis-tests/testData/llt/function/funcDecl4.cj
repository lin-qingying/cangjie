

class A {}

// Allow function return type to be inferred as Any
func test() {
    if (true) {
        return 1
    }
    return A()
}

main() {
    if (test() is Any) {
        return 0
    }
    return 1
}
