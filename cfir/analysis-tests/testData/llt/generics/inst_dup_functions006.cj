

open class A<T, U> {
    public func test(a: Int64) {
        return 1
    }
    public func test(a: T) {
        return 2
    }
}

class B<T> <: A<T, Bool> {
    public var a: T
    public init(input: T) {
        a = input
    }

    public func test(a: Int32) {
        return 3
    }
    public func i() {
        var j = this.test(a)
        return j
    }
}

main(): Int64 {
    var res1 = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<Int32><!>(1).i()
    var res2 = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<Int64><!>(1).i()
    return 0
}
