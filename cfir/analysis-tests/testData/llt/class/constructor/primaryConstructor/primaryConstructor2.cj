// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class B<X, Y> {
    public B(x: Int64, y: X, private var z: Y) {}
    public init(z: Y) {
        this.z = z
    }
}

class C {
    private C(var z!: Int64 = 3) {}
}

class F {
    public F(var z!: Int64 = 3) {}
}

class G {
    public G(var z!: Int64 = 3) {}
}

class H {
    public H(var z!: Int64 = 3) {}
    <!MULTIPLE_PRIMARY_CONSTRUCTORS!>p<!>ublic H(var x!: String = "3") {}
}

class J {
    public J(z: Int64, var <!REDECLARATION!>z<!>!: Int64 = 1) {}
}

class K {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic K(var k: Int64, var <!REDECLARATION!>k<!>!: Int64 = 1) {}
}

open class D {
    protected D(var z!: Int64 = 3) {}
}

class E <: D {
    public E(<!MEMBER_VARIABLE_CAN_NOT_SHADOW!>v<!>ar z!: Int64 = 3) {
        var c: Int64 = 2
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper()
    }
}

class L <: D {
    public L(<!MEMBER_VARIABLE_CAN_NOT_SHADOW!>v<!>ar z!: Int64 = 3) {
        <!ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR!>t<!>his(<!NEED_NAMED_ARGUMENT!>"ww"<!>)
    }
    public init(a!: String = "1") {
        <!TYPE_INCOMPATIBLE!>t<!>his.z = <!TYPE_MISMATCH!>"a"<!>
    }
}

class N <: B<Int64, Int64> {
    public <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>N<!>(c: Int64) {}
}

main(): Int64 {
    <!INVALID_ACCESS_CONTROL!>B<!><Int64, Int64>(4).z
    <!NO_CONSTRUCTOR!>C<!>()
    D()
    E()
    F()
    return 0
}
