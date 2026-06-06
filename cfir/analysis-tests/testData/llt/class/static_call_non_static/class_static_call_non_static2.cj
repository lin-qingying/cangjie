<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class Base {
    public var x: Int32 = 1
    public static var test: Int32 = 2
    public func f(): Int32 {
        return 1
    }
    public static func f1(): Int32 {
        return 1
    }
    public static func get(): Int32 {
        func a(x1!: Int32 = <!sema_static_function_cannot_access_non_static_member!>f<!>(), x2!: Int32 = <!sema_static_function_cannot_access_non_static_member!>x<!>): Int32 {
            <!sema_static_function_cannot_access_non_static_member!>f<!>()
            f1()
            test
            <!sema_static_function_cannot_access_non_static_member!>x<!>
            var lambda1 = {=> <!sema_static_lambdaExpr_cannot_access_non_static!>f<!>()}
            return <!sema_static_function_cannot_access_non_static_member!>f<!>()
        }
        var lambda = {=> <!sema_static_lambdaExpr_cannot_access_non_static!>f<!>()}
        return a()
    }
    public static var lam = {
        =>
        <!sema_static_lambdaExpr_cannot_access_non_static!>f<!>();
        0
    }
    public static var lam1 = {
        =>
        <!sema_static_lambdaExpr_cannot_access_non_static!>x<!>;
        0
    }
}
