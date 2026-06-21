

enum ET {
    A | B | C

    public static func f<U>(a: U) {
        return a
    }
}

main() {
    return ET.f<Int64>(0)
}
