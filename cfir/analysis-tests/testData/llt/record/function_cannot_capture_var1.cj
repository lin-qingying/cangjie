


struct A {
    public var a: Int64 = 1
    public static var c: Int64 = 2

    public var b: () -> Int64 = {=> <!ILLEGAL_USAGE_OF_MEMBER!>a<!>}
    public func f() {
        var d: Int64 = 1
        a
        b()
        var c: () -> Int64 = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> d}<!>
        func ff() {
            var b: Int64 = a
        }
        return a
    }
    public static func f1() {
        c
    }
}

main(): Int64 {
    return 0
}
