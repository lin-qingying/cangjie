

open class A {
    public func xxx() {
        0
    }
}

func xxx(a: Int32) {
    1
}

class Test <: A {
    public static func test() {
        return xxx(10)
    }
}

main() {
    var a = Test.test()
    if (a != 1) {
        return 1
    }
    return 0
}
