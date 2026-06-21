

package nn

public struct A<T> {
    public var a: B<T> = B<T>()
    public func abc() {
        return 1
    }
}

public struct B<T> {
    public var b: Int64 = 1
    public func abc() {
        return 1
    }
}

func foo() {
    A<Float64>()
}
