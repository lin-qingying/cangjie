


struct Test {
    public let a: Int32 = 0
    public var b: Int32 = 0
    public static var b2: Int32 = 0
    public func foo(): Int32 {
        return a
    }

    public static func foo1(): Int32 {
        return 0
    }
}

var test = Test()

func a(): Int32 {
    return 5
}

class A {
    public func foo(): Int32 {
        return 1
    }

    public static func foo1(): Int32 {
        return 0
    }
}

var testA = A()

func run(): Int64 {
    test.b = 1
    test.<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo<!> = a
    Test.<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo1<!> = a
    testA.<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo<!> = a
    A.<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo1<!> = a
    return Int64(test.b + test.foo() - 6)
}

main(): Int64 {
    return run()
}
