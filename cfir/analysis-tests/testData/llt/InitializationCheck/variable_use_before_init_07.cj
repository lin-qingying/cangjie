

class cls {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64
    static init() {
        func f() {
            a = 1
        }
        f()
    }
}

main(): Int64 {
    var a: Int64
    func f() {
        <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = 1
    }
    f()
    println(<!USED_BEFORE_INITIALIZATION!>a<!>)

    let b: Int64
    {=> <!CAPTURE_BEFORE_INITIALIZATION!>b<!> = 2}
    println(<!USED_BEFORE_INITIALIZATION!>b<!>)
    return 1
}
