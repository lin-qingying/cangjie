


interface Bar {
    func bar<T>(a: T): T
    func test<T>(v: T) {
        return v
    }
}

open class Cls<X> {
    public open func test<T>(v: T) {
        return v
    }
}

abstract class Cls2<X> {
    public func foo<T>(v: T): T
    public open func test<T>(v: T) {
        return v
    }
}

main(): Int64 {
    return 0
}
