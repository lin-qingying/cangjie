


//brief:导入泛型类型后继承

internal import p1.A
internal import p1.B as Boo

main(): Int64 {
    return 0
}

class C<T> <: A<T> {
    public var a: T
    public init(x: T) {
        a = x
    }
}

class D<T> <: Boo<T> {
    public init(x: T) {
        super(x)
    }
}
