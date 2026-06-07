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
    <!sema_multiple_primary_constructors!>p<!>ublic H(var x!: String = "3") {}
}

class J {
    public J(z: Int64, var <!sema_redefinition!>z<!>!: Int64 = 1) {}
}

class K {
    <!sema_class_uninitialized_field!>p<!>ublic K(var k: Int64, var <!sema_redefinition!>k<!>!: Int64 = 1) {}
}

open class D {
    protected D(var z!: Int64 = 3) {}
}

class E <: D {
    public E(<!sema_member_variable_can_not_shadow!>v<!>ar z!: Int64 = 3) {
        var c: Int64 = 2
        <!sema_illegal_place_of_calling_this_or_super!>s<!>uper()
    }
}

class L <: D {
    public L(<!sema_member_variable_can_not_shadow!>v<!>ar z!: Int64 = 3) {
        <!sema_illegal_place_of_calling_this_primary_constructor!>t<!>his(<!NEED_NAMED_ARGUMENT!>"ww"<!>)
    }
    public init(a!: String = "1") {
        <!sema_type_incompatible!>t<!>his.z = <!sema_mismatched_types!>"a"<!>
    }
}

class N <: B<Int64, Int64> {
    public <!sema_no_non_param_constructor_in_super_class!>N<!>(c: Int64) {}
}

main(): Int64 {
    <!sema_invalid_access_control!>B<!><Int64, Int64>(4).z
    <!sema_no_match_constructor!>C<!>()
    D()
    E()
    F()
    return 0
}
