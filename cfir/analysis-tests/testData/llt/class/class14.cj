// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

open class A {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}

class B <: A {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}

abstract class A1 {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}

class B1 <: A1 {
    public static func foo1(a: Int32, b!: Int64 = 0): Int32 {
        return a
    }
}
