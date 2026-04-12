// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    func f(g: (Int64, Int64) -> Int64) {
        var a = 1
        var b = 2
        return g(a, b)
    }
    var t = f {x, y => x + 2 * y}
    return t - 5
}
