// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public var a: Int64 = 1
}

class B <: <!NON_INHERITABLE_SUPER_CLASS!>A<!> {
    public var b: Int64 = 2
    public func f() {
        var c: Int32 = <!TYPE_MISMATCH!>a<!>
        var d: Int32 = <!TYPE_MISMATCH!>b<!>
    }
}

<!INHERITANCE_CYCLE!>c<!>lass C <: <!NON_INHERITABLE_SUPER_CLASS!>D<!> {}

class <!RECURSIVE_CONSTRUCTOR_CALL!>D<!> <: <!NON_INHERITABLE_SUPER_CLASS!>C<!> {}
