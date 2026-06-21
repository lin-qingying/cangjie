

struct A15 {
    public func foo(a: Float64) {
        return a
    }
}

extend A15 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(a: Int64) {
        return a
    }
}

extend A15 {
    public func foo(a: Int32) {
        return a
    }
}

extend A15 {
    public func foo(a: Rune) {
        return a
    }
}

extend A15 {
    public func foo(a: Float32) {
        return a
    }
}

extend A15 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(a: Int64) {
        return a
    }
}

let i = A15()
let i2 = <!AMBIGUOUS_USE!>i.foo<!> ~> i.foo
