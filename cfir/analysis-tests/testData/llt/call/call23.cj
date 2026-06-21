

open class I<T> {
    public func test(a: T) {
        return 1
    }
    public func test(a: Int32) {
        return 2
    }
}

main(): Int64 {
    var res1 = I<Int64>().test((1))
    if (res1 != 1) {
        return 1
    }
    return 0
}
