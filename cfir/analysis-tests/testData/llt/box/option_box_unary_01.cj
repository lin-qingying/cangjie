// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class A {
    operator func -() {
        1
    }
}

main() {
    var count = 0
    let a: ?Int64 = -(Some(A()) ?? A())
    if (let Some(1) <- a) {
        count++
    }
    let b: ?Int64 = -(Some(11) ?? 0)
    if (let Some(-11) <- b) {
        count++
    }
    if (count != 2) {
        return 1
    }
    return 0
}
