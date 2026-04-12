// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

interface I0 {
    func foo(): Int64 {
        return 1
    }

    func foo1(): Int64 {
        var res = this.foo()
        return res
    }
}

class C0 <: I0 {}

main(): Int64 {
    var obj = C0()
    var x = obj.foo1()
    if (x == 1) {
        return 0
    }
    return 1
}
