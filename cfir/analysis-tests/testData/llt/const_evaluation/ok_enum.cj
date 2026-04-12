// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum E {
    A | B(Int64)

    const func zero() {
        0
    }
}

const a = A
const b = B(0)

const func f(x: E) {
    match (x) {
        case A => x.zero()
        case B(t) => t
    }
}

const x = f(a) + f(b)

main() {
    x
}
