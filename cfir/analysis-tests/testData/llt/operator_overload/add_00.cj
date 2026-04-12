// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f

extend Int32 {
    func b() {
        1
    }
    operator func +(x: Unit) {
        2
    }
}

main() {
    let x = 1.b()
    let y = 1 + ()
    println(x)
    println(y)
}

/* SCAN
1
2
*/
