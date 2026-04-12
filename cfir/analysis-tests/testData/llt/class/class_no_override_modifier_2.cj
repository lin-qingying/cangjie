// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

interface I1 {
    static func foo(): Int64 {
        return 1
    }
}

class C1 <: I1 {
    public static func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    var a: Int64 = C1.foo()
    if (a == 2) {
        return 0
    }
    return 1
}
