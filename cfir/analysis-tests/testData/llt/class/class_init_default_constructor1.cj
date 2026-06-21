


class A {
    public var a: Int64 = <!USED_BEFORE_INITIALIZATION!>c<!>
    public var b: Int64 = 1
}

let c: Int64 = 1
