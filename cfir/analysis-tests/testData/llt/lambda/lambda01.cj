<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public var a: Int32 = 1
    public static var b: Int32 = 2
    public static let lambda2 = {=> return <!sema_static_lambdaExpr_cannot_access_non_static!>a<!> + b + <!sema_static_lambdaExpr_cannot_access_non_static!>f<!>oo1()}
    public static let arr = {=> {=> return <!sema_static_lambdaExpr_cannot_access_non_static!>f<!>oo1()}}

    public func foo1(): Int32 {
        return a + b + 2
    }

    public static func foo2(): Int32 {
        func foo3(): Int32 {
            return <!sema_static_function_cannot_access_non_static_member!>a<!> + b
        }
        return <!sema_static_function_cannot_access_non_static_member!>a<!> + b + <!sema_static_function_cannot_access_non_static_member!>f<!>oo1()
    }
}
