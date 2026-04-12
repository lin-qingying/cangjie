// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface A<T> {
    operator func [](b: (A<T>, A<T>)): Bool {
        true
    }
    operator func +(b: (A<T>, A<T>)): Bool {
        true
    }
}

class A_B<T> <: A<T> {}

main(): Int64 {
    var a: (A<Int32>, A<Int32>) = (A_B<Int32>(), A_B<Int32>())
    var b = A_B<Int32>()
    if (!(b[a])) {
        return 1
    }
    if (!(b + a)) {
        return 13
    }
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
