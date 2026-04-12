// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f() {
    0
}

func f(x: Int64) {
    x
}

const func f(x: Bool) {
    x
}

main() {
    if (f(true)) {
        f()
    } else {
        f(1)
    }
}
