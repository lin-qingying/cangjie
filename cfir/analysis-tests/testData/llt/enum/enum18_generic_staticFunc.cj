

enum ET<T> {
    A | B | C

    public static func f(a: T) {
        return a
    }
}

main() {
    return ET<Int64>.f(0)
}
