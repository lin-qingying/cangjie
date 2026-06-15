<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


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
        func a(x1!: Int32 = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>(), x2!: Int32 = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>x<!>): Int32 {
            <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>()
            f1()
            test
            <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>x<!>
            var lambda1 = {=> <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>f<!>()}
            return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>()
        }
        var lambda = {=> <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>f<!>()}
        return a()
    }
    public static var lam = {
        =>
        <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>f<!>();
        0
    }
    public static var lam1 = {
        =>
        <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>x<!>;
        0
    }
}
