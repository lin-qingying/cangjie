

open class A {
    public var a: Int64
    public init() {
        if (<!USED_BEFORE_INITIALIZATION!>a<!> > 0) {
            a = 8
        } else {
            a = 4
        }
    }
}
