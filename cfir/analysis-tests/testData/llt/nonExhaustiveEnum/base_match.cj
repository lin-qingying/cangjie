// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC-PIPE: %run %run_opt %output %run_args | compare %f

enum A {
    | B | C(Int64) | ...

    func foo() {
        1
    }
}

var a = B

main() {
    println(match (a) {
        case B => 0
        case C(_) => 1
        case _ => 2
    })
    println(match (C(4)) {
        case B => 0
        case C(a) where a > 3 => 1
        case _ => 2
    })
    println(match (C(2)) {
        case B => 0
        case C(a) where a > 3 => 1
        case _ => 2
    })
}
/* SCAN
0
1
2
*/
