


open class Base<T> {
    public var i: Int32 = 2
    public var j: Int32 = 1
    public func foo(): Int32 {
        return 3
    }
    public init() {}
    public init(a: Int32) {}
}

class Test <: Base<Int32> {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var i: Int32 = 1
    public init(a: Int32) {}
    public init(a: Test) {}
    public init() {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>this<!>.i)
    }
    public init(a: Int32, b: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>this<!>.f())
    }
    public init(a: Int32, b: Int32, c: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>i<!>)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>f<!>())
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>this<!>)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>super<!>.i)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>super<!>.foo())
    }
    public <!CONFLICTING_OVERLOADS!>init<!>(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>j<!>)
    }
    public <!CONFLICTING_OVERLOADS!>init<!>(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>f<!>oo())
    }
    public func f(): Int32 {
        return 0
    }
}
