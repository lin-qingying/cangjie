<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func mm() {
    <!THIS_SUPER_USE_ERROR_OUTSIDE_CLASS!>s<!>uper()
    <!THIS_SUPER_USE_ERROR_OUTSIDE_CLASS!>s<!>uper
}

open class c {
    var c: Int64 = 1
}

class m <: c {
    public var a = <!ILLEGAL_SUPER_ALONE!>s<!>uper // ok
    public var b = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper()
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var c: Int64 = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper()
    public static var d = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper()
    public static var e: Int64 = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper()
    public init() {
        super() // ok
    }
    public init(a: Int64) {
        <!ILLEGAL_SUPER_ALONE!>s<!>uper // ok
    }
    public init(a: Int64, b: Int64) {
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper().<!NOT_MEMBER_OF!>a<!>
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper().c
    }
    public init(a: Int64, b: Int64, c: Int64) {
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>s<!>uper().c++
    }
    public func f() {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper()
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper().c
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper().c++
    }
    public static func ff() {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper()
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper().c
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>s<!>uper().c++
    }
}
