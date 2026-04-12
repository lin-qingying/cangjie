// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct D {
    public D(var D!: Int64 = 3) {}
}

struct E {
    public E(var E: Int64) {}
}

struct F {
    public F(let E: Int64) {}
    public func F(a: Int64) {
        E
    }
}

main(): Int64 {
    if (D().D == 3 && E(2).E == 2 && F(4).F(2) == 4) {
        return 0
    }
    return 1
}
