


extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>public<!> operator func +(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>public<!> operator func <!RETURN_TYPE_INCOMPATIBLE!>==<!>(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>public<!> operator func <<(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>public<!> operator func -(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>public<!> operator func -() {
        return this
    }
    public operator func [](right: Int64) { // OK
        return this
    }
    public operator func +(right: Int32) { // OK
        return this + Int64(right)
    }
}

extend Bool {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>public<!> operator func <!EXTEND_MEMBER_CANNOT_SHADOW!>==<!>(right: Bool) {
        return true
    }
    public operator func +(right: Bool) { // OK
        return true
    }
}
