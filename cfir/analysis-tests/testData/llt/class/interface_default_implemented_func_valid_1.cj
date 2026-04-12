// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface A {
    mut func c(b: Int64): Int64

    mut func cc(b: Int64): Int64 {
        c(b)
    }
}

struct R <: A {
    public var a = 3
    public mut func c(b: Int64): Int64 {
        this.a = b
        return this.a
    }
}

main() {
    var a = R()
    a.cc(9)
    return (a.a - 9)
}
