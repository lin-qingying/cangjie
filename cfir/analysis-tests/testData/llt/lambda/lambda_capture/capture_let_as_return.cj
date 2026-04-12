// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    func f() {
        let x = 1
        func g() {
            x
        }
        g
    }
    {
        x: Int64, y: Int64 =>
        let a = x + y
        {=> 2 * a}
    }
    return 0
}
