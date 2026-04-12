// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum E {
    A(Int64)
}

const (A(a), (b, (c, d))) = (A(1), (2, (3, 4)))

const func f() {
    a == 1 && b == 2 && c == 3 && d == 4
}

main() {
    if (f()) {
        0
    } else {
        1
    }
}
