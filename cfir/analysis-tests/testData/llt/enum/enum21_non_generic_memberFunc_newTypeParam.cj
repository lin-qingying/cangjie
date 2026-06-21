

enum ET {
    A | B | C

    public func f<U>(a: U) {
        return a
    }
}

main() {
    let a = ET.A
    return a.f<Int64>(0)
}
