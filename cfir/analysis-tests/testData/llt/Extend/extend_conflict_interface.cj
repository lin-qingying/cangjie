

class A {
    static var a: Int32 = 5
}

extend A {
    public static func <!STATIC_FUNCTION_OVERLOAD_CONFLICTS!>me<!>() {
        return 0
    }

    static func <!STATIC_FUNCTION_OVERLOAD_CONFLICTS!>me<!>(): Int64 {
        return Int64(a)
    }

    func me(): Int32 {
        return 0
    }
}

main(): Int64 {
    return 0
}
