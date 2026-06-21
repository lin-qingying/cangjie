


class Data {
    public Data(let a!: Bool = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER, CAPTURE_HAS_SHADOW_VARIABLE!>d<!>u, var du!: Bool = a) {}
}

class A {
    public let a: Bool
    public var du: Bool
    public init(a!: Bool = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER, CAPTURE_HAS_SHADOW_VARIABLE!>d<!>u, du!: Bool = a) {
        this.a = a
        this.du = du
    }
}

class Date {
    public var v: Int32 = 1
    public Date(let width!: Int32 = 1, let length!: Int32 = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>v<!>,
        let perimeter!: () -> Int32 = {
            =>
            var res: Int32 = 1;
            return res
        }) {}
}

class Date1 {
    public var v: Int32 = 1
    public Date1(let width!: Int32 = 1, let length!: Int32 = 2,
        let perimeter!: () -> Int32 = {
            =>
            var res: Int32 = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>v<!>;
            return res
        }) {}
}

open class Date2 {
    public Date2(let v!: Int32 = 1, let length!: Int32 = 2,
        let perimeter!: () -> Int32 = {
            =>
            var res: Int32 = 1;
            return res
        }) {}
}

class Date3 <: Date2 {
    public Date3(let width!: Int32 = 1, <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>l<!>et length!: Int32 = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>v<!>,
        <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>l<!>et perimeter!: () -> Int32 = {
            =>
            var res: Int32 = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>v<!>;
            return res
        }) {}
}

class B {
    public B(let ai!: Bool = true && <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER, CAPTURE_HAS_SHADOW_VARIABLE!>x<!>a, var xa!: Bool = ai) {}
}

class C {
    public C(var xa!: Bool = true, let ai!: Bool = xa && true, var c!: Int32 = 3, var x!: Int32 = 2 + Date2().v + <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>t<!>his.c) {}
}
