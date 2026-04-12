// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-7: %run %run_opt %output %run_args

// f(x) = 2 * x + 1
const func f(x: Int64) {
    const func g(x: Int64) {
        2 * x
    }
    let double = g(x)
    1 - (-double)
}

const x = f(3)

main() {
    x
}
