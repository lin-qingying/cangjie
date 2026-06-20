// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public var a: Int32 = 1
    public static var b: Int32 = 2
    public static let lambda2 = {=> return <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>a<!> + b + <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>f<!>oo1()}
    public static let arr = {=> {=> return <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>f<!>oo1()}}

    public func foo1(): Int32 {
        return a + b + 2
    }

    public static func foo2(): Int32 {
        func foo3(): Int32 {
            return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!> + b
        }
        return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!> + b + <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>oo1()
    }
}
