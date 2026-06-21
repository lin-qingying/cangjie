

main(): Int64 {
    Boo()
    return 0
}

struct Boo {
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(input) {
            a = input + 1
        }
    }
    public prop n: Int32 {
        get() {
            return a
        }
    }
    public var a: Int32 = 1

    public func f() {
        return b
    }
    public var b = print("1")
    public func f2() {
        return b
    }
}
