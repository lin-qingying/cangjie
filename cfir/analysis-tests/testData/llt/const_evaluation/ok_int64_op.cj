// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

const func test() {
    let x = 2
    // Arithmetic
    -x == -2 && x + 1 == 3 && x - 1 == 1 && x * 3 == 6 && x / 2 == 1 && x % 3 == 2 && x ** 3 == 8 &&
        // Relation
        x == 2 && x != -2 &&
        x < 3 && x <= 3 && x > 0 && x >= 0 &&
            // Bitwise
            x << 1 == 4 && x >> 1 == 1 && (x & 3) == 2 && (x ^ 1) == 3 && (x | 1) == 3
}

main() {
    if (test()) {
        0
    } else {
        1
    }
}
