


main(): Int64 {
    return run()
}

class Data {
    var width: Int32
    var length: Int32

    init(x: Int32, y: Int32) {
        width = x
        length = y
    }

    func getWidth() {
        return width
    }

    func getLength() {
        return length
    }

    operator func -(operand: Data): Data {
        var left: Int32 = getWidth() - 1
        <!TYPE_MISMATCH!>var right: Int32 = getLength() - 1<!>
    }

    static func g() {
        var x = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>w<!>idth
        return x
    }
}

func run(): Int64 {
    var data: Data = Data(1, 2)
    var dd = <!INVALID_UNARY_EXPR!>-data<!>
    return 0
}
