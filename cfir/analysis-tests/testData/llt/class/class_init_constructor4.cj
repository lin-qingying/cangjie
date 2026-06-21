

open class A {
    public var a: Int64
    <!CLASS_UNINITIALIZED_FIELD!>public init(n: Int64)<!> {
        var i: Int64 = 0
        while (i < n) {
            this.a = <!USED_BEFORE_INITIALIZATION!>this.a<!> + n
            i = i + 1
        }
    }
}
