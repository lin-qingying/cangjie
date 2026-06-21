


struct R {
    public func add(): Int64 {
        return 1
    }
}

extend R {
    public func foo(): Int64 {
        add()
    }
}

main() {
    let a = R();
    return a.foo() - 1
}
