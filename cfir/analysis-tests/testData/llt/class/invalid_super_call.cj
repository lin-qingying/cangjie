<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func mm() {
    <!sema_this_super_use_error_outside_class!>s<!>uper()
    <!sema_this_super_use_error_outside_class!>s<!>uper
}

open class c {
    var c: Int64 = 1
}

class m <: c {
    public var a = <!sema_illegal_super_alone!>s<!>uper // ok
    public var b = <!sema_invalid_this_call_outside_ctor!>s<!>uper()
    <!sema_member_variable_can_not_shadow!>p<!>ublic var c: Int64 = <!sema_invalid_this_call_outside_ctor!>s<!>uper()
    public static var d = <!sema_invalid_this_call_outside_ctor!>s<!>uper()
    public static var e: Int64 = <!sema_invalid_this_call_outside_ctor!>s<!>uper()
    public init() {
        super() // ok
    }
    public init(a: Int64) {
        <!sema_illegal_super_alone!>s<!>uper // ok
    }
    public init(a: Int64, b: Int64) {
        <!sema_illegal_place_of_calling_this_or_super!>s<!>uper().<!sema_not_member_of!>a<!>
        <!sema_illegal_place_of_calling_this_or_super!>s<!>uper().c
    }
    public init(a: Int64, b: Int64, c: Int64) {
        <!sema_illegal_place_of_calling_this_or_super!>s<!>uper().c++
    }
    public func f() {
        <!sema_invalid_this_call_outside_ctor!>s<!>uper()
        <!sema_invalid_this_call_outside_ctor!>s<!>uper().c
        <!sema_invalid_this_call_outside_ctor!>s<!>uper().c++
    }
    public static func ff() {
        <!sema_invalid_this_call_outside_ctor!>s<!>uper()
        <!sema_invalid_this_call_outside_ctor!>s<!>uper().c
        <!sema_invalid_this_call_outside_ctor!>s<!>uper().c++
    }
}
