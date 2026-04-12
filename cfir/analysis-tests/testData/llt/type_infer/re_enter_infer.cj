// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func g(c: (Int32) -> Int32) {
    func h(a: Int32) {
        func f(b: Int32) {
            return a
        }
        var v: Int32 = f(1)
        return a
    }
    var x = c(1)
    return 1
}

func x(): Int32 {
    1
}

func x(a!: Int32 = 0): Int32 {
    2
}

func f(): Int32 {
    var bb: Int64 = g(x)
    return 1
}

main(): Int64 {
    var a = f()
    if (a != 1) {
        return 1
    }
    return 0
}
