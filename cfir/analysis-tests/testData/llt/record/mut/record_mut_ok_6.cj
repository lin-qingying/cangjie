// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface I1<T> {
    mut func foo1(x: T): Unit
}

struct R1<T> <: I1<T> {
    public var i: T

    public init(x: T) {
        i = x
    }

    public mut func foo1(x: T): Unit {
        i = x
    }
}

main() {
    var a = R1<Int64>(0)
    var b: I1<Int64> = a
    b.foo1(1)
    if (a.i == 0) {
        return 0
    }
    return 1
}
