// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum A {
    a | b(Int64)

    public operator func ()(x: Int64) {
        0
    }
}

main(): Int64 {
    var x = A.b(10) // choose enum constructor
    var y = A.a(1)
    var z = match (x) {
        case b(w) => w
        case a => 0
    }
    var a: Int64 = a(1)
    return z - 10 + a
}
