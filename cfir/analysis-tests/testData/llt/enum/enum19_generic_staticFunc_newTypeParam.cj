

enum ET<T> {
    A | B | C

    public static func f<U>(a: T, b: U) {
        return a
    }
}

main() {
    return ET<Int64>.f<Bool>(0, false)
}
