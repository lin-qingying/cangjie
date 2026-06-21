


open class Base<U> {
    public var a: <!UNDECLARED_TYPE_NAME!>T<!>
    public init(x: <!UNDECLARED_TYPE_NAME!>T<!>) {
        a = x
    }
}

class Data<T> <: Base<Int32> {
    public var f: T
    public init(input: T) {
        <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>s<!>uper(0)
        f = input
    }
}

func run(): Int64 {
    var i1: Int64 = 4
    var data: Data<Int64> = Data<Int64>(i1)
    var res: Int64 = data.f
    if (res == 4) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}
