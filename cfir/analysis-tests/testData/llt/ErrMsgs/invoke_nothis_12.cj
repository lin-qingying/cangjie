

var aa = 0

main(): Int64 {
    if (C().b is Data && C.data.a == 8 && aa == 1) {
        return 0
    }
    return 1
}

class Data {
    public Data(var a!: Int64 = 6) {
        aa++
    }

    public func f2() {
        this
    }

    public func f1(i: Int64): This {
        f2()
    }
}

class C {
    public static var data = Data(a: 8)
    public var b = {
        => data.f2(<!TOO_MANY_ARGUMENTS!>3<!>)
    }()
}
