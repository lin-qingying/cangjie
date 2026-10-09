


open class A {
    public func xxx(a: Int32) {
        0
    }
}

func xxx() {
    1
}

class Test <: A {
    public static func test() {
        return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>xxx<!>(10)
    }
}

main() {
    var a = Test.test()
    if (a != 0) {
        return 1
    }
    return 0
}
