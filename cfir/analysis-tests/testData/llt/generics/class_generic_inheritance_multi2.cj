

open class A<T> {
    public var a: T
    public init(n: T) {
        a = n
    }
}

class B<T> <: A<T> {
    public init(a: T) {
        super(a)
    }
}

main(): Int64 {
    var b = B<Int32>(1);
    return 0;
}
