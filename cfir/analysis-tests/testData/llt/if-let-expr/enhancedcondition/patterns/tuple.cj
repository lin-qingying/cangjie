// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args | compare %f

enum Color {
    R | G | B(Int64)
}

func foo(a: (?Int64, Color)) {
    if (let (Some(a), B(b)) <- a) {
        a + b
    } else {
        -1
    }
}

main() {
    println(foo((Some(3), B(15))))
    println(foo((Some(3), R)))
    println(foo((None, B(15))))
    println(foo((None, R)))
}

/* SCAN-IN
18
-1
-1
-1
*/
