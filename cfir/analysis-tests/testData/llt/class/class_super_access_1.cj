// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

open class C1 {
    public func foo(): Int64 {
        return 0
    }
}

interface I1 {
    func foo(): Int64 {
        return 0
    }
}

class C2 <: C1 & I1 {
    public func foo1(): Int64 {
        var f = super.foo
        f()
        return 0
    }
}

main(): Int64 {
    var obj = C2()
    if (obj.foo1() == 0) {
        return 0
    }
    return 1
}
