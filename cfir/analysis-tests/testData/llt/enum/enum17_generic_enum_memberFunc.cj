

enum ET<T> {
    A | B | C

    public func f(a: T) {
        return a
    }
}

main() {
    let a = ET<Int64>.A
    return a.f(0)
}
