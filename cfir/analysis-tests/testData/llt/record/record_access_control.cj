// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %noColor %f %error_count_limit_all 2>&1 | compare %f

struct A {
    public let a: Int64 = 1
    private let b: Int64 = 2
    public static let c: Int64 = 3
    private static let d: Int64 = 4
    public func m() {
        a // ok
        b // ok
        c // ok
        d // ok
        l() // ok
        o() // ok
    }
    private func l() {}
    public static func n() {
        <!sema_static_function_cannot_access_non_static_member!>a<!>
        <!sema_static_function_cannot_access_non_static_member!>b<!>
        c // ok
        d // ok
        <!sema_static_function_cannot_access_non_static_member!>l<!>()
        <!sema_static_function_cannot_access_non_static_member!>m<!>()
        o() // ok
    }
    private static func o() {}
    public operator func +(q: A) {
        a
        b
        c
        d
        l()
        m()
        n()
        o()
    }
    private operator func -(s: A) {}
}

main() {
    A().a // ok
    <!sema_illegal_access_non_static_member!>A<!>.a

    <!sema_invalid_access_control!>A<!>().b
    <!sema_illegal_access_non_static_member!>A<!>.b

    <!sema_object_cannot_access_static_member!>A<!>().c
    A.c // ok

    <!sema_object_cannot_access_static_member!>A<!>().d
    <!sema_invalid_access_control!>A<!>.d

    A().m()
    <!sema_illegal_access_non_static_member!>A<!>.m()

    A().<!sema_no_match_function_declaration_for_call!>l<!>()
    <!sema_illegal_access_non_static_member!>A<!>.l()

    <!sema_object_cannot_access_static_member!>A<!>().n()
    A.n() // ok

    <!sema_object_cannot_access_static_member!>A<!>().o()
    A.<!sema_no_match_function_declaration_for_call!>o<!>()

    <!sema_ref_not_be_type!>A<!> <!sema_invalid_binary_expr!>+<!> <!sema_ref_not_be_type!>A<!>
    A() + A()

    A() <!sema_invalid_binary_expr!>-<!> A()
    return 0
}
