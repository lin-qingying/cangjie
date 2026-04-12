// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    let a = 1
    func f(x: Int64, f: (Int64) -> Int64) {
        return f(x)
    }
    func g(x: Int64) {
        return x ^ 2
    }
    var lambda = {b: Int64, g: (Int64) -> Int64 => g(b)}
    f(1, {t => t * a})
    lambda(2, {t => t * a})
    lambda(3, g)
    return 0
}
