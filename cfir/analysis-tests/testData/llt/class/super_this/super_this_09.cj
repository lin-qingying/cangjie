

open class B {
    public var s = 1
    public func g() {}
}

class A <: B {
    public var ss = <!USED_BEFORE_INITIALIZATION!>s<!>
    public var sss = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.s
    public var ssss = super
    public var sssss = <!USED_BEFORE_INITIALIZATION!>g<!>()
    public var ssssss = <!USED_BEFORE_INITIALIZATION!>g<!>
    public var sssssss = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.g()
    public var ssssssss = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.g
    public var a: Int64
    public var b: Int64 = 1
    public var c = {
        =>
        <!USED_BEFORE_INITIALIZATION!>this.b<!>;
        <!USED_BEFORE_INITIALIZATION!>b<!>
    }
    public var d = <!USED_BEFORE_INITIALIZATION!>this.f<!>()
    public var e = <!USED_BEFORE_INITIALIZATION!>this.b<!>
    public var ff = {=> 1}
    public var gg = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>
    public var fff = {=> <!USED_BEFORE_INITIALIZATION!>this<!>}
    public init() {
        func m() {
            <!USED_BEFORE_INITIALIZATION!>this.b<!>
            <!USED_BEFORE_INITIALIZATION!>this<!>
            <!USED_BEFORE_INITIALIZATION!>this.f<!>()
            <!USED_BEFORE_INITIALIZATION!>b<!>
            <!USED_BEFORE_INITIALIZATION!>f<!>()
        }

        if (false) {
            a = 1
            this.f() // OK
        }
        <!USED_BEFORE_INITIALIZATION!>this.f<!>()
        <!USED_BEFORE_INITIALIZATION!>f<!>()
        <!USED_BEFORE_INITIALIZATION!>super.g<!>()
        <!USED_BEFORE_INITIALIZATION!>g<!>()
        <!USED_BEFORE_INITIALIZATION!>super.s<!>
        <!USED_BEFORE_INITIALIZATION!>s<!>
        <!USED_BEFORE_INITIALIZATION!>b<!>
        <!USED_BEFORE_INITIALIZATION!>this.b<!>
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
