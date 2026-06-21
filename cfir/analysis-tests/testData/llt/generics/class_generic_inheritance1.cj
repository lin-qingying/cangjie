

open class Fox<T> {
    public var fox: T
    public init(f: T) {
        fox = f
    }
}

open class Bar<T, U> <: Fox<T> {
    public var bar: T
    public init(b: T) {
        super(b)
        bar = b
    }
    public func bar1(): T {
        return this.fox
    }
}

class Foo<T> <: Bar<T, Int32> {
    public var foo: T
    public init(f: T) {
        super(f)
        foo = f
    }
    public func foo1(): T {
        return this.bar
    }
}

main(): Int64 {
    var c: Foo<Int32> = Foo<Int32>(0)
    c.bar = 1
    print(c.fox.toString())
    c.bar1();
    return 0;
}
