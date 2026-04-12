// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface Base3 {
    func foo(): Int64
}

interface Data <: Base3 {
    func foo(): Int64 {
        return 1
    }
    func foo(a: Int64): Int64 {
        return a
    }
}

class Test <: Data {}

main(): Int64 {
    return 0
}
