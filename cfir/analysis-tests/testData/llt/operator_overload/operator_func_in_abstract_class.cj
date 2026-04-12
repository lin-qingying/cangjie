// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

abstract class A {
    public operator func [](x: Float32, value!: Int64): Unit
}

class B <: A {
    var a: Int64 = 0
    public operator func [](x: Float32, value!: Int64): Unit {
        a = value
    }
    operator func [](x: Float32): Int64 {
        a
    }
}

main(): Int64 {
    var b = B()
    b[1.0] = 2
    return b[2.0] - 2
}
