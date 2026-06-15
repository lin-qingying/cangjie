<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
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
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var i: Int32 = 1
    public init(a: Int32) {}
    public init(a: Test) {}
    public init() {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>t<!>his.i)
    }
    public init(a: Int32, b: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>t<!>his.f())
    }
    public init(a: Int32, b: Int32, c: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>i<!>)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>f<!>())
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32) {
        this(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>t<!>his)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>s<!>uper.i)
    }
    public init(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>s<!>uper.foo())
    }
    public <!OVERLOAD_CONFLICTS!>init<!>(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>j<!>)
    }
    public <!OVERLOAD_CONFLICTS!>init<!>(a: Int32, b: Int32, c: Int32, d: Int32, e: Int32, f: Int32, g: Int32) {
        super(<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>f<!>oo())
    }
    public func f(): Int32 {
        return 0
    }
}
