// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

abstract class C1 {
    public func foo(): Int64
}

open class C2 <: C1 {
    public func foo(): Int64 {
        return 1
    }
}

class C3 <: C2 {}

main(): Int64 {
    var obj = C3()
    if (obj.foo() == 1) {
        return 0
    }

    return 1
}
