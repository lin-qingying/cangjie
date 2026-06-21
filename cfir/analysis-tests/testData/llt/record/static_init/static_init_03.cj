


struct A {
    var a: Int64 = 1
    static var b: Int64 = 1
    static init() {
        println(<!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!>)
    }
}

main() {
    return 0
}
