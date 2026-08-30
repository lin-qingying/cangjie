

open class B {
    public var s = 1
    public func g() {}
}

class A <: B {
    public var ss = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>s<!>
    public var sss = <!THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_NON_STATIC_MEMBER!>super<!>.s
    public var ssss = <!ILLEGAL_SUPER_ALONE!>super<!>
    public var sssss = <!ILLEGAL_USAGE_OF_MEMBER!>g<!>()
    public var ssssss = <!ILLEGAL_USAGE_OF_MEMBER!>g<!>
    public var sssssss = <!THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_NON_STATIC_MEMBER!>super<!>.g()
    public var ssssssss = <!THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_NON_STATIC_MEMBER!>super<!>.g
    public var a: Int64
    public var b: Int64 = 1
    public var c = {
        =>
        this.<!ILLEGAL_USAGE_OF_MEMBER!>b<!>;
        <!ILLEGAL_USAGE_OF_MEMBER!>b<!>
    }
    public var d = this.<!ILLEGAL_USAGE_OF_MEMBER!>f<!>()
    public var e = this.b
    public var ff = {=> 1}
    public var gg = <!THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_NON_STATIC_MEMBER!>this<!>
    public var fff = {=> this}
    public init() {
        func m() {
            this.<!ILLEGAL_USAGE_OF_MEMBER!>b<!>
            <!ILLEGAL_USAGE_OF_MEMBER!>this<!>
            this.<!ILLEGAL_USAGE_OF_MEMBER!>f<!>()
            <!ILLEGAL_USAGE_OF_MEMBER!>b<!>
            <!ILLEGAL_USAGE_OF_MEMBER!>f<!>()
        }

        if (false) {
            a = 1
            this.f() // OK
        }
        this.<!ILLEGAL_USAGE_OF_MEMBER!>f<!>()
        <!ILLEGAL_USAGE_OF_MEMBER!>f<!>()
        super.<!ILLEGAL_USAGE_OF_MEMBER!>g<!>()
        <!ILLEGAL_USAGE_OF_MEMBER!>g<!>()
        super.s
        s
        b
        this.b
        a = 1
        this.f()
        f()
        super.g()
        func mm() {
            this.b
            this
            this.f()
            b
            f()
        }
    }
    public func f() {}
}
