


class Cls<X> {
    public func test(b: X) {
        return b
    }
    public func test<T>(v: T) {
        return v
    }
    public func <!CONFLICTING_OVERLOADS!>test<!><T>(v: X) {
    }
}

main(): Int64 {
    let cls = Cls<Int64>()
    return 0
}
