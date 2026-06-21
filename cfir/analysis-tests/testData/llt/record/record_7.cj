


struct Base12 {
    public var x: Int64 = 1
    public static prop a: Int64 {
        get() {
            return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>x<!>
        }
    }
}

main() {
    var x = Base12()
    return 0
}
