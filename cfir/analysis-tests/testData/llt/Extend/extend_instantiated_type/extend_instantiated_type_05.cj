

class A<T> {}

extend A<Int64> {
    func foo() {
        11
    }
}

main() {
    if (A<Int64>().foo() != 11) {
        return 1
    }
    return 0
}
