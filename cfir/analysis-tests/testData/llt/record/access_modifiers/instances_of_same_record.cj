

struct C {
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

struct Date {
    private func test() {
        return 2
    }
    public func test1() {
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

main(): Int64 {
    if (C().f() == 2 && C().f1() == 3 && Date().f2() == 2 && Date().f3() == 2) {
        return 0
    }
    return 1
}
