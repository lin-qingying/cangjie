// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct A {
    public A(var a!: Int64 = 3, var b!: Int64 = a) {
        var d: Int64 = a
        this.b = d + 1
    }
}

struct B {
    public var a: Int64 = 2
    public B(a!: Int64 = 3, var b!: Int64 = a) {
        var d: Int64 = a
        this.b = d + 1
    }
}

struct C {
    public C(var a!: Int64 = 1, var b!: Int64 = a) {}
}

main(): Int64 {
    if (A().b == 4 && B().b == 4 && C(a: 2, b: 8).b == 8 && C().b == 1) {
        return 0
    }
    return 1
}
