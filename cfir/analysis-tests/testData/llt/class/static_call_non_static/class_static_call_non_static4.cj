


class Base {
    public func f(): Int32 {
        return 1
    }
    public static func get(): Int32 {
        func a(): Int32 {
            return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>()
        }
        return a()
    }
}
