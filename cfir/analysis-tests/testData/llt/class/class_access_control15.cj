// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface Data2 {
    func foo(a: Int64): Int64 {
        return 2
    }
}

interface Data3 <: Data2 {}

open class Test1 <: Data2 {
    private func foo(): Int64 {
        return 3
    }
}

class Test <: Test1 & Data2 & Data3 {}

main(): Int64 {
    var a = Test().foo
    if (a(2) == 2) {
        return 0
    }
    return 1
}
