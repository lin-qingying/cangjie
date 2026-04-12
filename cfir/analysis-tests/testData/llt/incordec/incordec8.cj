// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

// 成员函数中b++
main(): Int64 {
    return run()
}

class COO {
    public var a: Int32 = 1
    public var b: Int32 = 0
    public func foo(x: Int32): Int32 {
        b = 1
        b++
        return a + b + x
    }
}

func run(): Int64 {
    var coo: COO = COO()
    var res1: Int32 = coo.foo(1)
    if (res1 == 4) {
        return 0
    }
    return 1
}
