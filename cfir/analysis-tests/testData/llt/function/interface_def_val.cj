

interface A {
    static func a(): Unit {}
    static func i(input!: () -> Unit): Unit {
        input()
    }
    static func test(): Unit {
        i(input: a)
    }
}

class Test <: A {}

class B {
    public static var a = {=>}
    public static func i(input!: () -> Unit = a): Unit {
        input()
    }
    public static func test(): Unit {
        i()
    }
}

abstract class C {
    public static var a = {=>}
    public static func i(input!: () -> Unit = a): Unit {
        input()
    }
    public static func test(): Unit {
        i()
    }
}

main() {
    Test.a()
    Test.test()
    B.a()
    B.test()
    C.a()
    C.test()
    return 0
}
