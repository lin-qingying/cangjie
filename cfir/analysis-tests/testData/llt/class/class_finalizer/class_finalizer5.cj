

class A {
    public var a: Int32 = 3
    ~init() {}
}

main() {
    if (A().a == 3) {
        return 0
    }
    return 1
}
