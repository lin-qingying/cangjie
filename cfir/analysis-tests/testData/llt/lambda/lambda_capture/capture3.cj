

class A {
    public var a: Int64 = 1
    public init() {
        var b: Int64 = 4
        func f() {
            b
        }
        func g() {
            var a = {=> <!USE_FUNC_CAPTURE_VAR_ALONE!>f<!>}
            return 0
        }
    }
}
