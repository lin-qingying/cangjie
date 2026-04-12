// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f(c: Bool) {
    let a: Int64
    if (c) {
        a = Some(1) ?? throw Exception()
    } else {
        a = 2
    }
    return a
}

main() {
    f(true) - 1
}
