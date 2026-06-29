

class A {
    public init() {
        this
    }
    public func get(a: A) {
        1
    }
    public init(a: Int64) {
        get(this)
    }
    public init(a: Bool) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>this<!>)
    }
    public init(a: A) {}
}
