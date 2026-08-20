

open class A {
    public var a: Int64
    public <!CLASS_UNINITIALIZED_FIELD!>init<!>(n: Int64) {
        var i: Int64 = 0
        while (i < n) {
            this.a = this.<!USED_BEFORE_INITIALIZATION!>a<!> + n
            i = i + 1
        }
    }
}
