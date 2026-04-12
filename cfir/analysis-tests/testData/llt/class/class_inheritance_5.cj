// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface Data {
    static func foo(a: Int64): Int64

    func fooo(a!: Int64): Int64
}

interface Data2 {
    static func foo(a: Int64): Int64 {
        return 2
    }
}

interface Data3 {
    func fooo(a!: Int64): Int64 {
        return 2
    }
}

class Test <: Data & Data2 & Data3 {}

main(): Int64 {
    var res1: Int64 = Test().fooo(a: 2)
    var res2: Int64 = Test.foo(2)
    if (res1 != 2 || res2 != 2) {
        return 1
    }
    return 0
}
