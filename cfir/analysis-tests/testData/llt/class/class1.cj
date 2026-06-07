<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public var a: Int64 = 1
}

class B <: <!sema_non_inheritable_super_class!>A<!> {
    public var b: Int64 = 2
    public func f() {
        var c: Int32 = <!sema_mismatched_types!>a<!>
        var d: Int32 = <!sema_mismatched_types!>b<!>
    }
}

<!sema_inheritance_cycle!>c<!>lass C <: <!sema_non_inheritable_super_class!>D<!> {} // regex inheritance cycle detected: 'D->C->D'

class <!RECURSIVE_CONSTRUCTOR_CALL!>D<!> <: <!sema_non_inheritable_super_class!>C<!> {}
