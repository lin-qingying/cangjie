// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class B {
    public var i: Int64 = 2
    public func foo(): Int64 {
        this.i++
        return this.i - 3
    }
}

main(): Int64 {
    var b = B()
    return b.foo()
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
