// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args | compare %f

main() {
    let a = 0
    let v = foo(a)
    println(v)
}

func foo(a: Int64) {
    if (let 1 | 2 | 3 <- a) {
        a + 1
    } else {
        -1
    }
}

/* SCAN-IN
-1
*/
