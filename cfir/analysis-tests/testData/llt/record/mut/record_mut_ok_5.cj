// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface I1 {
    mut func foo1(): Unit
}

struct R1 <: I1 {
    public var i = 0
    public mut func foo1(): Unit {
        i = i + 1
    }
}

main() {
    var a = R1()
    var b: I1 = a
    b.foo1()
    if (a.i == 0) {
        return 0
    }
    return 1
}
