


main(): Int64 {
    return run()
}

struct Data {
    public var width: Int32
    public var length: Int32

    public init(x: Int32, y: Int32) {
        width = x
        length = y
    }

    public operator func -(): Data {
        var left: Int32 = width
        <!TYPE_MISMATCH!>var right: Int32 = length<!>
    }

    public static func g() {
        var x = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>w<!>idth
        return x
    }
}

func run(): Int64 {
    var data: Data = Data(1, 2)
    var dd = -data
    return 0
}
