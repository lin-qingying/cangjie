

class A {
    public init() {
        this
    }
    public func get(a: A) {
        1
    }
    public init(a: Int64) {
        get(<!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>)
    }
    public init(a: Bool) {
        this(<!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>)
    }
    public init(a: A) {}
}
