// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj hide_use1.cj


import ohos.dep.*
import ohos.dep

class D<T> <: <!UNRESOLVED_REFERENCE!>A<!> where T <: <!UNRESOLVED_REFERENCE!>A<!> {
    func xxx<R>(d: <!UNRESOLVED_REFERENCE!>A<!>): <!UNRESOLVED_REFERENCE!>A<!> where R <: <!UNRESOLVED_REFERENCE!>A<!> {
        var a: <!UNRESOLVED_REFERENCE!>A<!> = <!UNRESOLVED_REFERENCE!>A()<!>
        <!UNRESOLVED_REFERENCE!>B.AA<!>
        <!UNRESOLVED_REFERENCE!>B.BB(1)<!>
        <!UNRESOLVED_REFERENCE!>E.AA<!>
        <!UNRESOLVED_REFERENCE!>E.BB(1)<!>
        <!UNRESOLVED_REFERENCE!>goo(1)<!>
        var b: D<<!UNRESOLVED_REFERENCE!>A<!>>
        var c: <!UNRESOLVED_REFERENCE!>dep.A<!>
        a is <!UNRESOLVED_REFERENCE!>A<!>
        a as <!UNRESOLVED_REFERENCE!>A<!>
        <!UNRESOLVED_REFERENCE!>aaa<!> // hiden global var
        a
    }
}

main() {}
