<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class Data {
    public Data(let a!: Bool = <!sema_assignment_of_member_variable_cannot_use_this_or_super, sema_capture_has_shadow_variable!>d<!>u, var du!: Bool = a) {}
}

class A {
    public let a: Bool
    public var du: Bool
    public init(a!: Bool = <!sema_assignment_of_member_variable_cannot_use_this_or_super, sema_capture_has_shadow_variable!>d<!>u, du!: Bool = a) {
        this.a = a
        this.du = du
    }
}

class Date {
    public var v: Int32 = 1
    public Date(let width!: Int32 = 1, let length!: Int32 = <!sema_assignment_of_member_variable_cannot_use_this_or_super!>v<!>,
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
            var res: Int32 = <!sema_assignment_of_member_variable_cannot_use_this_or_super!>v<!>;
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
    public Date3(let width!: Int32 = 1, <!sema_member_variable_can_not_shadow!>l<!>et length!: Int32 = <!sema_assignment_of_member_variable_cannot_use_this_or_super!>v<!>,
        <!sema_member_variable_can_not_shadow!>l<!>et perimeter!: () -> Int32 = {
            =>
            var res: Int32 = <!sema_assignment_of_member_variable_cannot_use_this_or_super!>v<!>;
            return res
        }) {}
}

class B {
    public B(let ai!: Bool = true && <!sema_assignment_of_member_variable_cannot_use_this_or_super, sema_capture_has_shadow_variable!>x<!>a, var xa!: Bool = ai) {}
}

class C {
    public C(var xa!: Bool = true, let ai!: Bool = xa && true, var c!: Int32 = 3, var x!: Int32 = 2 + Date2().v + <!sema_assignment_of_member_variable_cannot_use_this_or_super!>t<!>his.c) {}
}
