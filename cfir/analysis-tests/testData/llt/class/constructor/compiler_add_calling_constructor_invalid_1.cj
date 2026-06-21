

open class C1 {
    private init() {
    }
}

class C2 <: C1 {
    <!EXPLICIT_SUPER_CALL_REQUIRED!>public init()<!> {
    }
}

main() {
    return 0
}
