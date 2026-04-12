// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

open class C0 {
    public func foo(): Int64 {
        return 0
    }
}

open class C1 <: C0 {}

class C2 <: C1 {
    public func foo2(): Int64 {
        var res: Int64 = super.foo()
        return res
    }
}

main(): Int64 {
    var obj = C2()
    var x = obj.foo2()
    if (x == 0) {
        return 0
    }
    return 1
}
