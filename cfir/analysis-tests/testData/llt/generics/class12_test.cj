

func run(): Int64 {
    var res: Int32 = Foo().opt.t
    if (res == 1) {
        return 0
    }
    return 1
}

class Data<T> {
    public var t: T
    public init(x: T) {
        t = x
    }
}

class Foo {
    public var opt: Data<Int32> = Data<Int32>(1)
}

main(): Int64 {
    return run()
}
