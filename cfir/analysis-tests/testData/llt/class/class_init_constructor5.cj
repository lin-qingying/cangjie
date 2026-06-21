

open class A {
    public var a: Int64
    <!CLASS_UNINITIALIZED_FIELD!>public init(n: Int64)<!> {
        var i: Int64 = 0
        while (i < n) {
            if (i > 10) {
                this.a = n
                break;
            }
            i = i + 1
        }
    }
}
