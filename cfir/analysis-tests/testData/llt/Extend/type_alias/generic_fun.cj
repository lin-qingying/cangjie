


extend Int64 {
    public func f<T>(a: T) {
        a
    }
}

main() {
    1.f<Int64>(1) - 1
}
