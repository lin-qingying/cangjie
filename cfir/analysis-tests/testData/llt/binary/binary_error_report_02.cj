


open class Base<T> {
    public static var i: Int32 = 2
    public static func foo(): Int32 {
        return 3
    }
}

class Test <: Base<Int32> {
    public var i1: Int32
    public var i2: Int32
    public init() {
        i1 = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>super<!>.i
        i2 = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>super<!>.foo()
    }
}

class Data<T> <: Base<T> {
    public var i1: Int32
    public var i2: Int32
    public init() {
        i1 = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>super<!>.i + 1
        i2 = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>super<!>.foo() + 1
    }
}

func run(): Int64 {
    var test: Test = Test()
    var data: Data<Int32> = Data<Int32>()
    return 0
}

main(): Int64 {
    return run()
}
