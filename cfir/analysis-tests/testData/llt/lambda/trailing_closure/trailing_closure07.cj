// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    func f() {
        func g(h: (Int64) -> Int64) { // label 1
            h(0)
        }
        g
    }
    func f(h: (Int64) -> Int64) { // label 2
        h(1)
    }
    var res = f() {t => t + 1} // match label 2
    return res - 2
}
