

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res1 = Base().Foo(a: 2)
    if (res1 == 4) {
        return 0
    }
    return 1
}

open class Base {
    public var x: Int64 = 2
    public func Foo(a!: Int64 = x): Int64 {
        return a * 2
    }
}
