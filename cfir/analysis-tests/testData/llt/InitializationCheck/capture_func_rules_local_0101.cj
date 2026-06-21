

main(): Int64 {
    return 0
}

var a = {
    =>
    var a: Int32
    let b: Int32
    func foo1(x!: Int32 = <!CAPTURE_BEFORE_INITIALIZATION!>a<!> + b) {
        return x
    }
    func foo2() {
        var x = <!CAPTURE_BEFORE_INITIALIZATION!>a<!> + b;
        return x
    }
}
