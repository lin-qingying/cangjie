

class C {
    private static var zi: Int64 = 2
    private static func test() {
        return 3
    }
    public func f() {
        return C.zi
    }
    public func f1() {
        return C.test()
    }
}

open class Date {
    private func test() {
        return 2
    }
    protected func test1() {
        return 3
    }
    public func f2() {
        return Date().test()
    }
    public func f3() {
        return f().test()
    }
}

func f() {
    return Date()
}

class A <: Date {
    public func f4() {
        return f().test1()
    }
    public func f5() {
        return super.test1()
    }
    public func f6() {
        return Date().test1()
    }
}

main(): Int64 {
    if (C().f() == 2 && C().f1() == 3 && Date().f2() == 2 && Date().f3() == 2 && A().f4() == 3 && A().f5() == 3 &&
        A().f6() == 3) {
        return 0
    }
    return 1
}
