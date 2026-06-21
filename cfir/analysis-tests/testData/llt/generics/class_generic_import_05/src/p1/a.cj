

package p1

public open class A<T> {}

public open class B<T> {
    public var b: T
    public init(x: T) {
        b = x
    }
}
