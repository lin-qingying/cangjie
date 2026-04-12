// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-auto  invalid use of non-static member 'a' in static lambda expression
// ASSERT: regex-auto  invalid use of non-static member 'a' in static lambda expression
// ASSERT: regex-auto  'a' is non-static member, cannot be accessed by static function 'foo3'
// ASSERT: regex-auto  'a' is non-static member, cannot be accessed by static function 'foo2'

class A {
    public var a: Int32 = 1
    public static var b: Int32 = 2
    public static let lambda2 = {=> return a + b + foo1()}
    public static let arr = {=> {=> return foo1()}}

    public func foo1(): Int32 {
        return a + b + 2
    }

    public static func foo2(): Int32 {
        func foo3(): Int32 {
            return a + b
        }
        return a + b + foo1()
    }
}
