// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I1 {
    func foo1(): Int32 {
        return 1
    }
}

interface I2<T> {
    func foo2(): Int32 {
        return 2
    }
}

interface I3 <: I1 & I2<Int64> {
    func foo1(): Int32 {
        return 3
    }
    func foo2(): Int32 {
        return 3
    }
}

main(): Int64 {
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
