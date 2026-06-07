<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


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
    <!sema_member_variable_can_not_shadow!>p<!>ublic var i: Int32 = 1
    public init(a: Int32) {}
    public init(a: Test) {}
    public init() {
        this(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>t<!>his.i)
    }
    public init(a: Int32, b: Int32) {
        this(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>t<!>his.f())
    }
    public init(a: Int32, b: Int32, c: Int32) {
        this(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>i<!>)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32) {
        this(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>f<!>())
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32) {
        this(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>t<!>his)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>s<!>uper.i)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>s<!>uper.foo())
    }
    public <!sema_overload_conflicts!>init<!>(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>j<!>)
    }
    public <!sema_overload_conflicts!>init<!>(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(<!sema_assignment_of_member_variable_cannot_use_this_or_super!>f<!>oo())
    }
    public func f(): Int32 {
        return 0
    }
}
